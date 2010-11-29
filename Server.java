
import java.io.*;
import java.net.*;


public class Server extends Thread {


    //  implement this and pass as arg to Server constructor
    public interface ClientHandler {
        public void newClient( Client c );
        public void newInputFromClient( Client c );
        public void lostClient( Client c );
    }


    private static final int DEFAULT_SERVER_READ_TIMEOUT = 1000;
    private static final int DEFAULT_CLIENT_READ_TIMEOUT = 1000;
    private static final int DEFAULT_NUM_RETRIES = 5;


    private final int port;
    private final ClientHandler handler;
    private final int serverTimeout;
    private final int clientTimeout;
    private final int numRetries;


    private ServerSocket ss;
    private boolean running;
    private int idxNextClient = 0;


    public Server( int port, ClientHandler handler ) {
        this( port, handler,
              DEFAULT_SERVER_READ_TIMEOUT, DEFAULT_CLIENT_READ_TIMEOUT,
              DEFAULT_NUM_RETRIES );
    }
    public Server( int port, ClientHandler handler,
                   int serverTimeout, int clientTimeout,
                   int numRetries ) {
        this.port = port;
        this.handler = handler;
        this.serverTimeout = serverTimeout;
        this.clientTimeout = clientTimeout;
        this.numRetries = numRetries;
        setName( "server" );
    }


    public synchronized boolean isRunning() { return running; }
    public synchronized void setRunning( boolean b ) { running = b; }


    public void run() {
        out( "Server.run(): starting" );
        setRunning( true );
        int numRetries = this.numRetries;
        while ( isRunning() ) {
            out( "Server.run(): opening server socket" );
            try {
                ss = new ServerSocket( port );
                ss.setSoTimeout( serverTimeout );
                numRetries = this.numRetries;
                while ( isRunning() ) {
                    try {
                        Socket s = ss.accept();
                        try {
                            Client c = new Client(s);
                            c.start();
                            handler.newClient(c);
                        } catch ( Exception e ) {
                            out( "Server.run(): exception: " + e );
                        }
                    } catch ( SocketTimeoutException e ) {}
                }
            } catch ( Exception e ) {
                out( "Server.run(): exception: " + e );
                if ( numRetries-- > 0 ) {
                    out( "Server.run(): will retry" );
                } else {
                    out( "Server.run(): giving up" );
                    setRunning( false );
                }
            } finally {
                try {
                    if ( ss != null ) {
                        ss.close();
                        ss = null;
                    }
                } catch ( IOException ee ) {}
            }
        }
        out( "Server.run(): stopping" );
    }


    public class Client extends Thread {


        private final Socket s;
        private final BufferedReader br;
        private final BufferedWriter bw;
        private final StringBuffer input;

        private boolean running;


        private Client( Socket s ) throws IOException {
            this.s = s;
            s.setSoTimeout( clientTimeout );
            br = new BufferedReader( new InputStreamReader(
                                         s.getInputStream() ) );
            bw = new BufferedWriter( new OutputStreamWriter(
                                         s.getOutputStream() ) );
            input = new StringBuffer();
            setName( "client" + idxNextClient++ );
        }


        public synchronized boolean isRunning() { return running; }
        public synchronized void setRunning( boolean b ) { running = b; }


        //  non-blocking read
        public String getInput() {
            synchronized ( input ) {
                if ( input.length() == 0 ) {
                    return null;
                } else {
                    String s = input.toString();
                    input.setLength(0);
                    return s;
                }
            }
        }

        private void addInput( String s ) {
            synchronized ( input ) {
                input.append(s);
                input.append("\n");
                handler.newInputFromClient( this );
            }
        }

        //  blocking write
        public void write( String s ) {
            try {
                bw.write(s);
                bw.flush();
            } catch ( IOException e ) {
                out( "Client.write(): exception: " + e );
                handler.lostClient( this );
                out( "Client.write(): forcing stop" );
                setRunning( false );
            }
        }

        public void run() {
            out( "Client.run(): starting" );
            setRunning( true );
            try {
                while ( isRunning() ) {
                    try {
                        String line = br.readLine();
                        addInput( line );
                    } catch ( SocketTimeoutException e ) {}
                }
            } catch ( IOException e ) {
                out( "Client.run(): exception: " + e );
                handler.lostClient( this );
                out( "Client.run(): forcing stop" );
                setRunning( false );                
            } finally {
                try {
                    if ( br != null ) br.close();
                    if ( bw != null ) bw.close();
                    if ( s  != null )  s.close();
                } catch ( IOException ee ) {}
            }
            out( "Client.run(): stopping" );
        }
    }


    private static void out( String s ) {
        System.out.println(s);
    }
}



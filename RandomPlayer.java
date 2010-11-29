
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.*;


public class RandomPlayer {



    RandomPlayer( String host, int port, String name,
                  int nGambles, int nRuns )
            throws Exception {
        //  connect & send name
        System.out.println( "connecting" );
        Socket s = new Socket( host, port );
        BufferedWriter bw = new BufferedWriter(
                            new OutputStreamWriter(
                            s.getOutputStream() ) );
        BufferedReader br = new BufferedReader(
                            new InputStreamReader(
                            s.getInputStream() ) );
        bw.write( name + "\n" );
        bw.flush();
        //  wait for OK
        String in = br.readLine();
        if ( !in.equals( "OK" ) ) {
            System.out.println( "got back: " + in );
            throw new RuntimeException();
        }
        //  play
        double[] allocs = new double[nGambles];
        Random rnd = new Random();
        DecimalFormat df = new DecimalFormat( "0.00000" );
        for ( int i = 0; i < nRuns; i++ ) {
            System.out.println( "playing round " + i );
            //  generate random allocs that sum to 1
            for ( int j = 0; j < nGambles; j++ )
                allocs[j] = rnd.nextDouble();
            normalize( allocs );
            //  send allocs
            StringBuffer sb = new StringBuffer( nGambles*8 );
            for ( int j = 0; j < nGambles; j++ )
                sb.append( df.format( allocs[j] ) ).append( " " );
            String out = sb.toString();
            System.out.println( "sending: " + out );
            bw.write( out + "\n" );
            bw.flush();
            //  make sure we get back OK
            in = br.readLine();
            if ( !in.equals( "OK" ) ) {
                System.out.println( "got back: " + in );
                throw new RuntimeException();
            }
            //  block till receive outcomes
            in = br.readLine();
            System.out.println( "got back: " + in );
        }
        System.out.println( "done" );
    }


    static void normalize( double[] d ) {
        double total = 0;
        for ( int i = 0; i < d.length; i++ )
            total += d[i];
        for ( int i = 0; i < d.length; i++ )
            d[i] /= total;
    }


    public static void main( String[] args ) throws Exception {
        if ( args.length != 5 ) {
            System.out.println(
                "usage:  java RandomPlayer" +
                " <host> <port> <name> <nGambles> <nRuns>" );
            System.exit(1);
        }
        new RandomPlayer( args[0],
                          Integer.parseInt( args[1] ),
                          args[2],
                          Integer.parseInt( args[3] ),
                          Integer.parseInt( args[4] ) );
    }

}

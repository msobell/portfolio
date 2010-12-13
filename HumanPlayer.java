import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.DecimalFormat;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Spawned from the Simulator after "Play Human" option is clicked Gets host,
 * port, nGambes (and the datafile name) from the Simulator at construction
 * Creates GUI for a user to type in name and start, when game starts and it's
 * user's turn, they can select parameters to send in their bets for each gamble
 * game continues until the user quits.
 * 
 * @author ajk377
 * 
 */
public class HumanPlayer {
  UserWindow userWindow;
  String name;
  private final int nGambles, port;
  private final String fName, host;
  private BufferedReader br;
  private BufferedWriter bw;

  public HumanPlayer(String host, int port, int nGambles, String fName)
      throws Exception {
    this.fName = fName;
    this.nGambles = nGambles;
    this.host = host;
    this.port = port;
    // set up GUI
    // make GUI to get user input
    userWindow = new UserWindow();
  }

  /**
   * Called when userWindow's start button gets pushed (after user registers his
   * name)
   * 
   * @param host
   * @param port
   */
  private void play(Double[] allocs) {
    if (connectionEstablished) {
      String in;
      // play=send the results
      String out = convertToString(nGambles, allocs);
      try {
        bw.write(out + "\n");
        bw.flush();
        // make sure we get back OK
        in = br.readLine();
        if (!in.equals("OK")) {
          System.out.println("got back: " + in);
          throw new RuntimeException(in);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void connect(String name) {
    // connect & send name
    System.out.println("connecting");
    try {
      Socket s = new Socket(host, port);
      bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
      br = new BufferedReader(new InputStreamReader(s.getInputStream()));
      bw.write(name + "\n");
      bw.flush();
      String in = br.readLine();
      if (!in.equals("OK")) {
        System.out.println("got back: " + in);
        throw new RuntimeException(in);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    // wait for OK

    connectionEstablished = true;
  }

  boolean connectionEstablished = false;

  public String convertToString(int nGambles, Double[] allocs) {
    DecimalFormat df = new DecimalFormat("0.00000");
    StringBuffer sb = new StringBuffer(nGambles * 8);
    for (int j = 0; j < nGambles; j++)
      sb.append(df.format(allocs[j])).append(" ");
    String out = sb.toString();
    System.out.println("sending: " + out);
    return out;
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 4) {
      System.out.println("usage:  Java Player"
          + " <Host> <Port> <Ngambles> <Datafilename>");
      System.exit(1);
    }
    new HumanPlayer(args[0], Integer.parseInt(args[1]),
        Integer.parseInt(args[3]), args[5]);
  }

  class UserWindow extends JFrame {

    Font f1 = new Font("Dialog", Font.BOLD, 12);
    Font f2 = new Font("Dialog", Font.PLAIN, 12);

    FontMetrics fm = getFontMetrics(f2);

    JButton bPlay, bName, bStart;
    ResultsPanel pInputs;

    int idxOrder = -1;
    double[] rets = new double[nGambles];
    double[] drs = new double[nGambles];
    Color[] colors = new Color[nGambles];

    AffineTransform atVert = AffineTransform.getRotateInstance(-Math.PI / 2);
    String name;

    public UserWindow() {
      super("User Inputs");
      this.name = "Human Player";
      buildGUI();
      addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          System.exit(0);
        }
      });
    }

    public Double[] getBets(int i) {
      // TODO Auto-generated method stub
      return null;
    }

    void buildGUI() {
      JLabel lAttrs = new JLabel("Name: " + name + " with " + nGambles
          + " gambles");
      lAttrs.setFont(f1);
      bStart = new JButton("Start");
      bPlay = new JButton("Send");
      bName = new JButton("Set Name");
      bPlay.setFont(f1);
      bName.setFont(f1);
      bStart.setFont(f1);
      bPlay.setEnabled(false);
      bStart.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // get name from the text area or something, set it to
          // makes panel like selectable(ungray it)
          bPlay.setEnabled(true);
          connect(name);
        }

      });
      bPlay.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // get contents from the panel
          // if turn..?
          Double[] bets = pInputs.getBets();
          play(bets);
        }
      });
      JPanel pTop = new JPanel();
      pTop.add(lAttrs);
      pTop.add(bPlay);
      pInputs = new ResultsPanel( nGambles );
      Box boxNorth = Box.createVerticalBox();
      boxNorth.add(pTop);
      boxNorth.add(pInputs);
      boxNorth.add(Box.createVerticalStrut(5));
      JPanel pane = (JPanel) getContentPane();
      pane.setLayout(new BorderLayout());
      pane.setBorder(new EmptyBorder(5, 5, 5, 5));
      pane.add(boxNorth, BorderLayout.NORTH);
    }

  }

    static class MySlider extends JSlider 
	implements ChangeListener {
	// num is an index into the bets/slids array
	int num;
	public void setNum( int num ) {
	    this.num = num;
	}

	public int getNum() {
	    return this.num;
	}
    }

    static class ResultsPanel extends JPanel {
	Double[] bets;
	JSlider[] slids;
	int nGambles;
	public ResultsPanel( int nGambles ) {
	    this.nGambles = nGambles;
	    this.bets = new Double[nGambles];
	    this.slids = new JSlider[nGambles];
	}

	public void stateChanged( ChangeEvent e ) {
	    MySlider source = (MySlider)e.getSource();
	    if (!source.getValueIsAdjusting()) {
		this.bets[(int)source.getNum()] = (double)source.getValue();
	    }
	}

	public void setBets() {
	    for( int i = 0; i < this.nGambles; i++ ) {
		// make a slider for each gamble
		slids[i] = new MySlider(); // horiz with 1-100 def 50
		slids[i].setNum( i );
		slids[i].addChangeListener( this );
		slids[i].setMajorTickSpacing( 10 );
		slids[i].setMinorTickSpacing( 1 );
		slids[i].setPaintTicks( true );
		slids[i].setPaintLabels( false );
		add(slids[i]);
	    }
	}

	public Double[] getBets() {
	    return this.bets;
	}
    }
}

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.Random;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
/**
 * Spawned from the Simulator after "Play Human" option is clicked
 * Gets host, port, nGambes (and the datafile name) from the Simulator at 
 * construction
 * Creates GUI for a user to type in name and start, when game starts and it's 
 * user's turn, they can select parameters to send in their bets for each gamble
 * game continues until the user quits.
 * @author ajk377
 *
 */
public class HumanPlayer {
  UserWindow userWindow;
  String name;
  private final int nGambles, port;
  private final String fName, host;
  
  public HumanPlayer(String host, int port, int nGambles, String fName)
  throws Exception {
    this.fName = fName;
    this.nGambles = nGambles;
    this.host = host;
    this.port = port;
    //set up GUI
    //make GUI to get user input
    userWindow= new UserWindow();
  }
  /**
   * Called when userWindow's start button gets pushed (after user registers
   * his name)
   * @param host
   * @param port
   * @param nGambles
   * @throws UnknownHostException
   * @throws IOException
   */
  private void connectAndPlay(String name) throws UnknownHostException, IOException {
    // connect & send name
    System.out.println("connecting");
    Socket s = new Socket(host, port);
    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
        s.getOutputStream()));
    BufferedReader br = new BufferedReader(new InputStreamReader(
        s.getInputStream()));
    bw.write(name + "\n");
    bw.flush();
    // wait for OK
    String in = br.readLine();
    if (!in.equals("OK")) {
      System.out.println("got back: " + in);
      throw new RuntimeException(in);
    }
    
    // play
    Double[] allocs = new Double[nGambles];
    Random rnd = new Random();
    DecimalFormat df = new DecimalFormat("0.00000");
    //TODO: this should be a while something or whatever
    for (int i = 0; i < 20; i++) {
      System.out.println("playing round " + i);
      allocs = userWindow.getBets(i);
      String out = convertToString(nGambles, allocs, df);
      bw.write(out + "\n");
      bw.flush();
      // make sure we get back OK
      in = br.readLine();      
      if (!in.equals("OK")) {
        System.out.println("got back: " + in);  
        throw new RuntimeException(in);
      }
      // block till receive outcomes
      in = br.readLine();
      System.out.println("got back: " + in);
    }
    System.out.println("done");
  }

  public String convertToString(int nGambles, Double[] allocs, DecimalFormat df) {    
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
        Integer.parseInt(args[3]),args[5]);
  }
class UserWindow extends JFrame {

    Font f1 = new Font("Dialog", Font.BOLD, 12);
    Font f2 = new Font("Dialog", Font.PLAIN, 12);

    FontMetrics fm = getFontMetrics(f2);

    JButton bPlay;
    JPanel pOutcomes, pPlayers;

    int idxOrder = -1;
    double[] rets = new double[nGambles];
    double[] drs = new double[nGambles];
    Color[] colors = new Color[nGambles];

    AffineTransform atVert = AffineTransform.getRotateInstance(-Math.PI / 2);

    public UserWindow() {
      super("User Inputs");
      buildGUI();
      addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          Simulator.this.close();
          System.exit(0);
        }
      });
    }

    public Double[] getBets(int i) {
      // TODO Auto-generated method stub
      return null;
    }

    void addPlayer(Player p) {
      pPlayers.add(new PlayerPanel(p));
      pPlayers.validate();
      // pPlayers.repaint();
    }

    void animateGame(Game g) {
      // out( "animateGame( " + g + " )\n" );
      idxOrder = -1;
      for (int i = 0; i < nGambles; i++) {
        rets[i] = 0;
        drs[i] = 0;
      }
      for (int i = 0; i < nGambles; i++) {
        int idx = g.gambleOrder[i];
        trace("showing outcome " + g.outcomes[idx] + "\n");
        rets[idx] = g.outcomes[idx].pyf;
        drs[idx] = Math.min(rets[idx] - 2, 4);
        if (drs[idx] < 0)
          colors[idx] = new Color((float) (-drs[idx] / 2), 0, 0);
        else
          colors[idx] = new Color(0, (float) (drs[idx] / 4), 0);
        idxOrder = i;
        repaint();
        try {
          Thread.sleep(ANIMATE_SLEEP);
        } catch (InterruptedException e) {
        }
      }
    }

    void buildGUI() {
      JLabel lAttrs = new JLabel("Favorable Attrs: " + attrFav1 + ","
          + attrFav2 + "  " + "Unfavorable Attrs: " + attrUnfav1 + ","
          + attrUnfav2);
      lAttrs.setFont(f1);
      bPlay = new JButton("Play");
      bPlay.setFont(f1);
      bPlay.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          bPlay.setEnabled(false);
          (new Thread() {
            public void run() {
              synchronized (players) {
                // out( "gui triggered round\n" );
                Game g = play();
                animateGame(g);
                sendFeedback(g);
                bPlay.setEnabled(true);
                repaint();
              }
            }
          }).start();
        }
      });
      JPanel pTop = new JPanel();
      pTop.add(lAttrs);
      pTop.add(bPlay);
      pOutcomes = new OutcomesPanel();
      Box boxNorth = Box.createVerticalBox();
      boxNorth.add(pTop);
      boxNorth.add(pOutcomes);
      boxNorth.add(Box.createVerticalStrut(5));
      pPlayers = new JPanel();
      pPlayers.setLayout(new GridLayout(1, 0, 2, 2));
      JPanel pane = (JPanel) getContentPane();
      pane.setLayout(new BorderLayout());
      pane.setBorder(new EmptyBorder(5, 5, 5, 5));
      pane.add(boxNorth, BorderLayout.NORTH);
      pane.add(pPlayers, BorderLayout.CENTER);
    }
}


import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

public class SimulatorWithoutSockets{

  static final boolean TRACE = false;
  static final boolean DEBUG = false;

  static final int HI = 0;
  static final int MD = 1;
  static final int LO = 2;

  static final String[] LVLS = new String[] { "HI", "MD", "LO" };

  static final Random RND = new Random();

  static final DecimalFormat DF_INT = new DecimalFormat("000");
  static final DecimalFormat DF_RET = new DecimalFormat("00.000");
  static final DecimalFormat DF_PROB = new DecimalFormat("0.000");
  static final DecimalFormat DF_DLRS = new DecimalFormat("0.000");

  static final String OK = "OK";
  static final String ERR = "ERR";

  static final long CHECK_PLAY_SLEEP = 1000;
  static final long ANIMATE_SLEEP = 100;

  static final int MAX_CHANGE = 10;

  static int nGambles;
  int roundCount = 0;
  Gamble[] gambles;
  int attrFav1, attrFav2, attrUnfav1, attrUnfav2;
  ArrayList<HumanPlayerWithoutSockets>clients = new ArrayList<HumanPlayerWithoutSockets>();
  ArrayList<Player> players = new ArrayList<Player>();
  HashMap<Player, HumanPlayerWithoutSockets> players2clients = new HashMap<Player, HumanPlayerWithoutSockets>();
  HashMap<HumanPlayerWithoutSockets, Player> clients2players = 
    new HashMap<HumanPlayerWithoutSockets, Player>();
  ArrayList<Game> games = new ArrayList<Game>();
  int idxNextGame = 0;
  Viz viz;
  double maxTotalIncome = 0;
  double maxCumWealth = 1;
  int changeCount = 0;
  // AJ added 12/12
  //removed cause web can't read file 
  //private final String dataFile;
  private ArrayList<HumanPlayerWithoutSockets> humanPlayers = 
    new ArrayList<HumanPlayerWithoutSockets>();
  private JApplet applet;
  SimulatorWithoutSockets(JApplet applet) {
    this.applet = applet;
    
    readData();
    assignHiddenAttrs();
      out("viz mode\n");
      this.viz = new Viz();
      this.viz.setSize(800, 500);
      this.viz.setVisible(true);
  }
  public Component getGUI(){
    return viz;
  }
  public Component getHumanGUI(){
    return viz.h.getGUI();
  }

  public void newClient(HumanPlayerWithoutSockets c) {
    synchronized (players) {
      // out( "newClient\n" );
      Player p = new Player();
      clients.add(c);
      players.add(p);
      players2clients.put(p, c);
      clients2players.put(c, p);
      if (viz != null) {
        viz.addPlayer(p);
        viz.repaint();
        applet.repaint();
      }
    }
  }

  public void newInputFromClient(HumanPlayerWithoutSockets c, String input) {
    synchronized (players) {
      Player p = (Player) clients2players.get(c);
      // out( "new input from player " + p.name + ": " + input + "\n" );
      if (p.name == null) {
        trace("expecting name\n");
        if (input.length() == 0) {
          String err = "empty name";
          out(err + "\n");
          //c.write(ERR + ": " + err + "\n");
          return;
        }
        p.name = input;
        out("registered player " + p.name + "\n");
        //c.write(OK + "\n");
      } else {
        trace("expecting allocations\n");
        String[] ss = input.split("[ |,]+");
        if (ss.length != nGambles) {
          String err = "expected " + nGambles + " tokens;" + " got "
              + ss.length;
          out(err + "\n");
//          c.write(ERR + ": " + err + "\n");
          return;
        }
        double[] allocs = new double[nGambles];
        for (int i = 0; i < nGambles; i++) {
          try {
            allocs[i] = Double.parseDouble(ss[i]);
          } catch (NumberFormatException e) {
            String err = "can't parse token: " + ss[i];
            out(err + "\n");
  //          c.write(ERR + ": " + err + "\n");
            return;
          }
        }
        // out( "allocations read ok\n" );
    //    c.write(OK + "\n");
        normalize(allocs);
        p.newAllocs = allocs;
        p.haveNewAlloc = true;
      }
      if (viz != null)
        viz.repaint();
      applet.repaint();
    }
  }

  public void lostClient(HumanPlayerWithoutSockets c) {
    synchronized (players) {
      out("lost client\n");
      Player p = (Player) clients2players.get(c);
      p.alive = false;
      p.lastTurn = null;
      if (viz != null)
        viz.repaint();
      applet.repaint();
    }
  }

  boolean tryPlay() {
    synchronized (players) {
      if (viz == null) {
        // have new allocs for any live players?
        int nAlive = 0;
        int nHaveNewAllocs = 0;
        for (int i = 0; i < players.size(); i++) {
          Player p = (Player) players.get(i);
          if (!p.alive)
            continue;
          nAlive++;
          if (p.haveNewAlloc)
            nHaveNewAllocs++;
        }
        if (nAlive == 0 || nHaveNewAllocs < nAlive)
          return false;
      }
      Game g = play();
      sendFeedback(g);
      return true;
    }
  }

  Game play() {
    synchronized (players) {
      // out( "play()\n" );
      Game g = newGame(idxNextGame++);
      games.add(g);
      runAllocs(g);
      roundCount++;
      return g;
    }
  }

  void sendFeedback(Game g) {
    // out( "sendFeedback( " + g + " )\n" );
    StringBuffer sb = new StringBuffer(nGambles * 3);
    for (int i = 0; i < nGambles; i++)
      sb.append(LVLS[g.outcomes[i].lvl]).append(' ');
    sb.setCharAt(sb.length() - 1, '\n');
    String feedback = sb.toString();
    for (int i = 0; i < players.size(); i++) {
      Player p = (Player) players.get(i);
      if (!p.alive) {
        out("player " + p.name + " not alive\n");
        continue;
      }
      if (!p.haveNewAlloc) {
        out("player " + p.name + " no new alloc\n");
        continue;
      }
      // out( "sending feedback to " + p.name + "\n" );
      Turn t = p.lastTurn;
      // this is a ghetto way to do this -- 12/6/10 Max
      // System.out.println("Max = " + Double.MAX_VALUE);
      p.cumWealth *= t.totalIncome;
      if (t.totalIncome >= 2)
        p.nWins++;
      String totalIncome = DF_RET.format(t.totalIncome);
      HumanPlayerWithoutSockets c = (HumanPlayerWithoutSockets) players2clients.get(p);
//      c.write(totalIncome + " " + feedback);
      System.out.println("cumWealth for " + p.name + ": " + p.cumWealth);
      p.haveNewAlloc = false;
    }
  }

  void runAllocs(Game g) {
    // out( "runAllocs( " + g + " )\n" );
    maxTotalIncome = 0;
    for (int i = 0; i < players.size(); i++) {
      Player p = (Player) players.get(i);
      if (!p.alive) {
        out("player " + i + " not alive\n");
        continue;
      }
      if (!p.haveNewAlloc) {
        out("player " + i + " no new alloc\n");
        p.lastTurn = null;
        continue;
      }
      // out( "running player " + p.name + "'s allocs\n" );
      Turn t = new Turn(p.newAllocs);
      p.newAllocs = null;
      p.turns.add(t);
      p.lastTurn = t;
      p.nGames++;
      for (int j = 0; j < nGambles; j++)
        t.incomes[j] = t.allocs[j] * g.outcomes[j].pyf;
      t.totalIncome = 0;
      for (int j = 0; j < nGambles; j++) {
        t.totalIncome += t.incomes[g.gambleOrder[j]];
        t.cumIncomeInGambleOrder[j] = t.totalIncome;
      }
      maxTotalIncome = Math.max(maxTotalIncome, t.totalIncome);
      maxCumWealth = Math.max(maxCumWealth, p.cumWealth * t.totalIncome);
      out("player " + p.name + ": totalIncome = " + t.totalIncome + "\n");
    }
  }

  Game newGame(int turn) {
    out("newGame(" + turn + ")\n");
    // random gamble playing order
    ArrayList al = new ArrayList(nGambles);
    for (int i = 0; i < nGambles; i++)
      al.add(new Integer(i));
    Collections.shuffle(al);
    int[] gambleOrder = new int[nGambles];
    for (int i = 0; i < nGambles; i++)
      gambleOrder[i] = ((Integer) al.get(i)).intValue();
    // play gambles and figure outcomes
    Outcome[] outcomes = new Outcome[nGambles];
    for (int i = 0; i < nGambles; i++) {
      Gamble g = gambles[gambleOrder[i]];
      trace(DF_INT.format(i) + ": playing gamble " + g + "\n");
      double[] probs = new double[3];
      probs[HI] = g.probs[HI];
      probs[MD] = g.probs[MD];
      probs[LO] = g.probs[LO];
      trace("probs: " + arrayStr(probs, DF_PROB) + "\n");
      // modify probs by hidden attrs
      int attrSway = MD;
      if (g.attr == attrFav1 || g.attr == attrFav2) {
        // System.out.println("Modifying " + g.toString() );
        trace("attr favorable\n");
        attrSway = HI;
        probs[HI] += (probs[LO] /= 2);
        normalize(probs);
      } else if (g.attr == attrUnfav1 || g.attr == attrUnfav2) {
        // System.out.println("Modifying " + g.toString() );
        // System.out.println("Old LO = " + probs[LO]);
        trace("attr unfavorable\n");
        attrSway = LO;
        probs[LO] += (probs[HI] /= 2);
        // System.out.println("New LO = " + probs[LO]);
        normalize(probs);
      } else {
        trace("attr neutral\n");
      }
      trace("probs: " + arrayStr(probs, DF_PROB) + "\n");
      // modify probs by links
      int[] nLinks = new int[3];
      if (g.links.length == 1) {
        trace("no links\n");
      } else {
        trace((g.links.length - 1) + " links\n");
        for (int j = 0; j < g.links.length; j++) {
          Gamble gg = g.links[j];
          if (gg.id == g.id)

            continue;
          int lvl = (gg.outcomes.length == turn) ? -1 : gg.outcomes[turn].lvl;
          trace("gamble " + gg.id + " " + lvl + "\n");
          switch (lvl) {
          case HI:
            nLinks[HI]++;
            break;
          case MD:
            nLinks[MD]++;
            break;
          case LO:
            nLinks[LO]++;
            break;
          }
        }
        trace("nLinks: " + nLinks[HI] + nLinks[MD] + nLinks[LO] + "\n");
        if (nLinks[HI] > nLinks[MD] + nLinks[LO]) {
          trace("bumping up probs[HI]\n");
          probs[HI] += (probs[LO] /= 2);
          normalize(probs);
        } else if (nLinks[LO] > nLinks[HI] + nLinks[MD]) {
          trace("bumping up probs[LO]\n");
          probs[LO] += (probs[HI] /= 2);
          normalize(probs);
        } else {
          trace("no change\n");
        }
      }
      trace("probs: " + arrayStr(probs, DF_PROB) + "\n");
      // play gamble!!
      double rnd = RND.nextDouble();
      int lvl = -1;
      double pyf = Double.NaN;
      if (rnd < probs[HI]) {
        lvl = HI;
        pyf = g.rets[HI];
      } else if (rnd < probs[HI] + probs[MD]) {
        lvl = MD;
        pyf = g.rets[MD];
      } else {
        lvl = LO;
        pyf = g.rets[LO];
      }
      trace("rnd=" + rnd + " lvl=" + lvl + " pyf=" + pyf + "\n");
      Outcome outcome = new Outcome(i, attrSway, nLinks, probs, rnd, lvl, pyf);
      outcomes[g.id] = outcome;
      g.outcomes = (Outcome[]) add(g.outcomes, outcome);
      debug(DF_INT.format(i) + ": g: " + g + " --> " + outcome + "\n");
    }
    return new Game(turn, gambleOrder, outcomes);
  }

  // pick 2 attrs to be favorable & 2 to be unfavorable
  void assignHiddenAttrs() {

    ArrayList al = new ArrayList(16);
    for (int i = 0; i < 16; i++)
      al.add(new Integer(i));
    Collections.shuffle(al);
    attrFav1 = ((Integer) al.get(0)).intValue();
    attrFav2 = ((Integer) al.get(1)).intValue();
    attrUnfav1 = ((Integer) al.get(2)).intValue();
    attrUnfav2 = ((Integer) al.get(3)).intValue();
    out("attrs: fav: " + attrFav1 + "," + attrFav2 + " unfav: " + attrUnfav1
        + "," + attrUnfav2 + "\n");
  }

  void readData() {
    String s = "#gamble(gambleid, high return, high prob, medium return, med prob, low return, low prob)\n" +
    		"000, 09.340, 0.066, 02.393, 0.455, 00.610, 0.478\n" +
    		"001, 05.66, 0.225, 01.500, 0.421, 00.140, 0.354\n" +
    		"002, 06.835, 0.128, 01.772, 0.489, 00.680, 0.383\n" +
    		"003, 06.539, 0.191, 01.458, 0.483, 00.137, 0.325\n" +
    		"004, 04.954, 0.234, 01.605, 0.510, 00.091, 0.256\n" +
    		"005, 04.748, 0.162, 02.304, 0.475, 00.374, 0.363\n" +
    		"006, 03.251, 0.329, 01.762, 0.421, 00.755, 0.250\n" +
    		"007, 04.480, 0.235, 01.900, 0.425, 00.414, 0.340\n" +
    		"008, 03.622, 0.386, 01.211, 0.421, 00.480, 0.193\n" +
    		"009, 04.009, 0.302, 01.459, 0.514, 00.221, 0.184\n" +
    		"#gambleatts(gambleid, gambleclass)\n" +
    		"000, 15\n" +
    		"001, 7\n" +
    		"002, 8\n" +
    		"003, 4\n" +
    		"004, 9\n" +
    		"005, 5\n" +
    		"006, 11\n" +
    		"007, 3\n" +
    		"008, 14\n" +
    		"009, 2\n" +
    		"#link(gambleid, gambleid)\n" +
    		"003, 002\n" +
    		"003, 008\n" +
    		"002, 008\n" +
    		"006, 001\n";
      
    debug("readData( " + s + " )\n");
    try {
      BufferedReader br = new BufferedReader(new StringReader(s));
      ArrayList[] als = new ArrayList[3];
      for (int i = 0; i < 3; i++)
        als[i] = new ArrayList();
      int section = -1;
      String line = null;
      while ((line = br.readLine()) != null) {
        line = line.trim();
        if (line.length() == 0)
          continue;
        if (line.charAt(0) == '#') {
          // new section of data file
          if (line.indexOf("gamble(") != -1)
            section = 0;
          else if (line.indexOf("gambleatts(") != -1)
            section = 1;
          else if (line.indexOf("link(") != -1)
            section = 2;
          else
            throw new RuntimeException("unexpected line in data file: " + line);
          continue;
        }
        // save all tokens as doubles
        String[] ss = line.split("[, ]+");
        double[] dd = new double[ss.length];
        for (int i = 0; i < ss.length; i++)
          dd[i] = Double.parseDouble(ss[i]);
        als[section].add(dd);
      }
      debug("read " + als[0].size() + " gambles, " + als[1].size() + " attrs, "
          + als[2].size() + " links\n");
      if (als[0].size() != als[1].size())
        throw new RuntimeException("# gambles != # attrs");
      // build gambles
      nGambles = als[0].size();
      System.out.println(nGambles);
      gambles = new Gamble[nGambles];
      for (int i = 0; i < nGambles; i++) {
        double[] ddg = (double[]) als[0].get(i);
        double[] dda = (double[]) als[1].get(i);
        if (ddg[0] != i || dda[0] != i)
          throw new RuntimeException("gambles or attrs not in order");
        if (ddg.length != 7 || dda.length != 2)
          throw new RuntimeException("wrong # of tokens in gamble or attr " + i);
        // rets & probs
        double[] rets = new double[] { ddg[1], ddg[3], ddg[5] };
        double[] probs = new double[] { ddg[2], ddg[4], ddg[6] };
        // normalize probs from file
        normalize(probs);
        // map attr string to int
        int attr = (int) dda[1];
        // System.out.println("Attr: " + attr);
        // old code for A1, A2, A3, A4 -- 12/5/10 Max
        // for ( int j = 1; j < 5; j++ )
        // if ( dda[j] == 0 )
        // ;
        // else if ( dda[j] == 1 )
        // attr += 1 << (4-j);
        // else
        // throw new RuntimeException(
        // "non-binary attr for " + i );
        // create gamble
        gambles[i] = new Gamble(i, probs, rets, attr);
      }
      // link gambles
      for (int i = 0; i < als[2].size(); i++) {
        double[] dd = (double[]) als[2].get(i);
        Gamble[] links1 = gambles[(int) dd[0]].links;
        Gamble[] links2 = gambles[(int) dd[1]].links;
        if (links1 != links2) {
          Gamble[] links3 = (Gamble[]) merge(links1, links2);
          for (int j = 0; j < links1.length; j++)
            links1[j].links = links3;
          for (int j = 0; j < links2.length; j++)
            links2[j].links = links3;
        }
      }
    } catch (Exception e) {
      out("exception reading returns and links: " + e + "\n");
      e.printStackTrace();
      out("exiting\n");
      System.exit(1);
    }
  }

  class Gamble {

    final int id;
    final double[] probs;
    final double[] rets;
    final int attr;
    final String s;
    Gamble[] links = new Gamble[] { this };
    Outcome[] outcomes = new Outcome[0];

    Gamble(int id, double[] probs, double[] rets, int attr) {
      this.id = id;
      this.probs = probs;
      this.rets = rets;
      this.attr = attr;
      s = "id=" + DF_INT.format(id) + " p={ " + arrayStr(probs, DF_PROB) + "}"
          + " r={ " + arrayStr(rets, DF_RET) + "}" + " a="
          + ((attr < 10) ? " " : "") + attr;
    }

    public String toString() {
      return s;
    }
  }

  class Turn {

    final double[] allocs;
    double[] incomes = new double[nGambles];
    double[] cumIncomeInGambleOrder = new double[nGambles];
    double totalIncome;

    Turn(double[] d) {
      allocs = d;
    }
  }

  class Game {

    final int turn;
    final int[] gambleOrder;
    final Outcome[] outcomes;

    Game(int turn, int[] gambleOrder, Outcome[] outcomes) {
      this.turn = turn;
      this.gambleOrder = gambleOrder;
      this.outcomes = outcomes;
    }

    public String toString() {
      return "game " + turn;
    }
  }

  class Outcome {

    final int idx;
    final int attrSway;
    final int[] lnkLvls;
    final double[] probs;
    final double rnd;
    final int lvl;
    final double pyf;
    final String s;

    Outcome(int idx, int attrSway, int[] lnkLvls, double[] probs, double rnd,
        int lvl, double pyf) {
      this.idx = idx;
      this.attrSway = attrSway;
      this.lnkLvls = lnkLvls;
      this.probs = probs;
      this.rnd = rnd;
      this.lvl = lvl;
      this.pyf = pyf;
      s = "@=" + DF_INT.format(idx) + " a=" + attrSway + " l=" + lnkLvls[HI]
          + lnkLvls[MD] + lnkLvls[LO] + " p={ " + arrayStr(probs, DF_PROB)
          + "}" + " r=" + DF_PROB.format(rnd) + " l=" + LVLS[lvl] + " p="
          + DF_RET.format(pyf);
    }

    public String toString() {
      return s;
    }
  }

  class Player {

    String name;
    ArrayList<Turn> turns = new ArrayList<Turn>();
    boolean alive = true;
    boolean haveNewAlloc = false;
    double[] newAllocs;
    int nGames = 0;
    int nWins = 0;
    double cumWealth = 1.0;
    Turn lastTurn = null;

  }

  class Viz extends JPanel {

    Font f1 = new Font("Dialog", Font.BOLD, 12);
    Font f2 = new Font("Dialog", Font.PLAIN, 12);

    FontMetrics fm = getFontMetrics(f2);

    JButton bPlay, bHum;
    JPanel pOutcomes, pPlayers;

    int idxOrder = -1;
    double[] rets = new double[nGambles];
    double[] drs = new double[nGambles];
    Color[] colors = new Color[nGambles];

    AffineTransform atVert = AffineTransform.getRotateInstance(-Math.PI / 2);

    Viz() {
      //super("Portfolio Simulator");
      buildGUI();
      addHumanGUI();
/*      addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          System.exit(0);
        }
      });*/
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
        // TODO -- add labels to each result (feeds to PaintComponents)
        repaint();
        applet.repaint();
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
      bHum = new JButton("Add Human!");
      bHum.setFont(f1);
      //AJ: do not need this button for this demo
      bHum.setVisible(false);
      bPlay.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          bPlay.setEnabled(false);
          (new Thread() {
            public void run() {
              synchronized (players) {
                // out( "gui triggered round\n" );
                Game g = SimulatorWithoutSockets.this.play();
                animateGame(g);
                sendFeedback(g);
                bPlay.setEnabled(true);
                repaint();
                applet.repaint();
              }
            }
          }).start();
        }
      });

      // added by Max -- 12/12/10
      bHum.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          bHum.setEnabled(false);
          (new Thread() {
            public void run() {
              out("gui triggered human\n");
              // nGambles from above
              // datafile from above
              HumanPlayerWithoutSockets h = new HumanPlayerWithoutSockets(nGambles,
                  SimulatorWithoutSockets.this, applet);
              //AJ added 12/12 just in case we want to refer to them. 
              //like kill them.
              humanPlayers.add(h);
              bHum.setEnabled(true);
              repaint();
              applet.repaint();
            }
          }).start();
        }
      });

      JPanel pTop = new JPanel();
      pTop.add(lAttrs);
      pTop.add(bPlay);
      pTop.add(bHum);
      pOutcomes = new OutcomesPanel();
      Box boxNorth = Box.createVerticalBox();

      boxNorth.add(pTop);
      boxNorth.add(pOutcomes);
      boxNorth.add(Box.createVerticalStrut(5));
      pPlayers = new JPanel();
      pPlayers.setLayout(new GridLayout(1, 0, 2, 2));
      JPanel pane = new JPanel();
      pane.setLayout(new BorderLayout());
      pane.setBorder(new EmptyBorder(5, 5, 5, 5));
      pane.add(boxNorth, BorderLayout.NORTH);
      pane.add(pPlayers, BorderLayout.CENTER);
      pane.setPreferredSize(new Dimension(500, 700));
     
      applet.getContentPane().add(pane);
      
    }
    private HumanPlayerWithoutSockets h; 
    public void addHumanGUI(){
      System.out.println("gui triggered human\n");
      h = new HumanPlayerWithoutSockets(nGambles,
          SimulatorWithoutSockets.this, applet);
      //AJ added 12/12 just in case we want to refer to them. 
      humanPlayers.add(h);
      bHum.setEnabled(true);
      repaint();
      applet.repaint();
    }

    class OutcomesPanel extends JPanel {

      OutcomesPanel() {
        super();
        setPreferredSize(new Dimension(0, 100));
        setBorder(new BevelBorder(BevelBorder.LOWERED));
      }

      public void paintComponent(Graphics g) {
        Dimension d = getSize();
        int w = d.width;
        int h = d.height;
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.BLACK);
        g.setFont(f1);
        g.drawString("Outcomes", 10, 20);
        g.drawString("Rounds played: " + roundCount, w - 300, 20);
        g.setFont(f2);
        g.drawString("# gambles played: " + (idxOrder + 1), w - 150, 20);
        int wGamble = (w - 20) / nGambles;
        int wGraph = wGamble * nGambles;
        int x0 = (w - wGraph) / 2;
        int xng = x0 + wGraph;
        int hGraph = h - 25;
        int hUnit = hGraph / 6;
        int y0 = h - 8;
        int y2 = y0 - (2 * hUnit);
        int pad = 2 * wGamble;
        g.setColor(Color.LIGHT_GRAY);
        g.drawLine(x0 - pad, y0, xng + pad, y0);
        g.drawLine(x0 - pad, y2, xng + pad, y2);
        for (int i = 0; i < nGambles; i++) {
        	// TODO - use g.drawString(string, x, y)
          if (drs[i] == 0)
            continue;
          int rh;
          int ry;
          if (drs[i] < 0) {
            rh = (int) Math.round(-drs[i] * hUnit);
            ry = y2;
          } else {
            rh = (int) Math.round(drs[i] * hUnit);
            ry = y2 - rh + 1;
          }
          // if bar height rounded to zero, make it 1 pxl
          if (rh == 0)
            rh = 1;
          // draw the number starting 2/5ths of the way in 5 pixels above the bar
          g.setColor(colors[i]);
          g.fillRect(x0 + wGamble * i, ry, wGamble, rh);
          g.drawString(i + "", x0 + wGamble*2/5 + 1 + wGamble * i, ry - 5);
        }
      }
    }

    class PlayerPanel extends JPanel {

      Player p;

      PlayerPanel(Player p) {
        super();
        this.p = p;
        setBorder(new BevelBorder(BevelBorder.LOWERED));
      }

      public void paintComponent(Graphics g) {
        Dimension d = getSize();
        int w = d.width;
        int h = d.height;
        int precision;

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        if (p.alive)
          g.setColor(Color.BLACK);
        else
          g.setColor(Color.LIGHT_GRAY);
        g.setFont(f1);
        g.drawString((p.name == null) ? "" : p.name, 10, 20);
        DecimalFormat df = new DecimalFormat("0.0000000");
        g.drawString("$ " + df.format(p.cumWealth), 10, 40);
        g.setFont(f2);
        String status;
        if (p.alive)
          if (p.haveNewAlloc)
            status = "ready";
          else
            status = "waiting";
        else
          status = "disconnected";
        g.drawString(status, 10, 57);
        g.drawString("# wins: " + p.nWins, 10, 71);
        // write/draw upwards, so rotate
        Graphics2D g2d = (Graphics2D) g;
        AffineTransform at = g2d.getTransform();
        g2d.rotate(-Math.PI / 2);
        // now that we are rotated, specify coords
        // for drawing ops as -y,x instead of x,y
        // draw current turn allocs & income
        if (p.lastTurn != null) {
          Turn t = p.lastTurn;
          // draw allocs
          int wAllocs = w - 75;
          int hAllocs = h - 90;
          // first draw outline
          g2d.setColor(Color.BLACK);
          g2d.drawRect(-(h - 9), 20, hAllocs + 1, wAllocs + 1);
          // fill in bar
          double allocRem = 1;
          int barRem = hAllocs;
          int pxlStart = -(h - 9);
          for (int i = 0; i < nGambles; i++) {
            if (t.allocs[i] == 0)
              continue;
            double fracAllocUsing = t.allocs[i] / allocRem;
            int barUsing = (int) Math.round(fracAllocUsing * barRem);
            if (rets[i] != 0 && barUsing > 0) {
              g.setColor(colors[i]);
              g.fillRect(pxlStart, 21, barUsing, wAllocs);
            }
            barRem -= barUsing;
            pxlStart += barUsing;
            allocRem -= t.allocs[i];
          }
          double inc = (idxOrder == -1) ? 0
              : t.cumIncomeInGambleOrder[idxOrder];
          String incStr = DF_DLRS.format(inc);
          System.out.print(incStr);
          int incStrLen = (int) Math.ceil(fm.stringWidth(incStr));
          g2d.setColor(Color.BLACK);
          g2d.setFont(f2);
          g2d.drawString(incStr, -(h - 55 + incStrLen), (w - 36));
          g2d.setColor(Color.GRAY);
          int incBarLen = (int) ((h - 135) * inc / maxTotalIncome);
          g2d.fillRect(-(h - 60), (w - 45), incBarLen, 9);
        }
        // draw cumulative wealth
        String cwStr = DF_DLRS.format(p.cumWealth);
        int cwStrLen = (int) Math.ceil(fm.stringWidth(cwStr));
        g2d.setColor(Color.BLACK);
        g2d.setFont(f2);
        g2d.drawString(cwStr, -(h - 55 + cwStrLen), (w - 15));
        g2d.setColor(Color.GRAY);
        int cwBarLen = (int) ((h - 135) * p.cumWealth / maxCumWealth);
        g2d.fillRect(-(h - 60), (w - 24), cwBarLen, 9);
        // reset transform
        g2d.setTransform(at);
      }
    }
  }

  static String arrayStr(double[] dd, DecimalFormat df) {
    StringBuffer sb = new StringBuffer(dd.length
        * (df.toPattern().length() + 1));
    for (int i = 0; i < dd.length; i++)
      sb.append(df.format(dd[i])).append(' ');
    return sb.toString();
  }

  static Object[] add(Object[] in, Object val) {
    Class c = val.getClass();
    int len = in.length + 1;
    Object[] out = (Object[]) Array.newInstance(c, len);
    for (int i = 0; i < in.length; i++)
      out[i] = in[i];
    out[in.length] = val;
    return out;
  }

  static Object merge(Object[] in1, Object[] in2) {
    Class c = in1[0].getClass();
    int len = in1.length + in2.length;
    Object[] out = (Object[]) Array.newInstance(c, len);
    int count = 0;
    for (int i = 0; i < in1.length; i++)
      out[count++] = in1[i];
    for (int i = 0; i < in2.length; i++)
      out[count++] = in2[i];
    return out;
  }

  static void normalize(double[] d) {
    double total = 0;
    for (int i = 0; i < d.length; i++)
      total += d[i];
    for (int i = 0; i < d.length; i++)
      d[i] /= total;
  }

  static void trace(String s) {
    if (TRACE)
      out(s);
  }

  static void debug(String s) {
    if (DEBUG)
      out(s);
  }

  static void out(String s) {
    System.out.print(s);
    System.out.flush();
  }

  public static void main(String[] args) {
    new SimulatorWithoutSockets( new JApplet());
  }
}

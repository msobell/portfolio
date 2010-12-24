import java.applet.Applet;

/**
 * Demo class that presents the simulator and the human player
 * with no sockets
 * @author ajk377
 *
 */
public class PortfolioDemo extends Applet {
  private static final long serialVersionUID = 1L;
  private SimulatorWithoutSockets sim;
  public PortfolioDemo(){
    sim = new SimulatorWithoutSockets("data.txt");
  }
  public void paint(){
    sim = new SimulatorWithoutSockets("data.txt");
  }
}

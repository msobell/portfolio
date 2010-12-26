
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JApplet;

/**
 * Demo class that presents the simulator and the human player
 * with no sockets
 * @author ajk377
 *
 */
public class PortfolioDemo extends JApplet {
  private static final long serialVersionUID = 1L;
  private SimulatorWithoutSockets sim;
  public void init(){
    
    this.setSize(new Dimension(1000, 800));
    this.getContentPane().setLayout(new FlowLayout(FlowLayout.LEADING, 3, 3));
    sim = new SimulatorWithoutSockets("data.txt", this);
    //this.add(sim.getGUI());
    //this.add(sim.getHumanGUI());
  }

}

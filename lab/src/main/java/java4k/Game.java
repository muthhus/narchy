package java4k;

import javax.swing.*;
import java.awt.*;

public interface Game  {
	
	void start();
	
	void stop();
	
	JPanel getPanel();
	
	Dimension getPreferredSize();
	
	void processAWTEvent(AWTEvent e);
	
//	public void updateGame(); 
//	public void repaintGame(); // TODO override paintComponent() in GamePanel
	
	/*
	  while(running) {
	  	repaint();
	  }
	  
	  
	  
	 */

}

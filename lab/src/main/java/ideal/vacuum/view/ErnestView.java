package ideal.vacuum.view;

import ideal.vacuum.Environment;


/**
 * The class that runs the simulation in a separate thread.
 */
public class ErnestView implements Runnable 
{
	private final MainFrame mainFrame;
	private final Environment m_environment;
	
	public ErnestView(Environment environment, MainFrame frame)
	{
		mainFrame=frame;
		m_environment = environment;
	}

	/**
	 * Run the simulation.
	 */
	@Override
	public void run()
	{
		// Initialize the agents ===
		m_environment.initAgents();
	
		// Run the simulation in an infinite loop ===
	
		while (m_environment.getMode() < Environment.SIMULATION_TERMINATE)
		{
			boolean testRun=false;
			
			mainFrame.drawGrid();
			
			m_environment.update();

//			if (testRun)
//				try { Thread.sleep(500);
//				} catch (InterruptedException e) {e.printStackTrace();}
//
//			if (mainFrame.version!=100){
//				try { Thread.sleep(50);
//				} catch (InterruptedException e) {e.printStackTrace();}
//			}
//			if (mainFrame.version == 120){
//				try { Thread.sleep(100);
//				} catch (InterruptedException e) {e.printStackTrace();}
//			}
		}
		
		m_environment.setStop();		
	}	
}

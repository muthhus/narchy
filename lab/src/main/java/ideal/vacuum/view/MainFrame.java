package ideal.vacuum.view;


import com.google.common.collect.Lists;
import ideal.vacuum.*;
import spacegraph.math.v3;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;


public class MainFrame extends JFrame implements Observer, ActionListener, KeyListener{
	public final long serialVersionUID = 1;

	public static JFrame MAIN_WIN;

	private static String WindowTitle = "Vacuum-SG V";

	private final JMenu m_file 						= new JMenu("File");
	private final JMenu m_options 					= new JMenu("Options");
	private final JMenu m_help 						= new JMenu("Help");
	private final JMenuItem m_KeyboardLayout 		= new JMenuItem("Key Stroke Short Cuts");
	private final JMenuItem m_aboutVacuum 			= new JMenuItem("About Vacuumcleaner");
	private final JMenuItem m_exit 					= new JMenuItem("Exit");
	private final JMenuItem m_configureRun        	= new JMenuItem("Configure Run...");
	private final JMenuItem m_loadBoard           	= new JMenuItem("Choose Board...");
	private final JCheckBoxMenuItem m_speakAloud    = new JCheckBoxMenuItem("Speak Aloud");
	
	private final JFileChooser m_chooser 			= new JFileChooser();
	private final JFileChooser m_boardChooser 		= new JFileChooser(); // Uses specific chooser so that it remembers the path
	
	private final MyFileFilter m_fileFilter 		= new MyFileFilter();

    private static Environment m_environment;
    public static ArrayList<ErnestModel> m_modelList;
    
    public static int version;
	
	private ErnestView m_simulationEngine; // To run in a separate thread.

	private final JPanel m_board;
	
	private Thread agentThread;
	
	private static final String PREF_W = "pref_w";
	private static final String PREF_H = "pref_h";
	private static final String PREF_X = "pref_x";
	private static final String PREF_Y = "pref_y";
	private static final String PREF_DIRTY = "pref_dirty";
	private static final String PREF_STEPS = "pref_steps";
	private static final String PREF_DELAY = "pref_delay";
	private static final String PREF_RANDOMBOARD = "pref_randomBoard";
	private static final String PREF_BOARDFILE = "pref_boardFile";
	private static final String PREF_PICTUREFILE = "pref_pictureFile";
	private static final String PREF_SPEAKALOUD = "pref_speakAloud";
	private static final String PREF_AGENTFILE = "pref_agentFile";
	private static final String PREF_AGENTTYPE = "pref_agentType";
	private static final String PREF_AGENTSHORTFILE = "pref_agentShortFile";
		
	/////////////////////////////////////////////////////////////////////////////////////////
	
	private final EnvironnementPanel m_envPanel;
	
	private final boolean m_halt = true;
	protected int index;
	
	private final JLabel m_statusBar = new JLabel();
	private final JButton m_run = new JButton("Play");
	private final JButton m_stop = new JButton("Pause");
	private final JButton m_step = new JButton("Step");
	private final JButton m_reset = new JButton("Reset");
	
	private final JButton m_arun = new JButton("Play agent");
	private final JButton m_astop = new JButton("Pause Agent");
	private final JButton m_astep = new JButton("Step Agent");
	
	private final javax.swing.Timer m_statusTimer =
		new javax.swing.Timer(100, new StatusTimerListener());

	private final ConfigureRunDlg m_configRunDlg = 
		new ConfigureRunDlg(this, m_environment);
	
	/**
	 * Main
	 * Can specify a model version
	 * @author ogeorgeon 
	 * @param strModel
	 */
	public static void main(String[] args){
		version=110;
		if (args.length == 1)
		{
            switch (args[0]) {
                case "Ernest110":
                    version = 110;
                    break;
                case "Ernest104":
                    version = 104;
                    break;
                case "Fish":
                    version = 0;
                    break;
                case "Ernest100":
                    version = 100;
                    break;
                case "Ernest120":
                    version = 120;
                    break;
                case "Ernest130":
                    version = 130;
                    break;
                default:
                    version = 100;
                    break;
            }
		}
		WindowTitle = WindowTitle + version;
		
		m_modelList= new ArrayList<>();
		m_environment= new Environment(m_modelList,version);
		
		new MainFrame();
	}

	/**
	 * Main
	 */
	public MainFrame()
	{
		super(WindowTitle);
		MAIN_WIN = this;
		// Retrieve preferences
		m_environment.initPreferences();
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		addWindowListener(new CloseHandler());

		/////////////////
		m_envPanel=new EnvironnementPanel(m_modelList,m_environment);
		

		// Initialize the board
		//try{
			String[] x = ("w w w w w w w w w w w w w w w w w w\n" +
					"w - - - - - - - - - - - - w w j i w\n" +
					"w - 5 w - - - - - + + + - w 4 4 - w\n" +
					"w - - w - - - - - - + + - w - - - w\n" +
					"w - - w w w - - - + + - - - - - - w\n" +
					"w - - - - - - - - - - - - - - - - w\n" +
					"w - - - - - - - - - - - - - - - - w\n" +
					"w - - - - - - w w w h - - - - - - w\n" +
					"w - - - - ^ - - - w w w w w - - - w\n" +
					"w - - - - - - 3 - - - - - g 2 - - w\n" +
					"w - - ^ - - - - - - + + - g - - - w\n" +
					"w w 9 - w w - - - + + - - - - - - w\n" +
					"w w w w w w w - - - - - - - - - - w\n" +
					"w w w w w w w w w w w w w w w w w w").split("\n");
			this.init(Lists.newArrayList(x));
//		}
//		catch (Exception e){
//			JOptionPane.showMessageDialog(this,
//				e.getMessage() + ".\n" +
//				"Next run will use default board file: ./" +
//				Environment.DEFAULT_BOARD,
//				"Error initializing the board",
//				JOptionPane.ERROR_MESSAGE);
//
//			// Switch back to default board file.
//			Preferences prefs = Preferences.userRoot().node("vacuum");
//			prefs.put(Environment.PREF_BOARDFILE, Environment.DEFAULT_BOARD);
//		}

		m_environment.setFrame(this);
		configureMenu();
		
		m_environment.addObserver(this);

		m_stop.setEnabled(true);
		m_run.setEnabled(true);
		m_reset.setEnabled(true);
		m_step.setEnabled(true);
		
		m_arun.setEnabled(false);
		m_astop.setEnabled(true);
		m_astep.setEnabled(false);
		
		m_stop.addActionListener(this);
		m_run.addActionListener(this);
		m_reset.addActionListener(this);
		m_step.addActionListener(this);
		
		m_astop.addActionListener(this);
		m_arun.addActionListener(this);
		m_astep.addActionListener(this);

		m_board = new JPanel(new GridLayout(1, 1));

		getContentPane().setLayout(new BorderLayout());

		getContentPane().add(m_board, BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel();

		buttonPanel.addKeyListener(this);

		buttonPanel.add(m_arun);
		buttonPanel.add(m_astop);
		buttonPanel.add(m_astep);
		
		buttonPanel.add(m_reset);
		buttonPanel.add(m_run);
		buttonPanel.add(m_stop);
		buttonPanel.add(m_step);
		JPanel statusPanel = new JPanel(new BorderLayout());
		statusPanel.add(m_statusBar, BorderLayout.CENTER);
		statusPanel.add(buttonPanel, BorderLayout.EAST);
		statusPanel.setBorder(BorderFactory.createRaisedBevelBorder());
		getContentPane().add(statusPanel, BorderLayout.SOUTH);


		//m_statusModel.pushPermStatus("Ready.");
		m_statusBar.setText("Ready.");
		m_statusBar.setPreferredSize(new Dimension(200, m_statusBar.getHeight()));
		m_statusTimer.start();

		m_chooser.setFileFilter(m_fileFilter);
		m_boardChooser.setFileFilter(m_fileFilter);
		//m_pictureChooser.setFileFilter(m_fileFilter);

		update(null, null);
		pack();
		Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    	this.setBounds( screen.x , screen.y , 800 , 500 );
		setVisible(true);
		addKeyListener(this);
		setFocusable(true);
	}
		


	public void init(List<String> lines) {
		int l_w;
		int l_h;
		int l_x = -1;
		int l_y = -1;
		index=0;
		m_modelList.clear();


		l_h = lines.size();
		l_w = (lines.get(0).length() + 1) / 2;

		if (l_h <= 0 || l_w <= 0)
            throw new IllegalStateException("Invalid width or height!");

		System.out.println("length: " + l_h + " width: " + l_w);

		m_environment.init(l_w, l_h);

		int y = 0;

		for (Iterator i = lines.iterator(); i.hasNext(); )
        {
            String line = (String)i.next();
            if (((line.length() + 1) / 2) != l_w)
                throw new
                    IllegalStateException("Width must be consistent!");

            String[] square = line.split(" ");

            for (int x = 0; x < l_w; x++)
            {
                m_environment.m_blocks[x][l_h-1-y]= Environment.empty;

                // Static fish
                if (square[x].equals("*"))
                {
                    m_environment.m_blocks[x][l_h-1-y]= Environment.fish;
                }

                // Fish agents
                if (square[x].equals("+"))
                {
                    //int index=m_modelList.size();

                    m_modelList.add(new FishModel(index));
                    m_modelList.get(index).setEnvironnement(m_environment); // OG
                    m_modelList.get(index).init(l_w, l_h);
                    m_modelList.get(index).setFrame(this);

                    m_modelList.get(index).mPosition.x = x;
                    m_modelList.get(index).mPosition.y = l_h-1 - y;
                    m_modelList.get(index).mPosition.z = 0;
                    m_modelList.get(index).mOrientation.x = 0;
                    m_modelList.get(index).mOrientation.y = 0;
                    m_modelList.get(index).mOrientation.z = 0.1f;
                    m_modelList.get(index).mTranslation.set(new v3());
                    m_modelList.get(index).mRotation.set(new v3());

//						m_modelList.get(index).setEnvironnement(m_environment);

                    index++;
                }

                // Ernest agents
                if (square[x].equalsIgnoreCase("^") || square[x].equalsIgnoreCase(">") ||
                    square[x].equalsIgnoreCase("v") || square[x].equalsIgnoreCase("<"))
                {
                    //int index=m_modelList.size();

//                    if (version==0)   m_modelList.add(new FishModel(index));
//                    else if (version==130)
                        m_modelList.add(
                        		new Ernest130Model(index)
								//new FishModel(index)
						);
//					else
//						throw new RuntimeException("unknown version");

                    m_modelList.get(index).setEnvironnement(m_environment); // OG
                    m_modelList.get(index).init(l_w, l_h);
                    m_modelList.get(index).setFrame(this);

                    m_modelList.get(index).mPosition.x = x;
                    m_modelList.get(index).mPosition.y = l_h-1 - y;
                    m_modelList.get(index).mPosition.z = 0;
                    m_modelList.get(index).mOrientation.x = 0;
                    m_modelList.get(index).mOrientation.y = 0;
                    m_modelList.get(index).mTranslation.set(new v3());
                    m_modelList.get(index).mRotation.set(new v3());

                    if (square[x].equalsIgnoreCase("^"))
                        m_modelList.get(index).mOrientation.z = (float) Math.PI/2;
                    else if (square[x].equalsIgnoreCase(">"))
                        m_modelList.get(index).mOrientation.z = 0;
                    else if (square[x].equalsIgnoreCase("v"))
                        m_modelList.get(index).mOrientation.z = (float) -Math.PI/2;
                    else if (square[x].equalsIgnoreCase("<"))
                        m_modelList.get(index).mOrientation.z = (float) Math.PI;

                    //m_modelList.get(index).setEnvironnement(m_environment);

                    index++;
                }

                if (Character.isLetter(square[x].toCharArray()[0]))
                {
                    int code = 'a';
                    code = square[x].toCharArray()[0] - code;
                    // Agent on target
                    if (square[x].equalsIgnoreCase("x"))
                    {
                        l_x = x;
                        l_y = y;
                    }
                    // Wall
                    else if (square[x].equalsIgnoreCase("w")
                         ||  square[x].equalsIgnoreCase("i")
                         ||  square[x].equalsIgnoreCase("j"))
                    {
                        m_environment.m_blocks[x][l_h-1-y]= Environment.wall;
                    }
                    else
                    {
                        if (square[x].equalsIgnoreCase("g")){
                            m_environment.m_blocks[x][l_h-1-y]=m_environment.wall2;
                        }
                        else if (square[x].equalsIgnoreCase("h")){
                            m_environment.m_blocks[x][l_h-1-y]=m_environment.wall3;
                        }
                        else m_environment.m_blocks[x][l_h-1-y]= Environment.empty;
                    }
                }
                // Singular dirty square
                if (Character.isDigit(square[x].toCharArray()[0]))
                {
                    switch (Integer.parseInt(square[x]) )
                    {
                        case 2: m_environment.m_blocks[x][l_h-1-y]=m_environment.alga4; break;
                        case 3: m_environment.m_blocks[x][l_h-1-y]=m_environment.alga5; break;
                        case 4: m_environment.m_blocks[x][l_h-1-y]= Environment.alga1; break;
                        case 5: m_environment.m_blocks[x][l_h-1-y]=m_environment.alga2; break;
                        case 9: m_environment.m_blocks[x][l_h-1-y]=m_environment.alga3; break;
                        default: break;
                    }
                }
            }
            y++;
        }

		if (m_modelList.size()<=0)
            throw new
                IllegalStateException("error 404 : Agents not found!");

		// Initialize the display index to the first Ernest agent.
		int index=0;
		boolean found=false;
		while (index<m_modelList.size() && !found){
			if (m_modelList.get(index).isAgent()) found=true;
			else index++;
		}
		if (found) m_environment.setDisplay(index);

		// Force the display index
		//m_environment.setDisplay(8);


		System.out.println("initialized ") ;
		//m_environment.setStop();

		//if (agentThread != null) agentThread.interrupt();

		//m_environment.initAgents();
		m_simulationEngine = new ErnestView(m_environment,this);
		//agentThread = new Thread(getErnestView());
		agentThread = new Thread(m_simulationEngine);
		agentThread.start();
		m_statusBar.setText("initialized");

	}

	/**
	 * Performs actions
	 */
	@Override
    public void actionPerformed(ActionEvent e)
	{
		m_modelList.get(0).setEventThread(Thread.currentThread());

		// Play the simulation ******
		if (e.getSource() == m_run)
		{
			System.out.println("Run Ernest ") ;
			m_environment.setRun();
			m_statusBar.setText("Playing");
			m_run.setEnabled(false);
			m_stop.setEnabled(true);
			m_step.setEnabled(false);
		}
		
		// Pause the simulation *****
		else if (e.getSource() == m_stop)
		{
			m_environment.setStop();
			m_run.setEnabled(true);
			m_stop.setEnabled(false);
			m_step.setEnabled(true);
			m_statusBar.setText("Pause");			
		}
		
		// Step the simulation *****
		else if (e.getSource() == m_step)
		{
			m_environment.setStep();
			System.out.println("Simulation Step") ;
			m_statusBar.setText("Simulation Step");
		}
		
		// Play the selected agent ******
		else if (e.getSource() == m_arun)
		{
			System.out.println("Run Ernest "+ m_environment.identDisplay+ ' ') ;
			m_modelList.get(m_environment.indexDisplay).cognitiveMode = ErnestModel.AGENT_RUN;
		}
		
		// Pause the selected agent ******
		else if (e.getSource() == m_astop)
		{
			//System.out.println("************* "+m_environment.identDisplay+" ; "+m_environment.indexDisplay+" ; "+m_modelList.size());
			System.out.println("Stop Ernest "+ m_environment.identDisplay+ ' ') ;
			m_modelList.get(m_environment.indexDisplay).cognitiveMode = ErnestModel.AGENT_STOP;
		}
		
		// Step the selected agent *****
		else if (e.getSource() == m_astep)
		{
			m_modelList.get(m_environment.indexDisplay).cognitiveMode = ErnestModel.AGENT_STEP;
			System.out.println("Agent "+m_environment.identDisplay+" Step") ;
			m_statusBar.setText("Agent "+m_environment.identDisplay+" Step");
		}
		
		// Reset the board ******
		else if (e.getSource() == m_reset){
			m_statusBar.setText("Ready");
			m_environment.setTerminate();
//			try
//			{
//				this.init(m_environment.getBoardFileName());
//				this.repaint();
//			}
//			catch (Exception ex)
//			{
//				JOptionPane.showMessageDialog(this,
//					"Error while initializing the board! (Check board file)\n" +
//					e.getClass().toString() + ": " + ex.getMessage(),
//					"Error!",
//					JOptionPane.ERROR_MESSAGE);
//			}
			m_run.setEnabled(true);
			m_stop.setEnabled(false);
			m_step.setEnabled(false);
		}
		
		// Quit
		else if (e.getSource() == m_exit)
		{
			System.exit(0);
		}
		
//		// Load Board
//		else if (e.getSource() == m_loadBoard)
//		{
//			m_fileFilter.set(".TXT", "Board Files");
//			m_boardChooser.setVisible(true);
//			int returnVal = m_boardChooser.showOpenDialog(this);
//			if(returnVal == JFileChooser.APPROVE_OPTION)
//			{
//				try
//				{
//					File boardFile = m_boardChooser.getSelectedFile();
//					m_environment.setBoardFileName(boardFile.getAbsolutePath());
//					this.init(boardFile.getAbsolutePath());
//				}
//				catch (Exception ex)
//				{
//				JOptionPane.showMessageDialog(this,
//					"Invalid board file!\n" +
//					ex.getClass().toString() + ": " + ex.getMessage(),
//					"Error!",
//					JOptionPane.ERROR_MESSAGE);
//				}
//			}
//		}
		else if (e.getSource() == m_configureRun)
		{
			m_configRunDlg.setVisible(true);
		}
		else if (e.getSource() == m_speakAloud)
		{
			m_modelList.get(0).setSpeakAloud(m_speakAloud.isSelected());
		}
		// Save the preferences
		//putPreferences();

		m_arun.setEnabled(m_modelList.get(m_environment.indexDisplay).cognitiveMode != ErnestModel.AGENT_RUN);
		m_astop.setEnabled(m_modelList.get(m_environment.indexDisplay).cognitiveMode == ErnestModel.AGENT_RUN);
		m_astep.setEnabled(m_modelList.get(m_environment.indexDisplay).cognitiveMode != ErnestModel.AGENT_RUN);
	}

	@Override
    public void update(Observable o, Object arg)
	{
		/*if (m_modelList.get(0).isAgentStopped())
		{
			m_file.setEnabled(true);
			m_options.setEnabled(true);
			m_help.setEnabled(true);
			//m_statusModel.pushPermStatus("Pause");

			setTitle(TITLE + " - " + m_modelList.get(0).getVersion());
		}
		else
		{
			m_file.setEnabled(false);
			m_options.setEnabled(false);
			m_help.setEnabled(false);

			//m_statusModel.pushPermStatus("Playing");
		}*/

		drawGrid();
		
		m_envPanel.repaint();
		getContentPane().validate();
		//repaint();
		//this.repaint();
	}
	/**
	 * Update all the squares in the grid from the model
	 */
	public void drawGrid()
	{
		int l_w = m_environment.getWidth();
		int l_h = m_environment.getHeight();
		// handle mouse events from continuous environment
		int c= m_envPanel.getClicked();
		
		// click left : change agent
		if (c == EnvironnementPanel.CLICK_AGENT){
			int id=m_environment.agentId(m_envPanel.m_FclickX, m_envPanel.m_FclickY);
			if (id!=-1){
				m_environment.setDisplay(id);
				m_statusBar.setText("Agent "+m_environment.identDisplay);
				m_arun.setEnabled(m_modelList.get(m_environment.indexDisplay).cognitiveMode != ErnestModel.AGENT_RUN);
				m_astop.setEnabled(m_modelList.get(m_environment.indexDisplay).cognitiveMode == ErnestModel.AGENT_RUN);
				m_astep.setEnabled(m_modelList.get(m_environment.indexDisplay).cognitiveMode == ErnestModel.AGENT_STOP);
			}
		}
		
		// click wheel : add or remove static fish
		if (c == EnvironnementPanel.CLICK_TARGET){
			if (m_environment.isFood(m_envPanel.m_clickX,l_h-1-m_envPanel.m_clickY)){
				m_environment.setBlock(m_envPanel.m_clickX, l_h-1-m_envPanel.m_clickY, Model.empty);
				m_environment.traceUserEvent("remove_food", m_envPanel.m_clickX, l_h-1-m_envPanel.m_clickY);
			}
			else{
				if (m_environment.isEmpty(m_envPanel.m_clickX,l_h-1-m_envPanel.m_clickY)){
					m_environment.setBlock(m_envPanel.m_clickX, l_h-1-m_envPanel.m_clickY, Model.fish);
					m_environment.traceUserEvent("add_food", m_envPanel.m_clickX, l_h-1-m_envPanel.m_clickY);
				}
			}
		}
		
		// click right : add or remove wall
		if (c == EnvironnementPanel.CLICK_WALL){
			if (m_environment.isWall(m_envPanel.m_clickX,l_h-1-m_envPanel.m_clickY)){
				m_environment.setBlock(m_envPanel.m_clickX, l_h-1-m_envPanel.m_clickY, Model.empty);
				m_environment.traceUserEvent("remove_wall", m_envPanel.m_clickX, l_h-1-m_envPanel.m_clickY);
			}
			else{
				if (m_environment.isEmpty(m_envPanel.m_clickX,l_h-1-m_envPanel.m_clickY)){
					m_environment.setBlock(m_envPanel.m_clickX, l_h-1-m_envPanel.m_clickY, Model.wall);
					m_environment.traceUserEvent("add_wall", m_envPanel.m_clickX, l_h-1-m_envPanel.m_clickY);
				}
			}
		}
		
		// click wheel + shift : add moving fish
		if (c == EnvironnementPanel.CLICK_MOVING_TARGET){
			if (!m_environment.isWall(m_envPanel.m_clickX,l_h-1-m_envPanel.m_clickY)){
				int index2=m_modelList.size();
			
				m_modelList.add(new FishModel(index));
				try {
					//m_modelList.get(index2).init(m_w, m_h);
					m_modelList.get(index2).init(m_environment.getWidth(), m_environment.getHeight());
				} catch (Exception e) {
					e.printStackTrace();
				}
				m_modelList.get(index2).setFrame(this);
			
				m_modelList.get(index2).mPosition.x = m_envPanel.m_FclickX;
				m_modelList.get(index2).mPosition.y = l_h - 0.5f - m_envPanel.m_FclickY;
				m_modelList.get(index2).mPosition.z = 0;
				m_modelList.get(index2).mOrientation.x = 0;
				m_modelList.get(index2).mOrientation.y = 0;
				m_modelList.get(index2).mOrientation.z = 0.1f;
				m_modelList.get(index2).mTranslation.set(new v3());
				m_modelList.get(index2).mRotation.set(new v3());
			
				m_modelList.get(index2).setEnvironnement(m_environment);
				m_modelList.get(index2).initErnest();
				
				index++;
			}
		}
		
		// click right + shift : add or remove alga
		if (c == EnvironnementPanel.CLICK_ALGA){
			if (m_environment.isAlga(m_envPanel.m_clickX,l_h-1-m_envPanel.m_clickY)){
				m_environment.setBlock(m_envPanel.m_clickX, l_h-1-m_envPanel.m_clickY, Model.empty);
				m_environment.traceUserEvent("remove_water", m_envPanel.m_clickX, l_h-1-m_envPanel.m_clickY);
			}
			else{
				if (m_environment.isEmpty(m_envPanel.m_clickX,l_h-1-m_envPanel.m_clickY)){
					m_environment.setBlock(m_envPanel.m_clickX, l_h-1-m_envPanel.m_clickY, Model.wall2);
					m_environment.setBlock(m_envPanel.m_clickX, l_h-1-m_envPanel.m_clickY, Model.alga1);
				}
			}
		}
		// click right + ctrl + shift : add or remove brick
		if (c == EnvironnementPanel.CLICK_BRICK){
			if (m_environment.isWall(m_envPanel.m_clickX,l_h-1-m_envPanel.m_clickY)){
				m_environment.setBlock(m_envPanel.m_clickX, l_h-1-m_envPanel.m_clickY, Model.empty);
				m_environment.traceUserEvent("remove_water", m_envPanel.m_clickX, l_h-1-m_envPanel.m_clickY);
			}
			else{
				if (m_environment.isEmpty(m_envPanel.m_clickX,l_h-1-m_envPanel.m_clickY)){
					m_environment.setBlock(m_envPanel.m_clickX, l_h-1-m_envPanel.m_clickY, Model.wall2);
					m_environment.traceUserEvent("add_water", m_envPanel.m_clickX, l_h-1-m_envPanel.m_clickY);
				}
			}
		}
		
		resizeGrid();
		m_envPanel.repaint();
	}

	/**
	 * Creates the grid board if it does not exist or if the grid's size has changed.
	 * @author mchohen
	 */
	private void resizeGrid()
	{
		//m_envPanel.setPreferredSize(new Dimension(40*m_envPanel.m_w,40*m_envPanel.m_h));
		m_envPanel.setPreferredSize(new Dimension(40 * m_environment.getWidth(),40 * m_environment.getHeight()));
		
		if (m_board != null) // Not sure why sometimes m_board is null
			m_board.add(m_envPanel);
	}

	/**
	 * Draw the Menus
	 * @author mchohen
	 * @author mfriedrich
	 * @author ogeorgeon 
	 */
	private void configureMenu()
	{
		// mnemonics...
		m_file.setMnemonic(KeyEvent.VK_F);
		m_exit.setMnemonic(KeyEvent.VK_X);
		
		m_options.setMnemonic(KeyEvent.VK_O);
		m_configureRun.setMnemonic(KeyEvent.VK_R);
		m_loadBoard.setMnemonic(KeyEvent.VK_L);

		// file menu...
		m_file.add(m_exit);
		
		m_exit.addActionListener(this);

		// options menu...
		m_options.add(m_configureRun);
		m_options.add(m_loadBoard);
		m_options.add(m_speakAloud);

		m_loadBoard.addActionListener(this);
		m_configureRun.addActionListener(this);

		if (!m_modelList.isEmpty())
			m_speakAloud.setSelected(m_modelList.get(0).getSpeakAloud());
		m_speakAloud.addActionListener(this);

		// help menu...
		m_help.add(m_aboutVacuum);
		m_help.add(m_KeyboardLayout);
		m_KeyboardLayout.addActionListener(this);
		m_aboutVacuum.addActionListener(this);

		// menu bar...
		JMenuBar bar = new JMenuBar();
		bar.add(m_file);
		bar.add(m_options);
		bar.add(m_help);
		setJMenuBar(bar);
		//m_env.setJMenuBar(bar);
	}

	private static class StatusTimerListener implements ActionListener
	{
		@Override
        public void actionPerformed(ActionEvent e)
		{
			//m_statusBar.setText(m_statusModel.getNextStatus());
		}
	}

	private class CloseHandler extends WindowAdapter
	{
		@Override
        public void windowClosing(WindowEvent e){
			m_statusTimer.stop(); 
			//if (m_ernest != null) m_ernest.close();
		}
	}
	
	/**
	 * Loads the Ernest execution class  
	 * @author ogeorgeon 
	 */
	private ErnestView getErnestView()
	{
		try
		{
			if (m_simulationEngine == null)
				//m_ernest = new ErnestView(m_modelList,this, m_environment);		
				m_simulationEngine = new ErnestView(m_environment,this);		
		}
		catch (NoClassDefFoundError e)
		{
			JOptionPane.showMessageDialog(this, 
					"Error loading the Ernest engine!\n" + 
					"Please restart the environment with ernest.jar included in the classpath.", 
					"Error!", 
					JOptionPane.ERROR_MESSAGE);
		}
		return m_simulationEngine;
	}

	@Override
    public void keyTyped(KeyEvent e) {
	}

	@Override
    public void keyPressed(KeyEvent e) {
		/* char c = e.getKeyChar();
		 System.out.println(c);
		 int i=m_environment.identDisplay;
		 
		 switch(c){
		 case '+': m_environment.setDisplay(i+1);
		 		   break;
		 case '-': m_environment.setDisplay(i-1);
		   		   break;
		 default:  break;
		 }*/
	}

	@Override
    public void keyReleased(KeyEvent e)
	{
	}
	
}

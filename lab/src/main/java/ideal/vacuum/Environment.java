package ideal.vacuum;


import ideal.vacuum.view.EnvironnementPanel;
import ideal.vacuum.view.MainFrame;
import spacegraph.math.v3;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.prefs.Preferences;


public class Environment extends Observable {
	
	// tactile properties
	public static final int EMPTY         = 0;
	public static final int SMOOTH        = 1;
	public static final int FOOD          = 2;
	public static final int HARD		  = 3;
	public static final int CUDDLE        = 4;
	
	// visual properties
	public static final Color FIELD_COLOR = Color.white;
	public static final Color WALL1       = new Color(100,100,100); // new Color(  0,128,  0);
	public static final Color WALL2       = new Color(  0,230, 92);
	public static final Color WALL3       = new Color(  0,230,160);
	public static final Color ALGA1       = new Color(115,230,  0);
	public static final Color ALGA2       = new Color( 46,230,  0);
	public static final Color ALGA3       = new Color(  0,230,230);
	public static final Color ALGA4       = new Color(230,207,  0);
	public static final Color ALGA5       = new Color(184,230,  0);
	public static final Color FISH1       = new Color(150,128,255);
	public static final Color AGENT       = new Color(100,100,100);
	
	public static Block empty=new Block(EMPTY, FIELD_COLOR,"empty");
	public static Block wall =new Block(HARD , WALL1,"wall1");
	public Block wall2=new Block(HARD , WALL2,"wall2");
	public Block wall3=new Block(HARD , WALL3,"wall3");
	public static Block alga1=new Block(SMOOTH,ALGA1,"alga1");
	public Block alga2=new Block(SMOOTH,ALGA2,"alga2");
	public Block alga3=new Block(SMOOTH,ALGA3,"alga3");
	public Block alga4=new Block(SMOOTH,ALGA4,"alga4");
	public Block alga5=new Block(SMOOTH,ALGA5,"alga5");
	public static Block fish =new Block(FOOD  ,FISH1,"fish");
	
	// trap objects
	public Block green_fish=new Block(FOOD  ,WALL1,"green_fish");
	public Block mauve_wall=new Block(HARD  ,FISH1,"mauve_wall");
	public Block invisible_wall=new Block(HARD,FIELD_COLOR,"invisible");


	public static final int ANIM_NO       = 0;
	public static final int ANIM_BUMP     = 1;
	public static final int ANIM_RUB      = 2;
	public static final int ANIM_TOUCH    = 3;
	
	//public static final Color FIELD_COLOR = Color.white; //new Color(150, 255, 150);
	public static final Color WALL_COLOR  = new Color(0, 128, 0); // Color.getHSBColor(1/3f, 1f, 0.5f)
	public static final Color WATER_COLOR = new Color(150, 128, 255); // Color.getHSBColor(1/3f, 1f, 0.5f)
	//public static final Color WATER_COLOR = new Color(0,0,255); // Color.getHSBColor(1/3f, 1f, 0.5f)
	public static final Color FOOD_COLOR  = new Color(227, 124, 255); // Color.getHSBColor(1/3f, 1f, 0.5f)
	public static final Color THIRSTY_HUNGRY_COLOR = new Color(190, 126, 255);

	public static final int INIT_W         = 4;
	public static final int INIT_H         = 4;
	public static final int INIT_X         = 0;
	public static final int INIT_Y         = 0;
	public static final int INIT_DIRTY     = 10;
	public static final int INIT_STEPS     = 100;
	public static final int INIT_DELAY     = 500;
	
	public static final String INIT_PICTURE    = "vacuum.gif";
	
	private static final String PREF_W = "pref_w";
	private static final String PREF_H = "pref_h";
//	private static final String PREF_X = "pref_x";
//	private static final String PREF_Y = "pref_y";
	private static final String PREF_DIRTY = "pref_dirty";
//	private static final String PREF_STEPS = "pref_steps";
	private static final String PREF_DELAY = "pref_delay";
//	private static final String PREF_RANDOMBOARD = "pref_randomBoard";
	public static final String PREF_BOARDFILE = "pref_boardFile";
//	private static final String PREF_PICTUREFILE = "pref_pictureFile";
	private static final String PREF_SPEAKALOUD = "pref_speakAloud";
//	private static final String PREF_AGENTFILE = "pref_agentFile";
//	private static final String PREF_AGENTTYPE = "pref_agentType";
//	private static final String PREF_AGENTSHORTFILE = "pref_agentShortFile";
	public static final String DEFAULT_BOARD = "Board16x12.txt";
		
	protected int m_w;
	protected int m_h;
	private int m_dirtyCount;
	private int m_delay;
	private final int m_informationX = 100;
	private final int m_informationY = 100;
	
	public int[][] m_anim;
	public Block[][] m_blocks;
	public ArrayList<ErnestModel> m_modelList;
	
	public boolean lock;

	private String m_boardFileName = "";

	private static final Random m_rand = new Random();

	private final Runnable m_thread = new NotifyThread();
	private final Runnable m_mainThread;
	private Runnable m_eventThread;
	
	private boolean m_bSpeakAloud     = true;
	private boolean m_bInternalState;
	private final boolean m_status = false;

	protected EnvironnementPanel m_env;
	protected MainFrame mainFrame;
	
	public int identDisplay;
	public int indexDisplay;
	public int version;
	
	private final Map<Class<? extends FramePlugin> , FramePlugin> framePlugins;
	
	public static int SIMULATION_STOP;
	public static int SIMULATION_RUN  = 1;
	public static int SIMULATION_STEP = 2;
	public static int SIMULATION_TERMINATE = 3;
	
	public int simulationMode = SIMULATION_STOP;
	
	public Environment(ArrayList<ErnestModel> list,int v){
		m_modelList=list;
		m_mainThread = Thread.currentThread();
		identDisplay=0;
		version=v;
		framePlugins= new LinkedHashMap<>();
		

	}
	public void setEnvironnement(EnvironnementPanel env){
		m_env=env;
	}
	public void setFrame(MainFrame m){
		mainFrame=m;
	}
	
	public <T> T getPlugin( Class<T> pluginClass ) {
        return (T) this.framePlugins.get( pluginClass );
    }
	
	public boolean isPlugued( Class<? extends FramePlugin> pluginClass ) {
		return this.framePlugins.containsKey( pluginClass );
	}
	
	public void unplugFrame( Class<? extends FramePlugin> pluginClass ) {
		this.framePlugins.remove( pluginClass );
	}
	
	public void plugFrame( Class<? extends FramePlugin> pluginClass ) throws NoSuchMethodException, SecurityException, ClassNotFoundException, InterruptedException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		final Constructor<?> constructor = pluginClass.getConstructor();
		
		FramePlugin javaClass = null;
		if (constructor != null) {
			javaClass = (FramePlugin) constructor.newInstance() ;
			Environment.this.framePlugins.put( javaClass.getClass() , javaClass );
		}
	}
	
	public void animFramesPlugins( final float angleRotation, final float xTranslation ){
		for ( FramePlugin plugin : this.framePlugins.values() ) {
			plugin.anim( angleRotation , xTranslation );
		}
	}
	
	public void refreshFramesPlugins() {
		for ( FramePlugin plugin : this.framePlugins.values() ) {
			plugin.refresh();
		}
	}
	/**
	 * Initialize the grid from a board file
	 * @author mcohen
	 * @author ogeorgeon add wall and internal state panel to the grid
	 */
	public void init(int w,int h) //throws Exception
	{
		m_w=w;
		m_h=h;
		m_anim=new int[w][h];
		m_blocks=new Block[w][h];
		for (int x=0; x < w; x++)
			for (int y=0; y < h; y++)
				m_blocks[x][y] = empty;

		putPreferences();
	}
	
	public void initAgents()
	{
		for (int i=0;i<m_modelList.size();i++)
			m_modelList.get(i).initErnest();
	}
	
//	public void closeAgents()
//	{
//		for (int i=0;i<m_modelList.size();i++)
//			m_modelList.get(i).closeErnest();
//	}
//	
	public void update()
	{
		lock=false;
		if (simulationMode > SIMULATION_STOP)
		{
			for (int i=0; i < m_modelList.size(); i++)
				m_modelList.get(i).update();
		}
		if (simulationMode == SIMULATION_STEP)
			simulationMode = SIMULATION_STOP;
		
		lock=true;
	}
	
	public void setDisplay(int ident){
		int index=indexOf(ident);
		if (index>=0 && m_modelList.size()>index){
			if (m_modelList.size()>indexDisplay) m_modelList.get(indexDisplay).display=false;
			m_modelList.get(index).setDisplay();
			m_modelList.get(index).display=true;
			identDisplay=ident;
			indexDisplay=index;
			
		}
	}
	
	public void setRun()
	{
		simulationMode = SIMULATION_RUN;
	}
	
	public void setStop()
	{
		simulationMode = SIMULATION_STOP;
	}
	
	public void setStep()
	{
		simulationMode = SIMULATION_STEP;
	}
	
	public void setTerminate()
	{
		simulationMode = SIMULATION_TERMINATE;
	}
	
	public int getMode()
	{
		return simulationMode;
	}
	
	public void setEventThread(Runnable t)
	{ m_eventThread = t; }

	public boolean isWall(float x, float y){ 
		return 	(m_blocks[Math.round(x)][Math.round(y)].isWall()); 
	}
	public boolean isFood(float x, float y){ 
		return 	(m_blocks[Math.round(x)][Math.round(y)].isFood()); 
	}
	public boolean isAlga(float x, float y){ 
		return 	(m_blocks[Math.round(x)][Math.round(y)].isAlga()); 
	}
//	public boolean isAgent(float x, float y, String name)
//	{
//		for (ErnestModel agent : m_modelList)
//			if (agent.isInCell(Math.round(x), Math.round(y)) && !name.equals(agent.getName())) 
//				return true;
//				
//		return false;			
//	}
	
	public boolean isEmpty(float x, float y){
		return m_blocks[Math.round(x)][Math.round(y)].isEmpty();
	}
	
	public boolean isVisible(float x, float y)
	{
		boolean afford;
		if (inScene(Math.round(x), Math.round(y), 0))
			afford = m_blocks[Math.round(x)][Math.round(y)].isVisible();
		else 
			afford = true;
		return afford;
	}
	
	public Color seeBlock(float x, float y){
		return m_blocks[Math.round(x)][Math.round(y)].seeBlock();
	}
	
	public int touchBlock(float x, float y){
		return m_blocks[Math.round(x)][Math.round(y)].touchBlock();
	}
	
	/**
	 * @param pos The position to test in Cartesian coordinates.
	 * @return true if this position is afford walking, or false if pos is a wall 
	 */
	public boolean affordWalk(v3 pos)
	{
		boolean afford;
		if (inScene(Math.round(pos.x), Math.round(pos.y), 0))
			afford = (!m_blocks[Math.round(pos.x)][Math.round(pos.y)].isWall());
		else 
			afford = false;
		return afford;
	}
	/**
	 * @param pos The position to test in Cartesian coordinates
	 * @return true if this position is dirty but not food. 
	 */
	protected boolean affordTouchSoft(v3 pos)
	{
		boolean afford;
		if (inScene(Math.round(pos.x), Math.round(pos.y), 0))
			afford = (m_blocks[Math.round(pos.x)][Math.round(pos.y)].isAlga());
		else 
			afford = false;
		return afford;
	}
	
	/**
	 * Eat an alga food
	 * @param pos The position to eat the food in Cartesian coordinates
	 */
	public void eatFood( v3 pos){
		if( this.affordEat( pos ) ){
			this.m_blocks[Math.round(pos.x)][Math.round(pos.y)] = Environment.empty;
		}
	}
	
	/**
	 * @param pos The position to test in Cartesian coordinates.
	 * @return true if this position is food. 
	 */
	protected boolean affordEat(v3 pos)
	{
		boolean afford;
		if (inScene(Math.round(pos.x), Math.round(pos.y), 0))
			afford = (m_blocks[Math.round(pos.x)][Math.round(pos.y)].isFood());
		else 
			afford = false;
		return afford;
	}
	/**
	 * @param pos The position to test in cartesian coordinates.
	 * @return true if this position is dirty or wall. 
	 */
	public boolean affordSee(v3 pos)
	{
		boolean afford;
		if (inScene(Math.round(pos.x), Math.round(pos.y), 0))
			afford = m_blocks[Math.round(pos.x)][Math.round(pos.y)].isVisible();
		else 
			afford = true;
		return afford;
	}
	
	/**
 		 * Get the entity at a given location in the scene or null.
 		 * (only entities different from the given name)
 		 * @param postion The position in the scene.
 		 * @param name The given agent's name (so the agent won't detect itself)
 		 * @return The entity.
 		 */
	public ErnestModel getEntity(v3 position, String name)
	{
		if (!inScene(Math.round(position.x), Math.round(position.y), Math.round(position.z)))
			return null;
		else
		{
			for (ErnestModel entity : m_modelList)
			{
				if (entity.overlap(position) && !entity.getName().equals(name))
					return entity;
			}
		}
		return null;
	}
	
	/**
	 * @param i X coordinate
	 * @param j Y coordinate
	 * @param k Z coordinate
	 * @return true if the given location is within the scene's boundaries.
	 */

	public boolean inScene(int i, int j, int k) {
		return (i >= 0 && i < m_w && j >= 0 && j < m_h && k == 0 );
	}

	public void setBlock(int x, int y, Block block)
	{ 
		m_blocks[x][y] = block;
	}

	public boolean isInformation(int x, int y)
	{ 
		return (m_informationX == x && m_informationY == y);
	}

	public int getCounter()
	{
		int counter=0;
		if (m_modelList.size()>indexDisplay) counter=m_modelList.get(indexDisplay).getCounter();
		
		return counter;
	}

	/**
	 * Returns the animation value
	 * (For yellow flashing the perceived square)
	 * @author ogeorgeon 
	 */
	public int getAnim(float x, float y)
	{ 
		return m_anim[Math.round(x)][Math.round(y)];
	}

	public void setAnim(float x, float y, int anim)
	{ 
		if (anim == ANIM_BUMP)
			speak("Ouch", false , false);

		m_anim[Math.round(x)][Math.round(y)] = anim;
	}

	/**
	 * getStatus
	 * Returns the status value of a schema enaction for an Ernest model
	 * @author ogeorgeon 
	 */
	public boolean getStatus()
	{
		return m_status;
	}

	public int getDirtyCount()
	{ return m_dirtyCount; }

	public int getDelay()
	{ return m_delay; }

	public float agentX(int i){
		if (i<m_modelList.size()) return m_modelList.get(i).mPosition.x;
		else                      return -1;
	}

	public float agentY(int i){
		if (i<m_modelList.size()) return m_modelList.get(i).mPosition.y;
		else                      return -1;
	}
	
	public int agentId(float x, float y){
		int ident=-1;
		float min=2;
		float d=0;
		
		for (int i=0;i<m_modelList.size();i++){
			d= (m_modelList.get(i).mPosition.x - x)*(m_modelList.get(i).mPosition.x - x)
			  +(m_modelList.get(i).mPosition.y - (m_h-y))*(m_modelList.get(i).mPosition.y - (m_h-y));
			d=(float) Math.sqrt(d);
			
			if (d<=1 && d<min){
				min=d;
				ident=m_modelList.get(i).ident;
			}
		}
		
		
		return ident;
	}

	public int getWidth()
	{ return m_w; }

	public int getHeight()
	{ return m_h; }

	public boolean getSpeakAloud()
	{
		return m_bSpeakAloud;
	}
	public boolean getInternalState()
	{
		return m_bInternalState;
	}

	public void setSpeakAloud(boolean b)
	{
		m_bSpeakAloud = b;
	}
	public void setInternalState(boolean b)
	{
		m_bInternalState = b;
	}

	public void setDelay(int delay)
	{ 
		m_delay = delay; 
		setChanged();
		notifyObservers2();
	}
	
	
	/**
	 * Initialize the preferences from Registry
	 * @author ogeorgeon 
	 */
	public void initPreferences()
	{
		Preferences prefs = Preferences.userRoot().node("vacuum");
		
		m_w = prefs.getInt(PREF_W,INIT_W);
		m_h = prefs.getInt(PREF_H,INIT_H);
		//m_x = prefs.getInt(PREF_X,INIT_X);
		//m_y = prefs.getInt(PREF_Y,INIT_Y);
		m_dirtyCount = prefs.getInt(PREF_DIRTY,INIT_DIRTY);
		m_delay = prefs.getInt(PREF_DELAY,INIT_DELAY);
		m_boardFileName = prefs.get(PREF_BOARDFILE, DEFAULT_BOARD);
		m_bSpeakAloud = prefs.getBoolean(PREF_SPEAKALOUD, true);
		
	}

	/**
	 * Save the preferences to Registry
	 * @author ogeorgeon 
	 */
	public void putPreferences()
	{
		Preferences prefs = Preferences.userRoot().node("vacuum");
		
		prefs.putInt(PREF_W, m_w);
		prefs.putInt(PREF_H, m_h);
		//prefs.putFloat(PREF_X, m_x);
		//prefs.putFloat(PREF_Y, m_y);
		prefs.putInt(PREF_DIRTY, m_dirtyCount);
		prefs.putInt(PREF_DELAY, m_delay);
		prefs.put(PREF_BOARDFILE, m_boardFileName);
		prefs.putBoolean(PREF_SPEAKALOUD, m_bSpeakAloud);
	}

	protected void notifyObservers2()
	{
		try
		{
			if ( (m_mainThread != Thread.currentThread()) &&
	 			 (m_eventThread != Thread.currentThread())	)
			{
				SwingUtilities.invokeAndWait(m_thread);
			}
			else
			{
				m_thread.run();
			}
		}
		catch  (InvocationTargetException e)
		{
		  	throw new IllegalStateException("Error notifying observers!");
		}
		catch  (InterruptedException e)
		{
			throw new IllegalStateException("Error notifying view! (Deadlock?)");
		}
	}

	private static class NotifyThread implements Runnable
	{
		@Override
		public void run()
		{ 
			//notifyObservers(); 
		}
	}

	public void setBoardFileName(String file) {
		m_boardFileName = file;
		putPreferences();
	}

	public String getBoardFileName() {
		return m_boardFileName;
	}

	/**************************************
	 * Speak aloud a text
	 * @param wait wait befor proceeding
	 * @param force Force the speak aloud
	 **************************************/
	public void speak(String text, boolean wait,boolean force)
	{

	}
	
	public void traceUserEvent(String type, int x, int y)
	{
//		Object element = m_tracer.newEvent("user", type, m_counter);
//		m_tracer.addSubelement(element, "x", x + "");
//		m_tracer.addSubelement(element, "y", y + "");
	}
		
	/**
	 * @param x The x coordinate of the square
	 * @param y The y coordinate of the square
	 * @return The background color of a square
	 *//*
	public Color getBackgroundColor(float x, float y)
	{
		Color backgroundColor = FIELD_COLOR;
		
		if (getDirty(x, y) == DIRTY)
			backgroundColor = WATER_COLOR ;			
		if (getDirty(x, y) == FOOD)
			backgroundColor = FOOD_COLOR;			
		if (getDirty(x, y) > FOOD)
		{
			float hue = getDirty(x, y) / 20.0f;
			backgroundColor = Color.getHSBColor(hue, 1f, 0.9f);
		}
		else if (getWall(x, y) == WALL || getWall(x, y) == WALL_INFORMATION || getWall(x, y) == WALL_INFORMATION2)
			backgroundColor = WALL_COLOR;
		else if (getWall(x, y) > WALL)
		{
			float hue = getWall(x, y) / 20.0f;
			backgroundColor = Color.getHSBColor(hue, 1.0f, 0.9f);
		}
		
		if (getAnim(x,y) == ANIM_BUMP)
			backgroundColor = Color.RED;
		//else if (getAnim(x,y) == ANIM_RUB)
		// 	backgroundColor = Color.PINK;
		else if (getAnim(x,y) == ANIM_TOUCH)
			backgroundColor = Color.YELLOW;
		
		//setAnim(m_x, m_y, ANIM_NO);

		return backgroundColor;
	}*/
	
	/**
	 * Paint a square
	 */
	public void paint(int x, int y, Graphics g) 
	{
	}
	
	/**
	 * Initialize the agent's picture
	 * To support the agent's rotation, picture file names must end with _up _right _down _left.
	 */
	private ImageIcon m_icon_up; 
	private ImageIcon m_icon_right; 
	private ImageIcon m_icon_down; 
	private ImageIcon m_icon_left; 
	
	public void setPicture(String pictureFileName)
	{
		m_icon_up    = new ImageIcon(pictureFileName);
		if ( pictureFileName.indexOf("_up.") > 0 )
		{
			m_icon_right = new ImageIcon(pictureFileName.replaceFirst("_up.", "_right."));
			m_icon_down  = new ImageIcon(pictureFileName.replaceFirst("_up.", "_down."));
			m_icon_left  = new ImageIcon(pictureFileName.replaceFirst("_up.", "_left."));
		}
		else
		{
			m_icon_right = new ImageIcon(pictureFileName);			
			m_icon_down  = new ImageIcon(pictureFileName);			
			m_icon_left  = new ImageIcon(pictureFileName);			
		}
	}
	
	/**
	 * Paint the agent as an icon.
	 * @param g The graphic object for painting.
	 */
	public void paintAgent(Graphics2D g,int x,int y,double sx,double sy)
	{
		Image img = m_icon_up.getImage();
		img = m_icon_right.getImage();
		/*if (m_orientation == ORIENTATION_RIGHT)
			img = m_icon_right.getImage();
		if (m_orientation == ORIENTATION_DOWN)
			img = m_icon_down.getImage();
		if (m_orientation == ORIENTATION_LEFT)
			img = m_icon_left.getImage();
		 */
		g.drawImage(img, 1, 1, null); // TODO check the position and size
	}

	protected void sleep(int t)
	{
		try
		{ 
			Thread.currentThread().sleep(t);
		}
		catch(InterruptedException ie)
		{}
	}

	public void paintDream(Graphics2D g,int x,int y,double sx,double sy)
	{
		
	}
	
	public void removeEntity(v3 position, String name)
	{
		if (inScene(Math.round(position.x), Math.round(position.y), Math.round(position.z)))
		{
			for (int i=0;i<m_modelList.size();i++)
			{
				if (m_modelList.get(i).overlap(position) && !m_modelList.get(i).getName().equals(name) && m_modelList.get(i).affordEat())
				{
					m_modelList.remove(i);
				}
			}
		}
		indexDisplay=indexOf(identDisplay);
	}
	
	public int indexOf(int ident){
		int index=-1;
		for (int i=0;i<m_modelList.size();i++){
			if (m_modelList.get(i).ident==ident) index=i;
		}
		return index;
	}

}


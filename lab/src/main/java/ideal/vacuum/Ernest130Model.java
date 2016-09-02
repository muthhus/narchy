package ideal.vacuum;

import ideal.vacuum.agent.*;
import ideal.vacuum.agent.behavior.*;
import ideal.vacuum.agent.motivation.Motivation;
import ideal.vacuum.agent.motivation.MotivationErnest8;
import ideal.vacuum.agent.spacememory.SpaceMemoryDesigner;
import ideal.vacuum.agent.vision.Eye;
import ideal.vacuum.eca.spas.Placeable;
import ideal.vacuum.ernest.BehaviorErnest7;
import ideal.vacuum.ernest.Ernest;
import ideal.vacuum.ernest.ErnestUtils;
import ideal.vacuum.view.MainFrame;
import ideal.vacuum.view.SpaceMemory;
import ideal.vacuum.view.SpaceMemoryFrame;
import spacegraph.math.v3;
import spacegraph.phys.math.Transform;

import java.awt.*;
import java.util.ArrayList;

/**
 * 
 * @author Joseph GARNIER
 * @version $Revision: 393 $
 */
public class Ernest130Model extends ErnestModel implements DesignerListener {

	private enum Version {
		ERNEST7() ,
		ERNEST8() ,
		ERNEST9()
	}

	private final static Version CURRENT_VERSION = Version.ERNEST9 ;
	private final static String SPACE_MEMORY_FRAME_CLASS_NAME = "memory110.SpaceMemoryFrame" ;
	private final static String EYE_VIEW_FRAME_CLASS_NAME = "agent.EyeView" ;
	private final static String INNER_EAR_FRAME_CLASS_NAME = "InnerEar" ;

	private AgentDesigner agentDesigner ;
	private SpaceMemoryDesigner spaceMemoryDesigner ;
	private Behavior behavior ;
	private BehaviorState behaviorState ;
	private Motivation motivation ;
	private SpaceMemory spaceMemory ;
	private Eye eye ;

	private Move schema; // OG

	public Ernest130Model( int agentNumericalID ) {
		super( agentNumericalID ) ;
	}

	public GraphicProperties getCopyOfGraphicProperties() {
		return new GraphicProperties(
				(v3) this.mPosition.clone() ,
				(v3) this.mOrientation.clone() ,
				(v3) this.mTranslation.clone() ,
				(v3) this.mRotation.clone() ,
				(v3) this.mPreviousPosition.clone() ,
				(v3) this.mPreviousOrientation.clone(),
				this.getDisplayCode()) ;
	}

	public MainFrame getMainFrame() {
		return this.mainFrame ;
	}

	public Environment getEnvironment() {
		return this.m_env ;
	}

	@Override
	public void sleep(int millis ) {
		super.sleep( millis ) ;
	}

	@Override
	public boolean affordCuddle( v3 pos ) {
		return super.affordCuddle( pos ) ;
	}

	@Override
	public void init(int gridWeight , int gridHeight ) {
		// Initialize the model
		super.init( gridWeight , gridHeight ) ;

		this.setChanged() ;
		this.notifyObservers2() ;
		this.eye = new Eye( this.m_env , this , this.mPosition , this.mName ) ;
		Color agentColor ;
		switch ( this.ident ) {
			case 0:
				agentColor = new Color( 0xFF8000 ) ;
				break ;
			case 1:
				agentColor = Color.BLUE ;
				break ;
			default:
				agentColor = new Color( 0xFF8000 ) ;
				break ;
		}

		switch ( Ernest130Model.CURRENT_VERSION ) {
			case ERNEST7:
				this.behavior = new BehaviorErnest7( this , this , this.eye ) ;
				this.motivation = new BehaviorErnest7.MotivationErnest7() ;
				this.agentDesigner = new AgentDesigner( this , agentColor , false , false ) ;
				break ;
			case ERNEST8:
				this.behavior = new BehaviorErnest8( this , this , this.eye ) ;
				this.motivation = new BehaviorErnest7.MotivationErnest7() ;
				this.agentDesigner = new AgentDesigner( this , agentColor , false , false ) ;
				break ;
			case ERNEST9:
				this.behavior = new BehaviorErnest9( this , this , this.eye ) ;
				this.motivation = new MotivationErnest8() ;
				this.agentDesigner = new AgentDesigner( this , agentColor , true , false ) ;
				break ;
			default:
				break ;
		}

		this.spaceMemoryDesigner = new SpaceMemoryDesigner( this , agentColor ) ;
		this.behaviorState = this.behavior.getCurrentBehaviorState() ;
		this.spaceMemory = new SpaceMemory() ;
	}

	@Override
	public String getVersion() {
		return "Ernest 13.0" ;
	}

	@Override
	public void initErnest() {
		// Instantiate Ernest
		this.m_ernest = new Ernest() ;

		// Initialize the visualization.
		this.spaceMemory.setModel( this ) ;

		// Only trace the first agent.
	    //this.m_tracer = new
		//XMLStreamTracer("http://macbook-pro-de-olivier-2.local/alite/php/stream/","UzGveECMaporPwkslFdyDfNIQLwMYk");
		// XMLStreamTracer("http://macbook-pro-de-olivier-2.local/alite/php/stream/","dlsQKeaXlclGbzRTN--ZLWajTDyGpr");
		// this.m_tracer = new XMLStreamTracer(
		// "http://134.214.128.53/abstract/lite/php/stream/" ,
		// "juIQzDzdCtBSpmNnJNkzdtTTajfsXe" ) ;
		//this.m_tracer = null ;
		// Initialize the Ernest
		// Ernest's inborn primitive interactions
		this.m_ernest.setParameters( 6 , 10 ) ;
		//this.m_ernest.setTracer( this.m_tracer ) ;
		this.motivation.putMotivation( this.m_ernest ) ;
		this.cognitiveMode = ErnestModel.AGENT_RUN ;

		System.out.println( "Ernest initialized" ) ;
	}

	@Override
	public void setDisplay() {
		try {
			if ( this.m_env.isPlugued( SpaceMemoryFrame.class ) ) {
				this.m_env.getPlugin( SpaceMemoryFrame.class ).close() ;
				this.m_env.unplugFrame( SpaceMemoryFrame.class ) ;
			}
			this.m_env.plugFrame( SpaceMemoryFrame.class ) ;
			this.m_env.getPlugin( SpaceMemoryFrame.class ).setMemory( this.spaceMemory ) ;
			this.m_env.getPlugin( SpaceMemoryFrame.class ).display() ;
		} catch ( Exception e ) {
			e.printStackTrace() ;
		}
	}

	@Override
	public void update() {
		if ( this.schema != null ) {
			this.behaviorState = this.behavior.doMovement( this.schema ) ;
			this.traceEnvironmentalData() ;
//			if ( this.m_tracer != null )
//				this.m_tracer.finishEvent() ;
		}

		this.schema = Move.getByLabel( this.m_ernest.step(
				this.behavior.getPlaces() ,
				this.behavior.getTransform() ).getLabel() ) ;
		if ( this.cognitiveMode == ErnestModel.AGENT_STEP )
			this.cognitiveMode = ErnestModel.AGENT_STOP ;

		this.refreshFramesPlugins( 0 , 0 ) ;

	}

	private void traceEnvironmentalData() {
//		if ( this.m_tracer != null ) {
//			Object e = this.m_tracer.addEventElement( "environment" ) ;
//			this.m_tracer.addSubelement( e , "x" , ErnestUtils.format( this.mPosition.x , 0 ) ) ;
//			this.m_tracer.addSubelement( e , "y" , ErnestUtils.format( this.mPosition.y , 0 ) ) ;
//			this.m_tracer.addSubelement(
//					e ,
//					"orientation" ,
//					ErnestUtils.format( this.mOrientation.z , 2 ) ) ;
//		}
	}

	private void refreshFramesPlugins( final float angleRotation , final float xTranslation ) {
		Transform transformation = this.m_ernest.getTransformToAnim() ;
		this.m_env.animFramesPlugins(
				-ErnestUtils.angle( transformation ) ,
				-ErnestUtils.translationX( transformation ) ) ;
	}

	@Override
	public void paintAgent(Graphics2D g2d , int x , int y , double sx , double sy ) {
		this.agentDesigner.paintAgent( g2d , x , y , sx , sy , this.behaviorState ) ;
	}

	@Override
	public void paintSpaceMemory(Graphics g , ArrayList<Placeable> placeList , float angleRotation ,
								 float xTranslation ) {
		this.spaceMemoryDesigner.paintSpaceMemory(
				(Graphics2D) g ,
				placeList ,
				this.behaviorState ,
				angleRotation ,
				xTranslation ) ;
	}

	@Override
	public void notifyGraphicPropertiesChanged( GraphicPropertiesChangeEvent properties ) {
		this.mPosition = properties.getmPosition() ;
		this.mOrientation = properties.getmOrientation() ;
		this.mTranslation = properties.getmTranslation() ;
		this.mRotation = properties.getmRotation() ;
		this.mPreviousPosition = properties.getmPreviousPosition() ;
		this.mPreviousOrientation = properties.getmPreviousOrientation() ;
	}

	@Override
	public void notifyBehaviorStateChanged( BehaviorStateChangeEvent behaviorStateEvent ) {
		this.behaviorState = behaviorStateEvent.getBehaviorState() ;
	}
}

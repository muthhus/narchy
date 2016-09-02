package ideal.vacuum.agent.spacememory;

import ideal.vacuum.Ernest130Model;
import ideal.vacuum.agent.behavior.BehaviorState;
import ideal.vacuum.eca.ActInstance;
import ideal.vacuum.eca.construct.egomem.PhenomenonInstance;
import ideal.vacuum.eca.spas.Placeable;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;


/**
 * 
 * @author Joseph GARNIER
 * @version $Revision: 391 $
 */
public class SpaceMemoryDesigner {

	protected final static int WIDTH = 300 ;
	protected final static int HEIGHT = 250 ;
	protected final static int WIDTH_REAL = WIDTH * 2 ;
	protected final static int HEIGHT_REAL = HEIGHT * 2 ;
	protected final static int SCALE = 30 ;

	private final Ernest130Model model ;
	private final Color agentColor ;

	public SpaceMemoryDesigner( Ernest130Model model , Color agentColor ) {
		this.model = model ;
		this.agentColor = agentColor;
	}

	public void paintSpaceMemory( Graphics2D g2d , ArrayList<Placeable> placeList ,
			BehaviorState behaviorState, float angleRotation , float xTranslation ) {
		g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING , RenderingHints.VALUE_ANTIALIAS_ON ) ;

		AffineTransform originLocation = g2d.getTransform() ;

		this.displayBackground( g2d ) ;
		this.displayCounter( g2d ) ;
		this.moveOriginToCenter( g2d ) ;
		
		AffineTransform centerLocation = g2d.getTransform() ;
		
		this.moveOrginForInteractions( g2d , angleRotation , xTranslation ) ;
		this.displayInteractions( g2d , placeList , behaviorState ) ;
		
		g2d.setTransform( centerLocation ) ;
		
		new AgentArrowDesigner().addAgent( g2d , this.agentColor ) ;
		
		g2d.setTransform( originLocation ) ;

		//System.out.println( "----------------------------------" ) ;
	}

	private void displayBackground( Graphics2D g2d ) {
		g2d.setColor( Color.WHITE ) ;
		g2d.fillRect( 0 , 0 , WIDTH_REAL , HEIGHT_REAL ) ;
	}

	private void displayCounter( Graphics2D g2d ) {
		String counter = this.model.getCounter() + "" ;
		g2d.setFont( new Font( "Dialog" , Font.BOLD , 18 ) ) ;
		g2d.setColor( Color.GRAY ) ;
		g2d.drawString( counter , WIDTH_REAL - 50 , 30 ) ;
	}

	private void displayAxis( Graphics2D g2d ) {
		g2d.setStroke( new BasicStroke( 4f ) ) ;
		g2d.setColor( Color.RED ) ;//-x;-y
		g2d.fill( new Rectangle2D.Double( 0 , 0 , 1 , 1 ) ) ;
		g2d.setColor( Color.RED ) ;
		g2d.draw( new Rectangle2D.Double( 0 , 0 , 1 , 1 ) ) ;
		g2d.setColor( Color.CYAN ) ;//+x;+y
		g2d.fill( new Rectangle2D.Double( WIDTH_REAL - 2 , HEIGHT_REAL - 2 , 1 , 1 ) ) ;
		g2d.setColor( Color.CYAN ) ;
		g2d.draw( new Rectangle2D.Double( WIDTH_REAL - 2 , HEIGHT_REAL - 2 , 1 , 1 ) ) ;
		g2d.setColor( Color.GRAY ) ;//+x;0
		g2d.fill( new Rectangle2D.Double( WIDTH_REAL - 2 , 0 , 1 , 1 ) ) ;
		g2d.setColor( Color.GRAY ) ;
		g2d.draw( new Rectangle2D.Double( WIDTH_REAL - 2 , 0 , 1 , 1 ) ) ;
		g2d.setColor( Color.GREEN ) ;//0;+y
		g2d.fill( new Rectangle2D.Double( 0 , HEIGHT_REAL - 2 , 1 , 1 ) ) ;
		g2d.setColor( Color.GREEN ) ;
		g2d.draw( new Rectangle2D.Double( 0 , HEIGHT_REAL - 2 , 1 , 1 ) ) ;
		g2d.setColor( Color.BLUE ) ;//center
		g2d.fill( new Rectangle2D.Double( WIDTH , HEIGHT , 1 , 1 ) ) ;
		g2d.setColor( Color.BLUE ) ;
		g2d.draw( new Rectangle2D.Double( WIDTH , HEIGHT , 1 , 1 ) ) ;
	}

	private void displayAxisTransformed( Graphics2D g2d ) {
		g2d.setStroke( new BasicStroke( 4f ) ) ;
		g2d.setColor( Color.RED ) ;//-x;-y
		g2d.fill( new Rectangle2D.Double( -WIDTH , -HEIGHT , 1 , 1 ) ) ;
		g2d.setColor( Color.RED ) ;
		g2d.draw( new Rectangle2D.Double( -WIDTH , -HEIGHT , 1 , 1 ) ) ;
		g2d.setColor( Color.CYAN ) ;//+x;+y
		g2d.fill( new Rectangle2D.Double( WIDTH - 2 , HEIGHT - 2 , 1 , 1 ) ) ;
		g2d.setColor( Color.CYAN ) ;
		g2d.draw( new Rectangle2D.Double( WIDTH - 2 , HEIGHT - 2 , 1 , 1 ) ) ;
		g2d.setColor( Color.GRAY ) ;//+x;-y
		g2d.fill( new Rectangle2D.Double( WIDTH - 2 , -HEIGHT , 1 , 1 ) ) ;
		g2d.setColor( Color.GRAY ) ;
		g2d.draw( new Rectangle2D.Double( WIDTH - 2 , -HEIGHT , 1 , 1 ) ) ;
		g2d.setColor( Color.GREEN ) ;//-x;+y
		g2d.fill( new Rectangle2D.Double( -WIDTH , HEIGHT - 2 , 1 , 1 ) ) ;
		g2d.setColor( Color.GREEN ) ;
		g2d.draw( new Rectangle2D.Double( -WIDTH , HEIGHT - 2 , 1 , 1 ) ) ;
		g2d.setColor( Color.BLUE ) ;//center
		g2d.fill( new Rectangle2D.Double( -50 , 30 , 1 , 1 ) ) ;
		g2d.setColor( Color.BLUE ) ;
		g2d.draw( new Rectangle2D.Double( -50 , 30 , 1 , 1 ) ) ;
	}
	
	private void moveOriginToCenter( Graphics2D g2d ) {
		AffineTransform centerLocation = new AffineTransform() ;
		centerLocation.translate( WIDTH , HEIGHT ) ;
		g2d.transform( centerLocation ) ;
	}

	private void moveOrginForInteractions( Graphics2D g2d , float angleRotation , float xTranslation ) {
		AffineTransform interactionsLocation = new AffineTransform() ;
		interactionsLocation.rotate( angleRotation ) ;
		interactionsLocation.translate( -xTranslation * SCALE , 0 ) ;
		g2d.transform( interactionsLocation ) ;
	}

	private void displayInteractions( Graphics2D g2d , ArrayList<Placeable> placeList ,
			BehaviorState behaviorState ) {
		for ( Placeable placeable : placeList ) {
			AffineTransform originLocation = g2d.getTransform() ;
			this.displayPlaceable( g2d , placeable , behaviorState ) ;
			g2d.setTransform( originLocation ) ;
		}
	}

	private void displayPlaceable( Graphics2D g2d , Placeable placeable , BehaviorState behaviorState ) {
		if (placeable instanceof ActInstance){
			AbstractSMInteractionDesigner interactionDesigner = new VisualInteractionDesigner();
			interactionDesigner.addInteraction( g2d , placeable , behaviorState );
		}
		if (placeable instanceof PhenomenonInstance){
			AbstractSMInteractionDesigner interactionDesigner = new PhenomenonInstanceDesigner();
			interactionDesigner.addInteraction( g2d , placeable , behaviorState );
		}
	}
}
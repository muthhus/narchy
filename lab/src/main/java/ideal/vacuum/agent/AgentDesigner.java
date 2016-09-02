package ideal.vacuum.agent;

import ideal.vacuum.Environment;
import ideal.vacuum.Ernest130Model;
import ideal.vacuum.agent.behavior.BehaviorState;

import java.awt.*;
import java.awt.geom.*;

/**
 * 
 * @author Joseph GARNIER
 * @version $Revision: 390 $
 */
public class AgentDesigner {

	public static Color UNANIMATED_COLOR = Color.GRAY ;

	private final Ernest130Model model ;
	private final Color agentColor ;
	private final boolean useRetina ;
	private final boolean useSharkBody ;

	public AgentDesigner( Ernest130Model model ,
			Color agentColor ,
			boolean useRetina ,
			boolean useSharkBody ) {
		this.model = model ;
		this.agentColor = agentColor ;
		this.useRetina = useRetina ;
		this.useSharkBody = useSharkBody ;
	}

	public void paintAgent( Graphics2D g2d , int x , int y , double sx , double sy ,
			BehaviorState behaviorState ) {
		GraphicProperties ernestGraphicProperties = this.model.getCopyOfGraphicProperties() ;
		behaviorState.setFocusColor(model.getDisplayCode());
		
		AffineTransform initialPosition = g2d.getTransform() ;

		this.fixAgentPosition( g2d , x , y , sx , sy , ernestGraphicProperties ) ;

		if ( this.useSharkBody ) {
			this.drawAgentSharkBody( g2d ) ;
		} else {
			this.drawAgentArrowBody( g2d ) ;
		}

		if ( this.useRetina ) {
			this.drawAgentRetina( g2d , behaviorState ) ;
		} else {
			this.drawAgentFocus( g2d , behaviorState ) ;
		}

		g2d.setTransform( initialPosition ) ;
	}

	private static void displayAxis( Graphics2D g2d ) {
		g2d.setStroke( new BasicStroke( 4f ) ) ;
		g2d.setColor( Color.CYAN ) ;
		g2d.fill( new Rectangle2D.Double( -47 , -47 , 1 , 1 ) ) ;
		g2d.setColor( Color.CYAN ) ;
		g2d.draw( new Rectangle2D.Double( -47 , -47 , 1 , 1 ) ) ;
		g2d.setColor( Color.GRAY ) ;
		g2d.fill( new Rectangle2D.Double( -47 , 47 , 1 , 1 ) ) ;
		g2d.setColor( Color.GRAY ) ;
		g2d.draw( new Rectangle2D.Double( -47 , 47 , 1 , 1 ) ) ;
		g2d.setColor( Color.GREEN ) ;
		g2d.fill( new Rectangle2D.Double( 47 , -47 , 1 , 1 ) ) ;
		g2d.setColor( Color.GREEN ) ;
		g2d.draw( new Rectangle2D.Double( 47 , -47 , 1 , 1 ) ) ;
		g2d.setColor( Color.RED ) ;
		g2d.fill( new Rectangle2D.Double( 47 , 47 , 1 , 1 ) ) ;
		g2d.setColor( Color.RED ) ;
		g2d.draw( new Rectangle2D.Double( 47 , 47 , 1 , 1 ) ) ;
	}

	private void fixAgentPosition( Graphics2D g2d , int x , int y , double sx , double sy ,
			GraphicProperties ernestGraphicProperties ) {
		AffineTransform orientation = new AffineTransform() ;
		orientation.translate( x , y ) ;
		orientation.rotate( -ernestGraphicProperties.getmOrientation().z + Math.PI / 2 ) ;
		orientation.scale( sx , sy ) ;
		orientation.scale( 1 , -1 ) ;
		orientation.scale( 0.8 , 0.8 ) ;
		g2d.transform( orientation ) ;
	}

	private void drawAgentArrowBody( Graphics2D g2d ) {
		Area agent = AgentDesigner.arrowBodyShape() ;
		g2d.setColor( this.agentColor ) ;
		g2d.fill( agent ) ;
		g2d.setStroke( new BasicStroke( 4f ) ) ;
		g2d.setColor( Color.black ) ;
		g2d.draw( agent ) ;
	}

	private void drawAgentSharkBody( Graphics2D g2d ) {
		Area agent = AgentDesigner.sharkBodyShape() ;
		g2d.setColor( this.agentColor ) ;
		g2d.fill( agent ) ;
		g2d.setStroke( new BasicStroke( 4f ) ) ;
		g2d.setColor( Color.black ) ;
		g2d.draw( agent ) ;
	}

	private static Area arrowBodyShape() {
		GeneralPath body = new GeneralPath() ;
		body.append( new Line2D.Double( -40 , -50 , 0 , -30 ) , false ) ;
		body.append( new Line2D.Double( 0 , -30 , 40 , -50 ) , true ) ;
		body.append( new Line2D.Double( 40 , -50 , 0 , 50 ) , true ) ;

		return new Area( body ) ;
	}

	private static Area sharkBodyShape() {
		GeneralPath body = new GeneralPath() ;
		body.append( new CubicCurve2D.Double( 0 , -40 , -30 , -40 , -5 , 45 , 0 , 45 ) , false ) ;
		body.append( new CubicCurve2D.Double( 0 , 45 , 5 , 45 , 30 , -40 , 0 , -40 ) , false ) ;

		GeneralPath leftPectoralFin = new GeneralPath() ;
		leftPectoralFin.append(
				new CubicCurve2D.Double( -15 , -15 , -30 , -10 , -40 , 0 , -40 , 20 ) ,
				false ) ;
		leftPectoralFin.append(
				new CubicCurve2D.Double( -40 , 20 , -30 , 10 , -20 , 8 , -10 , 10 ) ,
				true ) ;

		GeneralPath leftPelvicFin = new GeneralPath() ;
		leftPelvicFin.append(
				new CubicCurve2D.Double( -10 , 15 , -15 , 18 , -20 , 25 , -15 , 30 ) ,
				false ) ;
		leftPelvicFin.append(
				new CubicCurve2D.Double( -15 , 30 , -10 , 25 , -10 , 25 , -5 , 28 ) ,
				true ) ;

		GeneralPath rightPectoralFin = new GeneralPath() ;
		rightPectoralFin.append(
				new CubicCurve2D.Double( 15 , -15 , 30 , -10 , 40 , 0 , 40 , 20 ) ,
				false ) ;
		rightPectoralFin.append(
				new CubicCurve2D.Double( 40 , 20 , 30 , 10 , 20 , 8 , 10 , 10 ) ,
				true ) ;

		GeneralPath rightPelvicFin = new GeneralPath() ;
		rightPelvicFin.append(
				new CubicCurve2D.Double( 10 , 15 , 15 , 18 , 20 , 25 , 15 , 30 ) ,
				false ) ;
		rightPelvicFin.append(
				new CubicCurve2D.Double( 15 , 30 , 10 , 25 , 10 , 25 , 5 , 28 ) ,
				true ) ;

		GeneralPath caudalFin = new GeneralPath() ;
		caudalFin.append(
				new CubicCurve2D.Double( 10 , 50 , 15 , 20 , -15 , 20 , -10 , 50 ) ,
				false ) ;
		caudalFin.append(
				new CubicCurve2D.Double( -10 , 50 , -15 , 30 , 15 , 30 , 10 , 50 ) ,
				false ) ;

		Area shark = new Area( body ) ;
		shark.add( new Area( leftPectoralFin ) ) ;
		shark.add( new Area( rightPectoralFin ) ) ;
		shark.add( new Area( leftPelvicFin ) ) ;
		shark.add( new Area( rightPelvicFin ) ) ;

		return shark ;
	}

	private void drawAgentRetina( Graphics2D g2d , BehaviorState behaviorState ) {
		GeneralPath rightEye = new GeneralPath() ;
		rightEye.append( new Arc2D.Double( -30 , -40 , 60 , 70 , 180 , 180 , Arc2D.PIE ) , false ) ;

		g2d.setStroke( new BasicStroke( 2f ) ) ;
		if (behaviorState.getCellsArray().isEmpty())
			g2d.setColor(Environment.WALL1);
		else
			g2d.setColor( behaviorState.getCellsArray().get( 0 ).getBlockColor() ) ;
		g2d.setColor(behaviorState.getFocusColor());
		g2d.fill( rightEye ) ;
		g2d.setColor( Color.BLACK ) ;
		g2d.draw( rightEye ) ;
	}

	private void drawAgentFocus( Graphics2D g2d , BehaviorState behaviorState ) {
		Rectangle2D.Double focus = new Rectangle2D.Double( -12 , 20 , 25 , 30 ) ;
		Rectangle2D.Double left = new Rectangle2D.Double( -35 , -10 , 25 , 30 ) ;
		Rectangle2D.Double right = new Rectangle2D.Double( 10 , -10 , 25 , 30 ) ;
		g2d.setStroke( new BasicStroke( 2f ) ) ;

		if ( behaviorState.getLeftColor() != AgentDesigner.UNANIMATED_COLOR ) {
			g2d.setColor( behaviorState.getLeftColor() ) ;
			g2d.fill( left ) ;
			g2d.setColor( Color.BLACK ) ;
			g2d.draw( left ) ;
		}
		if ( behaviorState.getRightColor() != AgentDesigner.UNANIMATED_COLOR ) {
			g2d.setColor( behaviorState.getRightColor() ) ;
			g2d.fill( right ) ;
			g2d.setColor( Color.BLACK ) ;
			g2d.draw( right ) ;
		}
		if ( behaviorState.getFocusColor() != AgentDesigner.UNANIMATED_COLOR ) {
			g2d.setColor( behaviorState.getFocusColor() ) ;
			g2d.fill( focus ) ;
			g2d.setColor( Color.BLACK ) ;
			g2d.draw( focus ) ;
		}
	}

}

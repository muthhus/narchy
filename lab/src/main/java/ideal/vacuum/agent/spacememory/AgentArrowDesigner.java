package ideal.vacuum.agent.spacememory;

import java.awt.*;
import java.awt.geom.*;


/**
 * 
 * @author Joseph GARNIER
 * @version $Revision: 366 $
 */
public class AgentArrowDesigner extends AbstractSMAgentDesigner {
	
	private static final double SIMU_SCALE = 3;

	private Graphics2D g2d;
	
	@Override
	public void addAgent( Graphics2D g2d , Color agentColor) {
		this.g2d = g2d;
		this.applyGeometricalTransformation() ;

		g2d.setColor( agentColor ) ;
		g2d.fill( AgentArrowDesigner.arrowBodyShape() ) ;
		g2d.setStroke( new BasicStroke( SpaceMemoryDesigner.SCALE / 10f ) ) ;
		g2d.setColor( Color.BLACK ) ;
		g2d.draw( AgentArrowDesigner.arrowBodyShape() ) ;
		g2d.setColor( Color.GRAY );
		Shape arc = new Arc2D.Double(-2.5 * SpaceMemoryDesigner.SCALE * SIMU_SCALE, -2.5 * SpaceMemoryDesigner.SCALE * SIMU_SCALE, 5 * SpaceMemoryDesigner.SCALE * SIMU_SCALE, 5 * SpaceMemoryDesigner.SCALE * SIMU_SCALE, -90, 180,
				Arc2D.OPEN);
		g2d.draw( arc );
		//g2d.draw( AgentArrowDesigner.fieldOfVision() );
	}

	private void applyGeometricalTransformation() {
		AffineTransform agentLocation = new AffineTransform() ;
		agentLocation.scale( SpaceMemoryDesigner.SCALE / 100f , SpaceMemoryDesigner.SCALE / 100f ) ;
		this.g2d.transform( agentLocation ) ;
	}

	private static Area arrowBodyShape() {
		GeneralPath body = new GeneralPath() ;
		body.append( new Line2D.Double( -50 , -50 , -30 , 0 ) , false ) ;
		body.append( new Line2D.Double( -30 , 0 , -50 , 50 ) , true ) ;
		body.append( new Line2D.Double( -50 , 50 , 50 , 0 ) , true ) ;
		return new Area( body ) ;
	}
	
	private static Area fieldOfVision() {
		GeneralPath fieldOfVision = new GeneralPath() ;
		Shape circle = new Ellipse2D.Double( -2.5 * SpaceMemoryDesigner.SCALE * SIMU_SCALE, -2.5 * SpaceMemoryDesigner.SCALE * SIMU_SCALE, 5 * SpaceMemoryDesigner.SCALE * SIMU_SCALE, 5 * SpaceMemoryDesigner.SCALE * SIMU_SCALE);
		fieldOfVision.append( circle, false );		
		return new Area( fieldOfVision );
	}
}

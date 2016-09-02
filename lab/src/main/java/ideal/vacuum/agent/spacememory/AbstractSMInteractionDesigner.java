package ideal.vacuum.agent.spacememory;

import ideal.vacuum.agent.behavior.BehaviorState;
import ideal.vacuum.eca.spas.Placeable;

import java.awt.*;
import java.awt.geom.Point2D;


/**
 * 
 * @author Joseph GARNIER
 * @version $Revision: 387 $
 */
public abstract class AbstractSMInteractionDesigner {

	protected final Point2D getCartesianOffset( SpaceMemoryMove smMove , float placeOrientationAngle ) {
		Point2D overlapOffset = this.getOverlapOffset( smMove ) ;
		
		double cartesianOffsetX = overlapOffset.getX() *
				Math.cos( placeOrientationAngle ) +
				overlapOffset.getY() *
				Math.sin( placeOrientationAngle ) ;
		double cartesianOffsetY = -overlapOffset.getX() *
				Math.sin( placeOrientationAngle ) +
				overlapOffset.getY() *
				Math.cos( placeOrientationAngle ) ;
		
		return new Point2D.Double( cartesianOffsetX , cartesianOffsetY );
	}

	private final Point2D getOverlapOffset( SpaceMemoryMove smMove ) {
		Point overlapOffset = new Point( 0, 0 );

		switch ( smMove ) {
			case TURN_LEFT:
			case TURN_RIGHT:
				overlapOffset.x = SpaceMemoryDesigner.SCALE / 4 ;
				break ;
			case TOUCH:
				overlapOffset.x = -SpaceMemoryDesigner.SCALE / 3 ;
				break ;
			case TOUCH_LEFT:
				overlapOffset.x = -SpaceMemoryDesigner.SCALE / 4 ;
				overlapOffset.y = -SpaceMemoryDesigner.SCALE / 3 ;
				break ;
			case TOUCH_RIGHT:
				overlapOffset.x = -SpaceMemoryDesigner.SCALE / 4 ;
				overlapOffset.y = SpaceMemoryDesigner.SCALE / 3 ;
				break ;
			default:
				break ;
		}
		
		return overlapOffset;
	}
	
	public abstract void addInteraction( Graphics2D g2d , Placeable actInstance , BehaviorState behaviorState );
}

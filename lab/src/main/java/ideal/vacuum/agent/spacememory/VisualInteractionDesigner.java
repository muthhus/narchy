package ideal.vacuum.agent.spacememory;

import ideal.vacuum.agent.behavior.BehaviorState;
import ideal.vacuum.eca.spas.Placeable;
import ideal.vacuum.eca.spas.SpatialMemoryImpl;
import spacegraph.math.v3;

import java.awt.*;
import java.awt.geom.AffineTransform;


/**
 * 
 * @author Joseph GARNIER
 * @version $Revision: 387 $
 */
public class VisualInteractionDesigner extends AbstractSMInteractionDesigner {

	private Graphics2D g2d ;
	private SpaceMemoryMove smMove ;
	private SpaceMemoryTactileEffect smTactileEffect ;
	private SpaceMemoryVisualEffect smVisualEffect ;

	@Override
	public void addInteraction( Graphics2D g2d , Placeable actInstance , BehaviorState behaviorState ) {
		this.g2d = g2d ;
		double scale = (double)(SpatialMemoryImpl.PERSISTENCE_DURATION - actInstance.getClock() ) / SpatialMemoryImpl.PERSISTENCE_DURATION * 3d;
		this.applyGeometricalTransformation(
				actInstance.getOrientationAngle() , 
				actInstance.getPosition() ,
				scale ) ;

		//String interactionLabel = place.getPrimitive().getLabel() ;
		String interactionLabel = actInstance.getDisplayLabel() ;
		String moveLabel = SpaceMemoryMove.extractMoveLabel( interactionLabel ) ;
		this.smMove = SpaceMemoryMove.getSpaceMemoryMove( moveLabel ) ;
		
		if ( SpaceMemoryTactileEffect.containTactileEffect( interactionLabel ) ) {
			String tactileEffectLabel = SpaceMemoryTactileEffect.extractTactileEffectLabel( interactionLabel ) ;
			this.smTactileEffect = SpaceMemoryTactileEffect.getSpaceMemoryTactileEffect( tactileEffectLabel ) ;
		}

		if ( SpaceMemoryVisualEffect.containVisualEffect( interactionLabel ) ) {
			String visualEffectLabel = SpaceMemoryVisualEffect.extractLeftVisualEffectLabel( interactionLabel ) ;
			this.smVisualEffect = SpaceMemoryVisualEffect.getSpaceMemoryVisualEffect( visualEffectLabel ) ;			
			this.fillAndDrawShapeVisual(actInstance.getClock()) ;
		}else{
			this.fillAndDrawShapeNoVisual();
		}
	}

	private void applyGeometricalTransformation( float orientationAngle , v3 position ,
			double scale ) {
		AffineTransform interactionLocation = new AffineTransform() ;
		interactionLocation.translate(
				(int) ( position.x * SpaceMemoryDesigner.SCALE ) ,
				-(int) ( position.y * SpaceMemoryDesigner.SCALE ) ) ;
		interactionLocation.rotate( -orientationAngle ) ;
		interactionLocation.scale( scale , scale ) ;
		this.g2d.transform( interactionLocation ) ;
	}

	private void fillAndDrawShapeVisual(int clock) {
		this.g2d.setColor( this.smVisualEffect.getEffectColor() ) ;
		this.g2d.fill( this.smMove.getShape() ) ;
		this.g2d.setColor( Color.BLACK ) ;
		//this.g2d.setColor( new Color(clock * 10, clock * 10, clock * 10) ) ;
		this.g2d.setStroke( new BasicStroke( SpaceMemoryDesigner.SCALE / 100f ) ) ;
		this.g2d.draw( this.smMove.getShape() ) ;
	}
	private void fillAndDrawShapeNoVisual() {
		this.g2d.setColor( this.smTactileEffect.getEffectColor() ) ;
		this.g2d.fill( this.smMove.getShape() ) ;
		this.g2d.setColor( Color.BLACK ) ;
		this.g2d.setStroke( new BasicStroke( SpaceMemoryDesigner.SCALE / 100f ) ) ;
		this.g2d.draw( this.smMove.getShape() ) ;
	}
}

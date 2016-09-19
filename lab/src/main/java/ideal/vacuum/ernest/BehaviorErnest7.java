package ideal.vacuum.ernest;

import ideal.vacuum.Environment;
import ideal.vacuum.Ernest130Model;
import ideal.vacuum.agent.DesignerListener;
import ideal.vacuum.agent.Move;
import ideal.vacuum.agent.TactileEffect;
import ideal.vacuum.agent.behavior.AbstractBehavior;
import ideal.vacuum.agent.motivation.Motivation;
import ideal.vacuum.agent.vision.Eye;
import spacegraph.math.v3;

import java.awt.*;
import java.util.Objects;

import static ideal.vacuum.Environment.*;


/**
 * 
 * @author Joseph GARNIER
 * @version $Revision: 373 $
 */
public class BehaviorErnest7 extends AbstractBehavior {

	public BehaviorErnest7(Ernest130Model model , DesignerListener listener , Eye eye ) {
		super( model , listener , eye ) ;
	}

	@Override
	protected void turnRight() {
		this.turnRightAnimWorld() ;
		this.effect.setLabel( TactileEffect.FALSE.getLabel() ) ;
		this.effect.setTransformation( (float) Math.PI / 2 , 0 ) ;
	}

	@Override
	protected void turnLeft() {
		this.turnLeftAnimWorld() ;
		this.effect.setLabel( TactileEffect.FALSE.getLabel() ) ;
		this.effect.setTransformation( (float) -Math.PI / 2 , 0 ) ;
	}

	@Override
	protected void moveForward() {
		v3 localPoint = new v3( this.model.DIRECTION_AHEAD ) ;
		v3 aheadPoint = this.model.localToParentRef( localPoint ) ;
		Color blockColor = this.model.getEnvironment().seeBlock( aheadPoint.x , aheadPoint.y ) ;
		this.effect.setLocation( new v3( 1 , 0 , 0 ) ) ;
		this.effect.setColor( blockColor.getRGB() ) ;

		if ( this.model.getEnvironment().affordWalk( aheadPoint ) && !this.model.affordCuddle( aheadPoint ) ) {
			this.moveForwardAnimWorld() ;
			this.effect.setLabel( TactileEffect.TRUE.getLabel() ) ;
			this.effect.setTransformation( 0 , -1 ) ;
		} else {
			this.effect.setColor( Color.RED.getRGB() ) ;
			this.effect.setLabel( TactileEffect.FALSE.getLabel() ) ;
			this.focusColor = Color.RED ;
			this.bumpAheadAnimWorld() ;
		}
	}

	@Override
	protected void moveBackward() {
	}

	@Override
	protected void touch() {
		v3 localPoint = new v3( this.model.DIRECTION_AHEAD ) ;
		v3 aheadPoint = this.model.localToParentRef( localPoint ) ;
		Color blockColor = this.model.getEnvironment().seeBlock( aheadPoint.x , aheadPoint.y ) ;
		this.focusColor = blockColor ;
		this.effect.setLocation( new v3( 1 , 0 , 0 ) ) ;
		this.effect.setColor( blockColor.getRGB() ) ;

		if ( this.model.affordCuddle( aheadPoint ) ) {
			this.effect.setLabel( TactileEffect.TRUE.getLabel() ) ;
			this.focusColor = WALL1 ;
			this.effect.setColor( WALL1.getRGB() ) ;
		} else if ( this.model.getEnvironment().affordWalk( aheadPoint ) ) {
			this.effect.setLabel( TactileEffect.FALSE.getLabel() ) ;
			this.focusColor = Environment.FIELD_COLOR ;
		} else {
			if (Objects.equals(blockColor, WALL1) ||
					Objects.equals(blockColor, WALL2) ||
					Objects.equals(blockColor, WALL3)) {
				this.focusColor = WALL1 ;
			} else {
				this.focusColor = Environment.FIELD_COLOR ;
			}

			this.effect.setLabel( TactileEffect.TRUE.getLabel() ) ;
		}
		this.touchAnimWorld() ;
	}

	@Override
	protected void touchLeft() {
		v3 localPoint = new v3( this.model.DIRECTION_LEFT ) ;
		v3 leftPoint = this.model.localToParentRef( localPoint ) ;
		Color blockColor = this.model.getEnvironment().seeBlock( leftPoint.x , leftPoint.y ) ;
		this.leftColor = blockColor ;
		this.effect.setLocation( new v3( 0 , 1 , 0 ) ) ;
		this.effect.setColor( blockColor.getRGB() ) ;

		if ( this.model.affordCuddle( leftPoint ) ) {
			this.effect.setLabel( TactileEffect.TRUE.getLabel() ) ;
			this.leftColor = WALL1 ;
			this.effect.setColor( WALL1.getRGB() ) ;
		} else if ( this.model.getEnvironment().affordWalk( leftPoint ) ) {
			this.effect.setLabel( TactileEffect.FALSE.getLabel() ) ;
			this.leftColor = Environment.FIELD_COLOR ;
		} else {
			if (Objects.equals(blockColor, WALL1) ||
					Objects.equals(blockColor, WALL2) ||
					Objects.equals(blockColor, WALL3)) {
				this.leftColor = WALL1 ;
			} else {
				this.leftColor = Environment.FIELD_COLOR ;
			}

			this.effect.setLabel( TactileEffect.TRUE.getLabel() ) ;
		}

		this.touchAnimWorld() ;
	}

	@Override
	protected void touchRight() {
		v3 localPoint = new v3( this.model.DIRECTION_RIGHT ) ;
		v3 rightPoint = this.model.localToParentRef( localPoint ) ;
		Color blockColor = this.model.getEnvironment().seeBlock( rightPoint.x , rightPoint.y ) ;
		this.rightColor = blockColor ;
		this.effect.setLocation( new v3( 0 , -1 , 0 ) ) ;
		this.effect.setColor( blockColor.getRGB() ) ;

		if ( this.model.affordCuddle( rightPoint ) ) {
			this.effect.setLabel( TactileEffect.TRUE.getLabel() ) ;
			this.rightColor = WALL1 ;
			this.effect.setColor( WALL1.getRGB() ) ;
		} else if ( this.model.getEnvironment().affordWalk( rightPoint ) ) {
			this.effect.setLabel( TactileEffect.FALSE.getLabel() ) ;
			this.rightColor = Environment.FIELD_COLOR ;
		} else {
			if (Objects.equals(blockColor, WALL1) ||
					Objects.equals(blockColor, WALL2) ||
					Objects.equals(blockColor, WALL3)) {
				this.rightColor = WALL1 ;
			} else {
				this.rightColor = Environment.FIELD_COLOR ;
			}

			this.effect.setLabel( TactileEffect.TRUE.getLabel() ) ;
		}

		this.touchAnimWorld() ;
	}

	/**
	 *
	 * @author Joseph GARNIER
	 * @version $Revision: 310 $
	 */
	public static class MotivationErnest7 implements Motivation {

		@Override
		public void putMotivation(IErnest ernest ){
			// Touch wall
			ernest.addInteraction( Move.TOUCH.getLabel() + TactileEffect.TRUE.getLabel() , -2 ) ;
			// Touch empty
			ernest.addInteraction( Move.TOUCH.getLabel() + TactileEffect.FALSE.getLabel() , -1 ) ;
			// Touch right wall
			ernest.addInteraction( Move.TOUCH_RIGHT.getLabel() + TactileEffect.TRUE.getLabel() , -2 ) ;
			// Touch right empty
			ernest.addInteraction( Move.TOUCH_RIGHT.getLabel() + TactileEffect.FALSE.getLabel() , -1 ) ;
			// Touch left wall
			ernest.addInteraction( Move.TOUCH_LEFT.getLabel() + TactileEffect.TRUE.getLabel() , -2 ) ;
			// Touch left empty
			ernest.addInteraction( Move.TOUCH_LEFT.getLabel() + TactileEffect.FALSE.getLabel() , -1 ) ;
			// Move
			ernest.addInteraction( Move.MOVE_FORWARD.getLabel() + TactileEffect.TRUE.getLabel() , 5 ) ;
			// Bump
			ernest.addInteraction( Move.MOVE_FORWARD.getLabel() + TactileEffect.FALSE.getLabel() , -10 ) ;
			// Right
			ernest.addInteraction( Move.TURN_RIGHT.getLabel() + TactileEffect.TRUE.getLabel() , -3 ) ;
			ernest.addInteraction( Move.TURN_RIGHT.getLabel() + TactileEffect.FALSE.getLabel() , -3 ) ;
			// Left
			ernest.addInteraction( Move.TURN_LEFT.getLabel() + TactileEffect.TRUE.getLabel() , -3 ) ;
			ernest.addInteraction( Move.TURN_LEFT.getLabel() + TactileEffect.FALSE.getLabel() , -3 ) ;
			// Touch brick
			ernest.addInteraction( Move.TOUCH.getLabel() + TactileEffect.BRICK.getLabel() , -1 ) ;
			// Touch alga
			ernest.addInteraction( Move.TOUCH.getLabel() + TactileEffect.ALGA.getLabel() , -1 ) ;
			// Touch right alga
			ernest.addInteraction( Move.TOUCH_RIGHT.getLabel() + TactileEffect.ALGA.getLabel() , -1 ) ;
			// Touch left alga
			ernest.addInteraction( Move.TOUCH_LEFT.getLabel() + TactileEffect.ALGA.getLabel() , -1 ) ;
			// Move to alga
			ernest.addInteraction( Move.MOVE_FORWARD.getLabel() + TactileEffect.ALGA.getLabel() , 5 ) ;
		}
	}

}

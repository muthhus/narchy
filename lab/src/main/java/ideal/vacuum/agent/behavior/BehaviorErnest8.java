package ideal.vacuum.agent.behavior;

import ideal.vacuum.Environment;
import ideal.vacuum.Ernest130Model;
import ideal.vacuum.ErnestModel;
import ideal.vacuum.agent.DesignerListener;
import ideal.vacuum.agent.TactileEffect;
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
public class BehaviorErnest8 extends AbstractBehavior {

	public BehaviorErnest8( Ernest130Model model , DesignerListener listener , Eye eye ) {
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
			if (Objects.equals(blockColor, ALGA1) ||
					Objects.equals(blockColor, ALGA2) ||
					Objects.equals(blockColor, ALGA1) ||
					Objects.equals(blockColor, ALGA3) ||
					Objects.equals(blockColor, ALGA4) ||
					Objects.equals(blockColor, ALGA5)) {
				this.effect.setLabel( TactileEffect.ALGA.getLabel() ) ;
			} else {
				this.effect.setLabel( TactileEffect.TRUE.getLabel() ) ;
			}
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
			ErnestModel entity = this.model.getEnvironment().getEntity(
					aheadPoint ,
					this.model.getName() ) ;
			this.focusColor = entity.getColor() ;
			this.effect.setColor( Environment.AGENT.getRGB() ) ;
		} else if ( this.model.getEnvironment().affordWalk( aheadPoint ) ) {
			if (Objects.equals(blockColor, ALGA1) ||
					Objects.equals(blockColor, ALGA2) ||
					Objects.equals(blockColor, ALGA1) ||
					Objects.equals(blockColor, ALGA3) ||
					Objects.equals(blockColor, ALGA4) ||
					Objects.equals(blockColor, ALGA5)) {
				this.effect.setLabel( TactileEffect.ALGA.getLabel() ) ;
			} else {
				this.effect.setLabel( TactileEffect.FALSE.getLabel() ) ;
			}
		} else {
			if (Objects.equals(blockColor, WALL1) ||
					Objects.equals(blockColor, WALL2) ||
					Objects.equals(blockColor, WALL3)) {
				this.effect.setLabel( TactileEffect.TRUE.getLabel() ) ;
			} else {
				this.effect.setLabel( TactileEffect.BRICK.getLabel() ) ;
			}
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
			ErnestModel entity = this.model.getEnvironment().getEntity(
					leftPoint ,
					this.model.getName() ) ;
			this.leftColor = entity.getColor() ;
			this.effect.setColor( Environment.AGENT.getRGB() ) ;
		} else if ( this.model.getEnvironment().affordWalk( leftPoint ) ) {
			if (Objects.equals(blockColor, ALGA1) ||
					Objects.equals(blockColor, ALGA2) ||
					Objects.equals(blockColor, ALGA1) ||
					Objects.equals(blockColor, ALGA3) ||
					Objects.equals(blockColor, ALGA4) ||
					Objects.equals(blockColor, ALGA5)) {
				this.effect.setLabel( TactileEffect.ALGA.getLabel() ) ;
			} else {
				this.effect.setLabel( TactileEffect.FALSE.getLabel() ) ;
			}
		} else {
			if (Objects.equals(blockColor, WALL1) ||
					Objects.equals(blockColor, WALL2) ||
					Objects.equals(blockColor, WALL3)) {
				this.effect.setLabel( TactileEffect.TRUE.getLabel() ) ;
			} else {
				this.effect.setLabel( TactileEffect.BRICK.getLabel() ) ;
			}
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
			ErnestModel entity = this.model.getEnvironment().getEntity(
					rightPoint ,
					this.model.getName() ) ;
			this.rightColor = entity.getColor() ;
			this.effect.setColor( Environment.AGENT.getRGB() ) ;
		} else if ( this.model.getEnvironment().affordWalk( rightPoint ) ) {
			if (Objects.equals(blockColor, ALGA1) ||
					Objects.equals(blockColor, ALGA2) ||
					Objects.equals(blockColor, ALGA1) ||
					Objects.equals(blockColor, ALGA3) ||
					Objects.equals(blockColor, ALGA4) ||
					Objects.equals(blockColor, ALGA5)) {
				this.effect.setLabel( TactileEffect.ALGA.getLabel() ) ;
			} else {
				this.effect.setLabel( TactileEffect.FALSE.getLabel() ) ;
			}
		} else {
			if (Objects.equals(blockColor, WALL1) ||
					Objects.equals(blockColor, WALL2) ||
					Objects.equals(blockColor, WALL3)) {
				this.effect.setLabel( TactileEffect.TRUE.getLabel() ) ;
			} else {
				this.effect.setLabel( TactileEffect.BRICK.getLabel() ) ;
			}
		}

		this.touchAnimWorld() ;
	}
}

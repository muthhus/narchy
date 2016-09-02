package ideal.vacuum.agent.behavior;

import ideal.vacuum.Ernest130Model;
import ideal.vacuum.agent.DesignerListener;
import ideal.vacuum.agent.Move;
import ideal.vacuum.agent.TactileEffect;
import ideal.vacuum.agent.VisualEffect;
import ideal.vacuum.agent.vision.Eye;
import ideal.vacuum.agent.vision.PhotoreceptorCell;
import ideal.vacuum.eca.ActInstance;
import ideal.vacuum.eca.ActInstanceImpl;
import ideal.vacuum.eca.Primitive;
import ideal.vacuum.eca.PrimitiveImpl;
import ideal.vacuum.eca.construct.egomem.Aspect;
import ideal.vacuum.eca.construct.egomem.AspectImpl;
import spacegraph.math.v3;

import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * @author Joseph GARNIER
 * @version $Revision: 386 $
 */
public class BehaviorErnest9 extends AbstractBehavior {

	public BehaviorErnest9( Ernest130Model model , DesignerListener listener , Eye eye ) {
		super( model , listener , eye ) ;
	}

	private void lookTheWorld() {
		//GraphicProperties ernestGraphicProperties = this.model.getCopyOfGraphicProperties() ;
		this.colliculus.saccade();
		this.notifyBehaviorStateChanged( new BehaviorStateChangeEvent( this , this
				.getCurrentBehaviorState() ) ) ;
	}

	private void buildPlaces( String moveLabel, float angle , float xTranslation ) {
		this.actInstances.clear();
		Map<PhotoreceptorCell , VisualEffect> stimuli = this.colliculus.visualEffect( angle , xTranslation ) ;
		for ( Entry<PhotoreceptorCell , VisualEffect> entry : stimuli.entrySet() ) {
			Primitive primitive = PrimitiveImpl.get( moveLabel + entry.getValue().getLabel() ) ;
			v3 position = this.colliculus.getEventPosition( entry.getKey() , entry.getValue() ) ;
			ActInstanceImpl place = new ActInstanceImpl( primitive , position ) ;
			//place.setValue(entry.getKey().getBlockColor().getRGB()); // OG
			place.setAspect(AspectImpl.createOrGet(entry.getKey().getBlockColor().getRGB())); //OG
			place.setModality(ActInstance.MODALITY_VISION);
			this.actInstances.add( place );
		}		
	}
	
	private void addDefaultPlace(String moveLabel){
		Primitive primitive = PrimitiveImpl.get( moveLabel + VisualEffect.UNCHANGED.getLabel() ) ;
		ActInstanceImpl place = new ActInstanceImpl( primitive , new v3() ) ;
		place.setModality(ActInstance.MODALITY_MOVE);
		place.setAspect(Aspect.MOVE);
		this.actInstances.add(place);
	}
	
	private void addTactileOrVisualPlace( String label , v3 location , int displayCode) {
		Primitive primitive = PrimitiveImpl.get( label ) ;
		if (primitive != null){ // OG
			ActInstanceImpl place = new ActInstanceImpl( primitive , location ) ;
			//place.setValue(displayCode);
			place.setAspect(Aspect.MOVE);
			this.actInstances.add( place );
		}
		else 
			System.out.println("Illegal interaction label: " + label);
	}
	
	private void addBumpPlace() {
		Primitive primitive = PrimitiveImpl.get( Move.MOVE_FORWARD.getLabel() + TactileEffect.FALSE.getLabel() ) ;
			ActInstanceImpl place = new ActInstanceImpl( primitive , new v3( 1 , 0 , 0 ) ) ;
			//place.setValue(0xFF0000);
			place.setAspect(Aspect.BUMP);
			place.setModality(ActInstance.MODALITY_BUMP);
			this.actInstances.add( place );
	}
	
	private void addConsumePlace(int displayCode) {
		Primitive primitive = PrimitiveImpl.get( Move.MOVE_FORWARD.getLabel() + TactileEffect.FOOD.getLabel() ) ;
			ActInstanceImpl place = new ActInstanceImpl( primitive , new v3() ) ;
			//place.setValue(displayCode);
			place.setAspect(Aspect.CONSUME);
			place.setModality(ActInstance.MODALITY_CONSUME);
			this.actInstances.add( place );
	}
	
	@Override
	protected void turnRight() {
		this.turnRightAnimWorld() ;
		this.lookTheWorld() ;

		this.buildPlaces( Move.TURN_RIGHT.getLabel(), (float) Math.PI / 2 , 0 ) ;
		if (this.actInstances.isEmpty()) addDefaultPlace(Move.TURN_RIGHT.getLabel());
		this.setTransform( (float) Math.PI / 2 , 0 );
	}

	@Override
	protected void turnLeft() {
		this.turnLeftAnimWorld() ;
		this.lookTheWorld() ;

		this.buildPlaces( Move.TURN_LEFT.getLabel(), (float) -Math.PI / 2 , 0 ) ;
		if (this.actInstances.isEmpty()) addDefaultPlace(Move.TURN_LEFT.getLabel());
		this.setTransform( (float) -Math.PI / 2 , 0 );
	}

	@Override
	protected void moveForward() {
		v3 localPoint = new v3( this.model.DIRECTION_AHEAD ) ;
		v3 aheadPoint = this.model.localToParentRef( localPoint ) ;

		if ( this.model.getEnvironment().affordWalk( aheadPoint ) &&
				!this.model.affordCuddle( aheadPoint ) ) {
			this.moveForwardAnimWorld() ;
			this.lookTheWorld() ;
			this.buildPlaces( Move.MOVE_FORWARD.getLabel(), 0 , -1 );
			
			if ( this.model.getEnvironment().isFood( aheadPoint.x , aheadPoint.y ) ) {
				int displayCode = this.model.getEnvironment().seeBlock( aheadPoint.x , aheadPoint.y ).getRGB();
				this.effect.setColor( displayCode );
//				this.effect.setLocation( new v3() ) ;
				this.model.getEnvironment().eatFood( aheadPoint ) ;
//				this.effect.setLabel( TactileEffect.FOOD.getLabel() ) ;
				//this.addTactileOrVisualPlace( Move.MOVE_FORWARD.getLabel() + TactileEffect.FOOD.getLabel() , new v3(), displayCode ); // OG
				this.addConsumePlace(displayCode);
			}
//			this.effect.setTransformation( 0 , -1 ) ;
			this.setTransform( 0 , -1 );

		} else {
			this.bumpAheadAnimWorld() ;
			this.lookTheWorld() ;
			//this.buildPlaces( Move.MOVE_FORWARD.getLabel(), 0 , 0 );
			//this.addTactileOrVisualPlace( Move.MOVE_FORWARD.getLabel() + TactileEffect.FALSE.getLabel() , new v3( 1 , 0 , 0 ), 0xFF0000 );
			this.addBumpPlace();
//			this.effect.setLocation( new v3( 1 , 0 , 0 ) ) ;
//			this.effect.setColor( Color.RED.getRGB() );
//			this.effect.setLabel( TactileEffect.FALSE.getLabel() ) ;
			this.setTransform( 0 , 0 );
		}
		if (this.actInstances.isEmpty()) addDefaultPlace(Move.MOVE_FORWARD.getLabel());

	}

	@Override
	protected void moveBackward() {
	}

	@Override
	protected void touch() {
	}

	@Override
	protected void touchLeft() {
	}

	@Override
	protected void touchRight() {
	}
}

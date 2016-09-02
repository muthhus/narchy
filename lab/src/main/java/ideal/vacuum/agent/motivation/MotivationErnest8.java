package ideal.vacuum.agent.motivation;

import ideal.vacuum.agent.Move;
import ideal.vacuum.agent.TactileEffect;
import ideal.vacuum.agent.VisualEffect;
import ideal.vacuum.ernest.IErnest;
import ideal.vacuum.ernest.Pair;

/**
 * 
 * @author Joseph GARNIER
 * @version $Revision: 357 $
 */
public class MotivationErnest8 implements Motivation {

	@Override
    public void putMotivation(IErnest ernest ) {
		// r(u)
		Pair<Move , Integer>[] moves = new Pair[3] ;
		moves[0] = Pair.create( Move.MOVE_FORWARD , -2 ) ;
		moves[1] = Pair.create( Move.TURN_RIGHT , -1 ) ;
		moves[2] = Pair.create( Move.TURN_LEFT , -1 ) ;

		// r(e)
		Pair<VisualEffect , Integer>[] eyesEffects = new Pair[6];
		eyesEffects[0] = Pair.create( VisualEffect.APPEAR , 15 );
		eyesEffects[1] = Pair.create( VisualEffect.MOVE , 0 );
		eyesEffects[2] = Pair.create( VisualEffect.CLOSER , 10 );
		eyesEffects[3] = Pair.create( VisualEffect.UNCHANGED , 0 );
		eyesEffects[4] = Pair.create( VisualEffect.FARTHER , -15 );
		eyesEffects[5] = Pair.create( VisualEffect.DISAPPEAR , -15 );

		// r(f)
		Pair<TactileEffect , Integer>[] tactileEffects = new Pair[2] ;
		tactileEffects[0] = Pair.create( TactileEffect.TRUE , 0 ) ;
		tactileEffects[1] = Pair.create( TactileEffect.FALSE , -10 ) ;

		for ( Pair<VisualEffect , Integer> eyeEffect : eyesEffects ) {
			for ( Pair<Move , Integer> move : moves ) {
				// r(u,y) = r(u) + r(e)
				int satisfaction = move.getRight() + eyeEffect.getRight() ;
				String interactionLabel = move.getLeft().getLabel() +
						eyeEffect.getLeft().getLabel() ;
				ernest.addInteraction( interactionLabel , satisfaction ) ;
			}
		}
		// Bump
		ernest.addInteraction( Move.MOVE_FORWARD.getLabel() + TactileEffect.FALSE.getLabel() , -8 ) ;
		// eat
		ernest.addInteraction( Move.MOVE_FORWARD.getLabel() + TactileEffect.FOOD.getLabel() , 0 );		
	}
}

package ideal.vacuum.agent.behavior;

import ideal.vacuum.agent.Move;
import ideal.vacuum.eca.ActInstance;
import ideal.vacuum.ernest.Effect;
import spacegraph.phys.math.Transform;

import java.util.List;

/**
 * 
 * @author Joseph GARNIER
 * @version $Revision: 395 $
 */
public interface Behavior {

	BehaviorState doMovement(Move schema);

	BehaviorState getCurrentBehaviorState() ;

	Effect getEffect() ;

	Transform getTransform() ;

	List<ActInstance> getPlaces() ;
}

package ideal.vacuum.agent.behavior;

import java.util.EventObject;

public class BehaviorStateChangeEvent extends EventObject {

	private BehaviorState behaviorState;
	
	public BehaviorStateChangeEvent( Object source , BehaviorState behaviorState ) {
		super( source ) ;
		try {
			this.behaviorState = behaviorState.clone();
		} catch ( CloneNotSupportedException e ) {
			e.printStackTrace();
		}
	}

	public BehaviorState getBehaviorState() {
		return this.behaviorState ;
	}
}

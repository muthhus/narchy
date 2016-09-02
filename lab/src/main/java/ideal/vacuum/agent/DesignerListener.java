package ideal.vacuum.agent;

import ideal.vacuum.agent.behavior.BehaviorStateChangeEvent;

import java.util.EventListener;

/**
 * 
 * @author Joseph GARNIER
 * @version $Revision: 345 $
 */
public interface DesignerListener extends EventListener{

	void notifyGraphicPropertiesChanged(GraphicPropertiesChangeEvent properties);
	
	void notifyBehaviorStateChanged(BehaviorStateChangeEvent behaviorState);
}

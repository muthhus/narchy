package ideal.vacuum.eca.construct;

import ideal.vacuum.eca.ss.enaction.Act;
import ideal.vacuum.tracing.ITracer;

import java.util.List;

/**
 * An Appearance of a PhenomenonType in an Area.
 * An Appearance may also be called an Observation
 * @author Olivier
 */
public interface Appearance {
	
	/**
	 * @return The Observation's label
	 */
    String getLabel();
	
	/**
	 * @param act The act to add to this action.
	 */
    void addAct(Act act);

	/**
	 * @return The list of primitive interactions that perform this action.
	 */
    List<Act> getActs();
	
	/**
	 * @param act The primitive to check 
	 * @return true if this primitive belongs to this action
	 */
    boolean isEvokedBy(Act act);
	
	Act getStillAct();

	void setStillAct(Act stillAct);
	
	List<Act> getAffordedActs();
	
	void addAffordedAct(Act act);

	Act getFlowAct();

	void setFlowAct(Act flowAct);

	Action getDiscriminentAction();

	void setDiscriminentAction(Action discriminentAction);

	/**
	 * @return The observation's phenomenon
	 */
	//public PhenomenonType getPhenomenonType();	

	/**
	 * @return The Observation's area
	 */
	//public Area getArea();
	//public void setArea(Area area);

    void trace(ITracer tracer, Object e);
	
}

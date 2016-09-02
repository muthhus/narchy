package ideal.vacuum.eca.decider;

import ideal.vacuum.eca.construct.Action;
import ideal.vacuum.eca.ss.enaction.Act;

/**
 * A proposition to perform an action. 
 * @author ogeorgeon
 */
public interface ActionProposition extends Comparable<ActionProposition> {
	/**
	 * @return The interaction proposed by this proposition.
	 */
    Action getAction();
	
	/**
	 * @return The proposition's weight according to the Spatial System.
	 */
    int getSSWeight();
	
	/**
	 * @param ssWeight The weight to add to the proposition.
	 */
    void addSSWeight(int ssWeight);
		
	
	//public void setExperiment(Experiment experiment);
	//public Experiment getExperiment();
	
	/**
	 * @return The anticipated act.
	 */
	//public Appearance getAnticipatedAppearance();

	/**
	 * @param anticipatedAct The anticipated act.
	 */
	//public void setAnticipatedAppearance(Appearance appearance);
	
	//public float getConfidence();
	//public void setConfidence(float confidence);
	
	/**
	 * Two propositions are equal if they propose the same action. 
	 */
    boolean equals(Object o);
	
	void setSpatialAnticipatedAct(Act ssAnticipatedAct);
	Act getSpatialAnticipatedAct();
	
	void setSSActWeight(int ssActWeight);
	int getSSActWeight();
	
}

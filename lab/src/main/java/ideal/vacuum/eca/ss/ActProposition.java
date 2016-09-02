package ideal.vacuum.eca.ss;

import ideal.vacuum.eca.ss.enaction.Act;

/**
 * A proposition to enact an act. 
 * @author ogeorgeon
 */
public interface ActProposition extends Comparable<ActProposition>
{
	/**
	 * @return The interaction proposed by this proposition.
	 */
    Act getAct();
	
	/**
	 * @return The weight of the prosing act.
	 */
    int getWeight();
	
	/**
	 * @param w The weight of the proposing act.
	 */
    void addWeight(int w);
	
	/**
	 * The weighted value is needed to handles the propagation of the proposition weight to the sub act 
	 * @param weightedValue The weight of the proposing Act times the value of the proposed act
	 */
    void setWeightedValue(int weightedValue);
	
	/**
	 * The weighted value is needed to handles the propagation of the proposition weight to the sub act 
	 * @return The weight of the proposing Act times the value of the proposed act
	 */
    int getWeightedValue();
		
	/**
	 * Two propositions are equal if they propose the same interaction. 
	 */
    boolean equals(Object o);
}

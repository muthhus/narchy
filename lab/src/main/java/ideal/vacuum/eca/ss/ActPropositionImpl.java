package ideal.vacuum.eca.ss;

import ideal.vacuum.eca.ss.enaction.Act;

/**
 * A proposition that Ernest enacts an interaction. 
 * @author ogeorgeon
 */
public class ActPropositionImpl implements ActProposition 
{
	private final Act act;
	private int weightedValue;
	private int weight;
	
	/**
	 * Constructor. 
	 * @param a The proposed interaction.
	 * @param w The proposal's weight.
	 */
	public ActPropositionImpl(Act a, int w){
		act = a;
		weight = w;
	}

	@Override
    public int compareTo(ActProposition o){
		return new Integer(o.getWeight()).compareTo(weight);
	}

	@Override
    public Act getAct(){
		return act;
	}

	@Override
    public int getWeight(){
		return weight;
	}
	
	@Override
    public void addWeight(int w){
		weight += w;
	}

	/**
	 * Two propositions are equal if they propose the same interaction. 
	 */
	public boolean equals(Object o)
	{
		boolean ret = false;
		
		if (o == this)
			ret = true;
		else if (o == null)
			ret = false;
		else if (!o.getClass().equals(this.getClass()))
			ret = false;
		else
		{		
			ActPropositionImpl other = (ActPropositionImpl)o;
            ret = other.act == this.act;
		}
		
		return ret;
	}

	/**
	 * Generate a textual representation of the proposition for debug.
	 * @return A string that represents the proposition. 
	 */
//	public String toString(){
//		return act + " with weight = " + weight/10;
//	}

	@Override
    public int getWeightedValue() {
		return weightedValue;
	}

	@Override
    public void setWeightedValue(int weightedValue) {
		this.weightedValue = weightedValue;
	}
	
	public String toString(){
		return "act: " + this.act.getLabel() + " weighted_value: " + this.weightedValue / 10 + " proposing_weight: " + this.weight;
	}

}

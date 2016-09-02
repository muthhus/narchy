package ideal.vacuum.eca.decider;

import ideal.vacuum.eca.construct.Action;
import ideal.vacuum.eca.construct.experiment.Experiment;
import ideal.vacuum.eca.ss.enaction.Act;

/**
 * A proposition to perform an action. 
 * @author ogeorgeon
 */
public class ActionPropositionImpl implements ActionProposition {

	private final Action action;
	private int ssWeight;
	private Experiment experiment;
	//private Appearance anticipatedAppearance = null;
	//private float confidence = 0;
	
	private Act ssAnticipatedAct;
	private int ssActWeight;
	
	/**
	 * Constructor. 
	 * @param a The proposed action.
	 * @param ssWeight The weight proposed by the ss.
	 */
	public ActionPropositionImpl(Action a, int ssWeight){
		this.action = a;
		this.ssWeight = ssWeight;
	}

	@Override
    public int compareTo(ActionProposition a){
		return new Integer(a.getSSWeight()).compareTo(ssWeight);
	}

	@Override
    public Action getAction(){
		return action;
	}

	@Override
    public int getSSWeight(){
		return ssWeight;
	}
	
	@Override
    public void addSSWeight(int w){
		ssWeight += w;
	}

	/**
	 * Two propositions are equal if they propose the same action. 
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
			ActionPropositionImpl other = (ActionPropositionImpl)o;
            ret = other.action == this.action;
		}
		
		return ret;
	}

	/**
	 * Generate a textual representation of the proposition for debug.
	 * @return A string that represents the proposition. 
	 */

//	public Appearance getAnticipatedAppearance() {
//		return anticipatedAppearance;
//	}
//
//	public void setAnticipatedAppearance(Appearance anticipatedAct) {
//		this.anticipatedAppearance = anticipatedAct;
//	}

	@Override
    public int getSSActWeight() {
		return ssActWeight;
	}

	@Override
    public void setSSActWeight(int ssActWeight) {
		this.ssActWeight = ssActWeight;
	}

	@Override
    public Act getSpatialAnticipatedAct() {
		return ssAnticipatedAct;
	}

	@Override
    public void setSpatialAnticipatedAct(Act ssAnticipatedAct) {
		this.ssAnticipatedAct = ssAnticipatedAct;
	}

	public String toString(){
        String proposition = "action " + action.getLabel();
        proposition += " weight " + ssWeight / 10;
		if (this.experiment != null)
			proposition += " exeperiment " + this.experiment.getLabel();
        if (ssAnticipatedAct != null){
            proposition += " ss_act " + ssAnticipatedAct.getLabel();
            proposition += " ss_value " + ssAnticipatedAct.getValue();
		}				
		return proposition;
	}

//	public float getConfidence() {
//		return confidence;
//	}
//
//	public void setConfidence(float confidence) {
//		this.confidence = confidence;
//	}

	public Experiment getExperiment() {
		return experiment;
	}

	public void setExperiment(Experiment experiment) {
		this.experiment = experiment;
	}

}

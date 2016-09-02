package ideal.vacuum.eca.construct.experiment;

import ideal.vacuum.eca.construct.Appearance;
import ideal.vacuum.eca.construct.Displacement;
import ideal.vacuum.eca.ss.enaction.Act;
import ideal.vacuum.tracing.ITracer;

/**
 * An experiment is an Action performed on an Appearance.
 * Experiments record the resulting act, displacement, and postAppearance.
 * so as to predict what act will result from an Action performed on an Appearance. 
 * @author Olivier
 */
public interface Experiment {
	
	/**
	 * @return This experiment's label.
	 */
    String getLabel();
	
	/**
	 * @param act the act to add to this experiment 
	 */
    void incActCounter(Act act);
	
	/**
	 * @param displacement The displacement to record to this experiment
	 */
    void incDisplacementCounter(Displacement displacement);
	
	/**
	 * @param appearance The post-appearance
	 */
    void incPostAppearanceCounter(Appearance appearance);

	/**
	 * @return The act most probably resulting from this experiment.
	 */
	//public Act predictAct();

	/**
	 * @return The displacement most probably resulting from this experiment
	 */
    Displacement predictDisplacement();
	
	/**
	 * @return The post-appearance most probably resulting from this experiment
	 */
    Appearance predictPostAppearance();
	
	/**
	 * @return True if this experiment has been made. 
	 */
    boolean isTested();
	
	float getConfidence();
	
	void trace(ITracer tracer, Object e);

}

package ideal.vacuum.eca.ss;


import ideal.vacuum.eca.ss.enaction.Enaction;
import ideal.vacuum.tracing.ITracer;

import java.util.ArrayList;

/**
 * The sequential system of the Enactive Cognitive Architecture.
 * @author ogeorgeon
 */
public interface IImos 
{
	/**
	 * @param regularityThreshold The regularity sensibility threshold.
	 */
    void setRegularityThreshold(int regularityThreshold);

	/**
	 * @param tracer The tracer used to generate the activity traces
	 */
    void setTracer(ITracer<Object> tracer);
	
	/**
	 * Track the enaction at hand. 
	 * @param enaction The current enaction.
	 */
    void track(Enaction enaction);
	
	/**
	 * Terminates the enaction at hand
	 * Record and reinforce new schemas and construct the final context.
	 * @param enaction The current enaction that is being terminated.
	 */
    void terminate(Enaction enaction);
	
	/**
	 * Generates a list of propositions based on the enaction's activation context.
	 * @param enaction The previous enaction
	 * @return The next list of propositions
	 */
    ArrayList<ActProposition> propose(Enaction enaction);

	/**
	 * The counter of interaction cycles.
	 * @return The current interaction cycle number.
	 */
	//public int getCounter();	
}

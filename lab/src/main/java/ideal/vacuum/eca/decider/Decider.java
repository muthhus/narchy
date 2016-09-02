package ideal.vacuum.eca.decider;

import ideal.vacuum.eca.ss.enaction.Enaction;
import ideal.vacuum.tracing.ITracer;

/**
 * A decider decides what interaction to try to enact next
 * when the previous decision cycle is over
 * based on the current state of sequential and spatial memory
 * and based on autotelic and interactional motivation
 * @author Olivier
 */
public interface Decider 
{
	/**
	 * @param tracer The tracer.
	 */
    void setTracer(ITracer tracer);

	/**
	 * @param regularityThreshold The regularity sensibility threshold.
	 */
    void setRegularityThreshold(int regularityThreshold);
	
	/**
	 * @param maxSchemaLength The maximum length of acts
	 */
    void setMaxSchemaLength(int maxSchemaLength);

	/**
	 * @param enaction The current enaction.
	 * @return The next enaction.
	 */
    Enaction decide(Enaction enaction);
	
	/**
	 * @param enaction The current enaction.
	 */
    void carry(Enaction enaction);
}

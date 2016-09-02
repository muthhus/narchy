package ideal.vacuum.eca;

import ideal.vacuum.eca.construct.egomem.Aspect;
import ideal.vacuum.eca.spas.Placeable;
import ideal.vacuum.eca.ss.enaction.Act;
import ideal.vacuum.tracing.ITracer;

/**
 * An Act Instance is an occurrence of the enaction of an interaction memorized in spatio-temporal memory.
 * @author Olivier
 */
public interface ActInstance extends Placeable
{	
	
	int MODALITY_MOVE = 0;
	int MODALITY_BUMP = 1;
	int MODALITY_VISION = 2;
	int MODALITY_CONSUME = 3;
	
	/**
	 * @return This Act Instance's primitive interaction.
	 */
    Primitive getPrimitive();
	
	/**
	 * @return The act constructed from this act instance.
	 */
    Act getAct();
	
	/**
	 * Normalize this act instance.
	 * @param scale The unit for normalization
	 */
    void normalize(float scale);

	/**
	 * @return The aspect 
	 */
    Aspect getAspect();
	
	/**
	 * @param aspect The aspect sensed in the environment
	 */
    void setAspect(Aspect aspect);
	
	/**
	 * @return The sensory modality
	 */
    int getModality();
	
	/**
	 * @param modality The sensory modality
	 */
    void setModality(int modality);
	
	/**
	 * The label of the primitive interaction for display
	 */
	@Override
    String getDisplayLabel();
	
	/**
	 * @param tracer The tracer
	 * @param e The XML element that contains the trace of this act instance.
	 */
    void trace(ITracer tracer, Object e);

}

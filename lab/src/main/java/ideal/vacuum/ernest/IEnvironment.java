package ideal.vacuum.ernest;

import ideal.vacuum.eca.ActInstance;
import ideal.vacuum.eca.Primitive;
import ideal.vacuum.tracing.ITracer;
import spacegraph.phys.math.Transform;


/**
 * Interface for an Environment suitable to Ernest.
 * @author mcohen
 * @author ogeorgeon
 */
public interface IEnvironment 
{
	/**
	 * Enact a primitive schema and return the enaction status.
	 * @param primitive The primitive schema that Ernest has chosen to enact.
	 * @return The effect that results from the enaction of the primitive schema in the environment.
	 */
    ActInstance enact(Primitive primitive);
	
	/**
	 * Intialize Ernest's possibilities of interaction.
	 * @param ernest The Ernest agent.
	 */
    void initErnest(IErnest ernest);
	
	/**
	 * Trace the environment's state.
	 * @param tracer the tracer.
	 */
    void trace(ITracer tracer);
	
	Transform getTransformation();
}

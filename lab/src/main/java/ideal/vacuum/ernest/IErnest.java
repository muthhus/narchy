package ideal.vacuum.ernest;

import ideal.vacuum.eca.ActInstance;
import ideal.vacuum.eca.Primitive;
import ideal.vacuum.eca.spas.Placeable;
import ideal.vacuum.tracing.ITracer;
import spacegraph.phys.math.Transform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * The interface through which the environment can use an Ernest agent. 
 * @author ogeorgeon
 */
public interface IErnest 
{

	/**
	 * Set Ernest's fundamental learning parameters.
	 * Use null to leave a value unchanged.
	 * @param regularityThreshold The Regularity Sensibility Threshold.
	 * @param maxSchemaLength The Maximum Schema Length.
	 */
    void setParameters(int regularityThreshold, int maxSchemaLength);
	
    /**
	 * Initialize the tracer that generates Ernest's activity trace.
	 * (The tracer is instantiated by the environment so that the environment can choose the suitable tracer and also use it to trace things) 
	 * @param tracer The tracer.
	 */
    void setTracer(ITracer tracer);

	/**
	 * Get a description of Ernest's internal state.
	 * (This is used to display Ernest's internal state in the environment)
	 * @return A representation of Ernest's internal state
	 */
    String internalState();
	
	/**
	 * Run Ernest one step.
	 * @param effect The effect received from the environment.
	 * @return The next primitive schema to enact.
	 */
    String step(Effect effect);
	
	Primitive step(List<ActInstance> actInstances, Transform transform);
	
	/**
	 * @param i x coordinate (0 = left, 2 = right)
	 * @param j y coordinate (0 = ahead, 2 = behind)
	 * @return The value in the corresponding place in the environment. 
	 */
	//public int getValue(int i, int j);
    int getDisplayCode();

	/**
	 * @param label The primitive interaction's label
	 * @param satisfaction The satisfaction.
	 * @return The created or already existing act.
	 */
    Primitive addInteraction(String label, int satisfaction);

	/**
	 * @return The list of places in Ernest's local space memory.
	 */
    ArrayList<Placeable> getPlaceList();
	
	/**
	 * @return The counter of cognitive cycles.
	 */
    int getClock();
	
	/**
	 * @return The counter of updates from the spatial system.
	 */
    int getUpdateCount();
	
	/**
	 * @return The list of primitive interactions available to Ernest.
	 */
    Collection<Primitive> getPrimitives();
	
	/**
	 * @return The transformation of spatial memory to anim.
	 */
    Transform getTransformToAnim();
	
}
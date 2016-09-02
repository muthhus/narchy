package ideal.vacuum.ernest;


import ideal.vacuum.tracing.ITracer;
import spacegraph.math.v3;
import spacegraph.phys.math.Transform;

/**
 * An effect sensed by Ernest after making a move in the environment.
 * Or an effect expected after simulating a move in spatial memory.
 * @author ogeorgeon
 */
public interface Effect 
{
	
	/**
	 * @param label The label of the enacted primitive interaction.
	 */
    void setEnactedInteractionLabel(String label);

	/**
	 * @return The label of the primitive interaction.
	 */
    String getEnactedInteractionLabel();
	
	/**
	 * @param label The elementary effect of the enacted primitive scheme.
	 */
    void setLabel(String label);
	
	/**
	 * @return The elementary effect of the enacted primitive scheme.
	 */
    String getLabel();
	
	/**
	 * @param location The location concerned by the enacted scheme.
	 */
    void setLocation(v3 location);
	
	/**
	 * @return The location concerned by the enacted scheme.
	 */
    v3 getLocation();
	
	/**
	 * Set the transformation caused by the scheme enaction
	 * @param angle The angle of rotation.
	 * @param x The translation along the agent axis.
	 */
    void setTransformation(float angle, float x);

	/**
	 * @return The agent's movement during the scheme enaction.
	 */
    Transform getTransformation();
	
	/**
	 * @param color The color used to represent this effect in the trace
	 */
    void setColor(int color);
	
	/**
	 * @return The color used to represent this effect in the trace
	 */
    int getColor();

	/**
	 * @param tracer The tracer
	 */
    void trace(ITracer tracer);
}

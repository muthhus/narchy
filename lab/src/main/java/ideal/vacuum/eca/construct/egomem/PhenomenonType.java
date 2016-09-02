package ideal.vacuum.eca.construct.egomem;

import ideal.vacuum.eca.Primitive;
import ideal.vacuum.tracing.ITracer;

import java.util.List;

/**
 * A PhenomenonType is intended to represent a type of phenomenon that can be observed in the external world.
 * A PhenomenonType conflates primitive interactions based on the fact that their spatial location consistently overlaps.
 * A PhenomenonType may also be called a Bundle.
 * @author Olivier
 */
public interface PhenomenonType {
	
	/** Predefined phenomenon type */
    PhenomenonType EMPTY = PhenomenonTypeImpl.createOrGet("0");
	
	/**
	 * @return This PhenomenonType's label.
	 */
    String getLabel();
	
	/**
	 * @param aspect The visual aspect
	 */
    void setAspect(Aspect aspect);
	
	/**
	 * @return The visual aspect
	 */
    Aspect getAspect();
	
	/**
	 * @param primitive The primitive interaction to add to this phenomenon type
	 */
    void addPrimitive(Primitive primitive);
	
	/**
	 * @return The list of primitive interactions attached to this phenomenon type
	 */
    List<Primitive> getPrimitives();
	
	/**
	 * @param primitive The primitive to check 
	 * @return true if this primitive belongs to this phenomenon type.
	 */
    boolean contains(Primitive primitive);
	
	/**
	 * Trace this phenomenon type
	 * @param tracer The tracer
	 * @param e The xml tag that contains the trace
	 */
    void trace(ITracer tracer, Object e);
	
	void setAttractiveness(int attractiveness);
	int getAttractiveness();

}

package ideal.vacuum.eca.construct.egomem;

import ideal.vacuum.eca.spas.Placeable;
import ideal.vacuum.tracing.ITracer;
import spacegraph.math.v3;


/**
 * An instance of phenomenon known to be present in the surrounding environment
 * @author Olivier
 */
public interface PhenomenonInstance extends Placeable {
	
	PhenomenonInstance EMPTY = new PhenomenonInstanceImpl(PhenomenonType.EMPTY, new v3());
	
	/**
	 * @return The primitive interaction
	 */
    PhenomenonType getPhenomenonType();
	
	/**
	 * @param phenomenonType The type of this phenomenon 
	 */
    void setPhenomenonType(PhenomenonType phenomenonType);
	
	/**
	 * @param tracer The tracer
	 * @param e the xml element that contains the trace of this phenomenon instance
	 */
    void trace(ITracer tracer, Object e);
	
	void setClock(int clock);
	
}

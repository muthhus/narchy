package ideal.vacuum.eca.spas;

import ideal.vacuum.eca.construct.egomem.PhenomenonInstance;
import ideal.vacuum.eca.ss.enaction.Enaction;
import ideal.vacuum.tracing.ITracer;

import java.util.ArrayList;

/**
 * The spatial system.
 * @author Olivier
 */
public interface Spas 
{

	/**
	 * @param tracer The tracer
	 */
    void setTracer(ITracer tracer);
	
	/**
	 * The main routine of the Spatial System that is called on each interaction cycle.
	 * @param enaction The current enaction.
	 */
    void track(Enaction enaction);

	/**
	 * @return The list of places in Ernest's local space memory.
	 */
    ArrayList<Placeable> getPlaceableClones();
	
	/**
	 * @param position The position.
	 * @return The value to display at this position
	 */
	//public int getValue(v3 position);
	
	/**
	 * Provide a rgb code to display the local space map in the environment.
	 * @param i x coordinate.
	 * @param j y coordinate.
	 * @return The value of the bundle in this place in local space memory.
	 */
//	public int getValue(int i, int j);
    int getDisplayCode();
	
	/**
	 * @return the Phenomenon Instance which get the attention of Ernest 
	 */
    PhenomenonInstance getFocusPhenomenonInstance();

}

package ideal.vacuum.eca.spas;

import ideal.vacuum.eca.construct.egomem.PhenomenonInstance;
import spacegraph.math.v3;
import spacegraph.phys.math.Transform;

import java.util.List;

/**
 * A Spatial Memory is a set of placeable objects to which a general spatial transformation apply.
 * @author Olivier
 */
public interface SpatialMemory 
{
	/**
	 * Tick this spatial memory's clock (to compute decay)
	 */
    void tick();
	
	/**
	 * Add a place in spatial memory
	 * @param actInstance The place to add in spatial memory.
	 */
    void addPlaceable(Placeable placeable);
	
	//public void addActInstance(ActInstance actInstance);

		/**
	 * @return A clone of this spatial memory
	 */
	//public ArrayList<Placeable> clonePlaceList();
	
	/**
	 * @return The list of Placeable objects.
	 */
    List<Placeable> getPlaceables();
	
	/**
	 * @param transform The transformation
	 */
    void transform(Transform transform);
	
	/**
	 * Remove places that are older than the decay laps
	 */
    void forgetOldPlaces();
	
	List<PhenomenonInstance> getPhenomenonInstances();

	PhenomenonInstance getPhenomenonInstance(v3 position);

	void clearPhenomenonInstanceFront();
}

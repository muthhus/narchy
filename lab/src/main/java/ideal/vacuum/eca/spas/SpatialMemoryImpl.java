package ideal.vacuum.eca.spas;

import ideal.vacuum.eca.construct.egomem.PhenomenonInstance;
import spacegraph.math.v3;
import spacegraph.phys.math.Transform;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Ernest's spatial memory. 
 * @author Olivier
 */
public class SpatialMemoryImpl implements SpatialMemory, Cloneable
{
	
	/** The radius of a location. */
	public final static float LOCATION_RADIUS = 0.5f;
	public final static float LOCAL_SPACE_MEMORY_RADIUS = 20f;//4f;
	public final static float DISTANCE_VISUAL_BACKGROUND = 10f;
	public final static float EXTRAPERSONAL_DISTANCE = 1.5f;
	public final static float DIAG2D_PROJ = (float) (1/Math.sqrt(2));
	public final static v3 DIRECTION_HERE         = new v3(0, 0, 0);
	public final static v3 DIRECTION_AHEAD        = new v3(1, 0, 0);
	public final static v3 DIRECTION_BEHIND       = new v3(-1, 0, 0);
	public final static v3 DIRECTION_LEFT         = new v3(0, 1, 0);
	public final static v3 DIRECTION_RIGHT        = new v3(0, -1, 0);
	public final static v3 DIRECTION_AHEAD_LEFT   = new v3(DIAG2D_PROJ, DIAG2D_PROJ, 0);
	public final static v3 DIRECTION_AHEAD_RIGHT  = new v3(DIAG2D_PROJ, -DIAG2D_PROJ, 0);
	public final static v3 DIRECTION_BEHIND_LEFT  = new v3(-DIAG2D_PROJ, DIAG2D_PROJ, 0);
	public final static v3 DIRECTION_BEHIND_RIGHT = new v3(-DIAG2D_PROJ, -DIAG2D_PROJ, 0);
	public final static float    SOMATO_RADIUS = 1f;
	
	/** The duration of persistence in local space memory. */
	public static int PERSISTENCE_DURATION = 7;//50;
	
	/** The Local space structure. */
	private final List<Placeable> placeables = new ArrayList<>();
	
	/**
	 * Clone spatial memory to perform simulations
	 * TODO clone the places 
	 * From tutorial here: http://ydisanto.developpez.com/tutoriels/java/cloneable/ 
	 * @return The cloned spatial memory
	 */
//	public ArrayList<Placeable> clonePlaceList() 
//	{
//		ArrayList<Placeable> clonePlaces = new ArrayList<Placeable>();
//		for (Placeable placeable : placeables)
//			clonePlaces.add(placeable.clone());
//
//		return clonePlaces;
//	}

	@Override
    public void tick()
	{
		for (Placeable p : placeables)
			p.incClock();
	}

	@Override
    public void addPlaceable(Placeable placeable){
		placeables.add(placeable);
	}
	
//	public void addActInstance(ActInstance actInstance){
//		placeables.add(actInstance);
//		PhenomenonInstance phenomenonInstance = getPhenomenonInstance(actInstance.getPosition());
//		if (phenomenonInstance == null){
//			// create a new phenomenon type with this act
//			PhenomenonType phenomenonType = PhenomenonTypeImpl.createNew();
//			phenomenonType.addPrimitive(actInstance.getPrimitive());
//			// create a new phenomenon instance at this place
//			phenomenonInstance = new PhenomenonInstanceImpl(phenomenonType, actInstance.getPosition());
//		}
//		else{
//			// add this act to the phenomenon type of this place
//			PhenomenonType phenomenonType = phenomenonInstance.getPhenomenonType();
//			phenomenonType.addPrimitive(actInstance.getPrimitive());
//		}
//	}
	
	@Override
    public void transform(Transform transform)
	{
		for (Placeable p : placeables)
			p.transform(transform);
	}
	
	/**
	 * Clear a position in the local space memory.
	 * @param position The position to clear.
	 */
//	public void clearPlace(v3 position)
//	{
//		for (Iterator<Placeable> it = placeables.iterator(); it.hasNext();)
//		{
//			ActInstance l = (ActInstance)it.next();
//			if (l.isInCell(position))
//				it.remove();
//		}		
//	}
	
	/**
	 * Clear the places farther than DISTANCE_VISUAL_BACKGROUND.
	 */
//	public void clearBackground()
//	{
//		for (Iterator<Placeable> it = placeables.iterator(); it.hasNext();)
//		{
//			Placeable l = it.next();
//			if (l.getDistance() > DISTANCE_VISUAL_BACKGROUND - 1)
//				it.remove();
//		}
//	}
	
	/**
	 * Clear all the places older than PERSISTENCE_DURATION.
	 */
	@Override
    public void forgetOldPlaces()
	{
		for (Iterator<Placeable> it = placeables.iterator(); it.hasNext();)
		{
			Placeable p = it.next();
			if (p.getClock() > PERSISTENCE_DURATION )//|| p.getPosition().x < -.1) 
				it.remove();
		}
	}
		
//	public void trace(ITracer tracer)
//	{
//		if (tracer != null && !placeables.isEmpty())
//		{
//			Object localSpace = tracer.addEventElement("local_space");
//			tracer.addSubelement(localSpace, "position_8", ErnestUtils.hexColor(getDisplayCode(DIRECTION_HERE)));
//			tracer.addSubelement(localSpace, "position_7", ErnestUtils.hexColor(getDisplayCode(DIRECTION_BEHIND)));
//			tracer.addSubelement(localSpace, "position_6", ErnestUtils.hexColor(getDisplayCode(DIRECTION_BEHIND_LEFT)));
//			tracer.addSubelement(localSpace, "position_5", ErnestUtils.hexColor(getDisplayCode(DIRECTION_LEFT)));
//			tracer.addSubelement(localSpace, "position_4", ErnestUtils.hexColor(getDisplayCode(DIRECTION_AHEAD_LEFT)));
//			tracer.addSubelement(localSpace, "position_3", ErnestUtils.hexColor(getDisplayCode(DIRECTION_AHEAD)));
//			tracer.addSubelement(localSpace, "position_2", ErnestUtils.hexColor(getDisplayCode(DIRECTION_AHEAD_RIGHT)));
//			tracer.addSubelement(localSpace, "position_1", ErnestUtils.hexColor(getDisplayCode(DIRECTION_RIGHT)));
//			tracer.addSubelement(localSpace, "position_0", ErnestUtils.hexColor(getDisplayCode(DIRECTION_BEHIND_RIGHT)));
//		}
//	}

	@Override
    public List<Placeable> getPlaceables() {
		return this.placeables;
	}
	
	@Override
    public List<PhenomenonInstance> getPhenomenonInstances() {
		List<PhenomenonInstance> phenomenonInstances = new ArrayList<>();
		for (Placeable placeable : this.placeables)
			if (placeable instanceof PhenomenonInstance)
				phenomenonInstances.add((PhenomenonInstance)placeable);
		return phenomenonInstances;
	}
	
	@Override
    public PhenomenonInstance getPhenomenonInstance(v3 position){
		PhenomenonInstance phenomenonInstance = null;
		for (Placeable placeable : this.placeables)
			if (placeable instanceof PhenomenonInstance)
				if (placeable.isInCell(position))
					phenomenonInstance = (PhenomenonInstance)placeable;
		
		return phenomenonInstance;
	}

	@Override
    public void clearPhenomenonInstanceFront() {
		for (Iterator<Placeable> it = placeables.iterator(); it.hasNext();){
			Placeable placeable = it.next();
			if (placeable instanceof PhenomenonInstance)
				if (placeable.getPlace().getPosition().x > 0)
					it.remove();					
		}
	}
	
}

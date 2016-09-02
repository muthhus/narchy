package ideal.vacuum.eca.construct;

import ideal.vacuum.eca.construct.egomem.Area;
import ideal.vacuum.ernest.ErnestUtils;
import spacegraph.phys.math.Transform;

import java.util.HashMap;
import java.util.Map;

/**
 * A transformation in spatial memory
 * @author Olivier
 */
public class DisplacementImpl implements Displacement {

	private static final Map<String , Displacement> DISPLACEMENTS = new HashMap<>() ;

	/** Appearance DOWN */
	public static String DISPLACEMENT_LABEL_CHANGE = "CHANGE";
	/** Appearance UP */
	public static String DISPLACEMENT_LABEL_STILL = "STILL";

	public static Displacement DISPLACEMENT_CHANGE = new DisplacementImpl(DISPLACEMENT_LABEL_CHANGE);
	public static Displacement DISPLACEMENT_STILL = new DisplacementImpl(DISPLACEMENT_LABEL_STILL);
	
	private final String label;
	
	private final Transform Transform = new Transform();
	private Area preArea;
	private Area postArea;
	
	/**
	 * @param label The displacement's label
	 * @return The displacement
	 */
	public static Displacement createOrGet(String label){
		if (!DISPLACEMENTS.containsKey(label))
			DISPLACEMENTS.put(label, new DisplacementImpl(label));
		return DISPLACEMENTS.get(label);
	}
	
	/**
	 * @param preArea The area before displacement
	 * @param postArea The area after displacement
	 * @return The displacement
	 */
	public static Displacement createOrGet(Area preArea, Area postArea){
		String label = createKey(preArea, postArea);
		if (!DISPLACEMENTS.containsKey(label))
			DISPLACEMENTS.put(label, new DisplacementImpl(preArea, postArea));
		return DISPLACEMENTS.get(label);
	}
	
	private static String createKey(Area preArea, Area postArea){
		return preArea.getLabel() + postArea.getLabel();
	}
	
	/**
	 * @param t The transformation that defines this displacement
	 * @return The displacement
	 */
	public static Displacement createOrGet(Transform t){
		String label = createKey(t);
		if (!DISPLACEMENTS.containsKey(label))
			DISPLACEMENTS.put(label, new DisplacementImpl(t));
		return DISPLACEMENTS.get(label);
	}
	
	/**
	 * @param t The transformation that defines this displacement
	 * @return The displacement
	 */
	private static String createKey(Transform t) {
		String key = "stay";
		float angle = ErnestUtils.angle(t);
		if (Math.abs(angle) > .1){
			if ( angle > 0)	key = "^";
			else			key ="v";
		}
		else{
			if (ErnestUtils.translationX(t) > .5) key =".";
			else key = "<";
		}
		
		// Only distinguish between stay and move.
		if (t.epsilonEquals(new Transform(), .1f))
			key = "stay";
		else
			key = "move";
		
		return key;
	}

	private DisplacementImpl(Area preArea, Area postArea){
		this.preArea = preArea;
		this.postArea = postArea;
		this.label = createKey(preArea, postArea);
	}
	
	private DisplacementImpl(String label){
		this.label = label;
	}
	
	private DisplacementImpl(Transform t){
		this.label = createKey(t);
		this.Transform.set(t);
	}

	@Override
    public String getLabel() {
		return label;
	}
	
	@Override
    public void setTransform(Transform Transform){
		this.Transform.set(Transform);
	}
	
	@Override
    public Transform getTransform(){
		Transform t = this.Transform;
		if (this.label.equals("<")) t = new Transform();
		return t;
	}

	/**
	 * Displacements are equal if they have the same label. 
	 */
	public boolean equals(Object o){
		boolean ret = false;
		
		if (o == this)
			ret = true;
		else if (o == null)
			ret = false;
		else if (!o.getClass().equals(this.getClass()))
			ret = false;
		else
		{
			Displacement other = (Displacement)o;
			//ret = (other.getTransform().epsilonEquals(Transform, .1));
			ret = (other.getLabel().equals(this.label));
		}
		
		return ret;
	}

	@Override
    public Area getPreArea() {
		return preArea;
	}

	@Override
    public Area getPostArea() {
		return postArea;
	}

}

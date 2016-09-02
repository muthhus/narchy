package ideal.vacuum.eca;

import ideal.vacuum.eca.construct.Appearance;
import ideal.vacuum.eca.construct.Displacement;
import ideal.vacuum.eca.construct.DisplacementImpl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A primitive interaction.
 * @author Olivier
 */
public class PrimitiveImpl implements Primitive {

	private static final Map<String , Primitive> INTERACTIONS = new HashMap<>() ;

	private String label = "";
	private int value;
	
	private Displacement displacement = DisplacementImpl.DISPLACEMENT_CHANGE;

	private final Map<Appearance , Appearance> transforms = new HashMap<>() ;
	//private Map<Displacement , Integer> displacements = new HashMap<Displacement , Integer>() ;

	/**
	 * @param label The primitive interaction's label
	 * @param value The primitive interaction's value
	 * @return The primitive interaction created or retrieved
	 */
	public static Primitive createOrGet(String label, int value){
		if (!INTERACTIONS.containsKey(label))
			INTERACTIONS.put(label, new PrimitiveImpl(label, value));
		return INTERACTIONS.get(label);
	}
	
	/**
	 * @param preInteraction The composite interaction's pre-interaction
	 * @param postInteraction The composite interaction's post-interaction
	 * @return The primitive interaction created or retrieved
	 */
	public static Primitive createOrGetComposite(Primitive preInteraction, Primitive postInteraction){
		String label = '(' + preInteraction.getLabel() + postInteraction.getLabel() + ')';
		int value = preInteraction.getValue() + postInteraction.getValue();
		if (!INTERACTIONS.containsKey(label))
			INTERACTIONS.put(label, new PrimitiveImpl(label, value));
		return INTERACTIONS.get(label);
	}
	
	/**
	 * @param label The primitive interaction's label
	 * @return The primitive interaction
	 */
	public static Primitive get(String label){
		return INTERACTIONS.get(label);
	}
	
	/**
	 * @return The collection of all primitive interactions
	 */
	public static Collection<Primitive> getINTERACTIONS() {
		return INTERACTIONS.values();
	}
	
	private PrimitiveImpl(String label, int value){
		this.label = label;
		this.value = value;
//		Act act = ActImpl.createOrGetPrimitiveAct(this, AreaImpl.createOrGet(new v3(1,0,0)));
//		Action action = ActionImpl.createOrGet("[a" + act.getLabel() + "]");
//		action.addAct(act);
//		PhenomenonType phenomenonType = PhenomenonTypeImpl.createOrGet("[p" + label +"]");
//		phenomenonType.addPrimitive(this);	
	}
	
	@Override
    public String getLabel(){
		return this.label;
	}

	@Override
    public int getValue(){
		return this.value;
	}
	
	/**
	 * Interactions are equal if they have the same label. 
	 */
	public boolean equals(Object o)
	{
		boolean ret = false;
		
		if (o == this)
			ret = true;
		else if (o == null)
			ret = false;
		else if (!o.getClass().equals(this.getClass()))
			ret = false;
		else
		{
			Primitive other = (Primitive)o;
			ret = (other.getLabel().equals(this.label));
		}
		
		return ret;
	}

	public String toString()
	{
		return this.label + '(' + this.value / 10 + ')';
	}
	
	@Override
    public void recordTransfomr(Appearance preAppearance, Appearance postAppearance){
		transforms.put(preAppearance, postAppearance);
	}

	@Override
    public Appearance transform(Appearance appearance){
		return transforms.get(appearance);
	}

	@Override
    public Displacement getDisplacement() {
		return displacement;
	}

	@Override
    public void setDisplacement(Displacement displacement) {
		this.displacement = displacement;
	}

//	public void incDisplacementCounter(Displacement displacement){
//		if (displacements.containsKey(displacement))
//			displacements.put(displacement, displacements.get(displacement) + 1);
//		else
//			displacements.put(displacement, 1);
//	}
//
//	public Displacement predictDisplacement(Area area) {
//		int max = 0;
//		Displacement predictDisplacement = null;
//		
//		for (Map.Entry<Displacement, Integer> entry : displacements.entrySet())
//			if (entry.getKey().getPreArea().equals(area) && entry.getValue() > max){
//				predictDisplacement = entry.getKey();
//				max = entry.getValue();
//			}
//		
//		return predictDisplacement;
//	}
//	
//	public String getDisplacementLabels(){
//		String label = "";
//		for (Map.Entry<Displacement, Integer> entry : displacements.entrySet())
//			label += entry.getKey().getLabel() + "(" + entry.getValue() + ") ";
//		
//		return label;
//	}

}

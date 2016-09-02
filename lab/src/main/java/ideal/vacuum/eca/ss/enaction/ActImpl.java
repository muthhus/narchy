package ideal.vacuum.eca.ss.enaction;

import ideal.vacuum.eca.Primitive;
import ideal.vacuum.eca.PrimitiveImpl;
import ideal.vacuum.eca.construct.egomem.Area;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A sensorimotor pattern of interaction of Ernest with its environment 
 * @author Olivier
 */
public class ActImpl implements Act 
{

	/** The list of all acts */
	private static final Map<String , Act> ACTS = new LinkedHashMap<>() ;
	/** Default weight of primitive interactions */
	private static final int PRIMITIVE_WEIGHT = 100;
	
	private String label = "";
	private boolean m_primitive = true;
	private Act preAct;
	private Act postAct;
	private int value;
	private int m_enactionWeight;
	private int m_length = 1;
	private Act m_prescriber;
	private int m_step;
	private Primitive primitive;
	private Area area;
	private int color;
	
	/**
	 * @return The list of all acts known by the agent.
	 */
	public static Collection<Act> getACTS(){
		return ACTS.values();
	}

	/**
	 * @param interaction The primitive interaction from which this act is made.
	 * @param area The area.
	 * @return The created primitive interaction.
	 */
	public static Act createOrGetPrimitiveAct(Primitive interaction, Area area)
	{
		String key = createPrimitiveKey(interaction, area);
		if (!ACTS.containsKey(key)){
			ActImpl newAct = new ActImpl(key, true, null, null, interaction.getValue(), interaction, area);
			ACTS.put(key, newAct);
			System.out.println("Define primitive act " + key);
		}
		return ACTS.get(key);
	}
	
	private static String createPrimitiveKey(Primitive interaction, Area area) {
		String key = interaction.getLabel() + area.getLabel();
		return key;
	}
	
	/**
	 * @param preAct The pre-act.
	 * @param postAct The post-act.
	 * @return The created composite interaction.
	 */
	public static Act createOrGetCompositeAct(Act preAct, Act postAct)
	{
		String key = createCompositeKey(preAct, postAct);
		int enactionValue = preAct.getEnactionValue() + postAct.getEnactionValue();
		if (!ACTS.containsKey(key)){
			Primitive primitive = PrimitiveImpl.createOrGetComposite(preAct.getPrimitive(), postAct.getPrimitive());
			Act newAct = new ActImpl(key, false, preAct, postAct, enactionValue, primitive, postAct.getArea());
			//Act newAct = new ActImpl(key, false, preAct, postAct, enactionValue, null, postAct.getArea());
			
//			Transform newTransform = new Transform();
//          newTransform.mul(postAct.getDisplacement().getTransform(), preAct.getDisplacement().getTransform());
//			Displacement displacement = DisplacementImpl.createOrGet(newTransform);

			ACTS.put(key, newAct);
		}
		return ACTS.get(key);
	}
	
	private static String createCompositeKey(Act preAct, Act postAct) {
		String key = preAct.getLabel() + postAct.getLabel();
		return key;
	}
	
	private ActImpl(String label, boolean primitive, Act preAct, Act postAct, int value, Primitive interaction, Area area)
	{
		this.label = label;
		m_primitive = primitive;
		this.primitive = interaction;
		this.preAct = preAct;
		this.postAct = postAct;
		this.value = value;
		if (primitive)
			m_enactionWeight = PRIMITIVE_WEIGHT;
		else
			m_length = preAct.getLength() + postAct.getLength();
		this.area = area;
	}
	
	@Override
    public Act getPreAct()
	{
		return preAct;
	}

	@Override
    public Act getPostAct()
	{
		return postAct;
	}

	@Override
    public int getEnactionValue()
	{
		return this.value;
	}

	@Override
    public boolean isPrimitive()
	{
		return m_primitive;
	}
	
	/**
	 * Acts are equal if they have the same label. 
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
			Act other = (Act)o;
			ret = (other.getLabel().equals(getLabel()));
		}
		
		return ret;
	}

	@Override
    public String getLabel()
	{
		String l = "";
		if (m_primitive)
			l = this.label;
		else
			l = '(' + preAct.getLabel() + postAct.getLabel() + ')';
		return l; 
	}

	@Override
    public void setWeight(int enactionWeight)
	{
		m_enactionWeight = enactionWeight;
	}

	@Override
    public int getWeight()
	{
		return m_enactionWeight;
	}

	@Override
    public int getLength()
	{
		return m_length;
	}

	@Override
    public void setStep(int step)
	{
		m_step = step;
	}

	@Override
    public int getStep()
	{
		return m_step;
	}

	@Override
    public void setPrescriber(Act prescriber)
	{
		m_prescriber = prescriber;
	}

	@Override
    public Act getPrescriber()
	{
		return m_prescriber;
	}
	
	/**
	 * Update the prescriber if this interaction was enacted
	 */
	@Override
    public Act updatePrescriber()
	{
		Act prescriber = m_prescriber;
		m_prescriber = null;
		Act nextInteraction = null;
		if (prescriber != null)
		{
			int step = prescriber.getStep();
			if (step == 0)
			{
				// The prescriber's pre-interaction was enacted
				prescriber.setStep(0 + 1);
				nextInteraction = prescriber.getPostAct();
				nextInteraction.setPrescriber(prescriber);
			}
			else
			{
				// The prescriber's post-interaction was enacted
				// Update the prescriber's prescriber
				nextInteraction = prescriber.updatePrescriber();
			}
		}
		
		return nextInteraction;
	}

	@Override
    public void terminate()
	{
		if (m_prescriber != null)
		{
			m_prescriber.terminate();
			m_prescriber = null;
		}
		m_step = 0;
	}

	@Override
    public Act prescribe()
	{
		Act prescribedInteraction = null;
		if (m_primitive)
			prescribedInteraction = this;
		else
		{
			m_step = 0;
			preAct.setPrescriber(this);
			prescribedInteraction = preAct.prescribe();
		}
		return prescribedInteraction;		
	}
	
	public String toString()
	{
		return getLabel() + '(' + value/10 + ',' + m_enactionWeight + ')';
	}

	@Override
    public Area getArea() {
        if (m_primitive)
			return this.area;
		else {
            return postAct.getArea();
        }
	}

	@Override
    public void setArea(Area area) {
		this.area = area;
	}

	@Override
    public Primitive getPrimitive() {
		return this.primitive;
	}

	@Override
    public void setPrimitive(Primitive primitive) {
		this.primitive = primitive;
	}

	@Override
    public int getColor() {
		return color;
	}

	@Override
    public void setColor(int color) {
		this.color = color;
	}

	@Override
    public int getValue() {
        if (m_primitive)
			return this.primitive.getValue();
		else {
            return this.getPreAct().getValue() + postAct.getValue();
        }
	}
	
//	public void initPrimitive(){
//		this.primitive = PrimitiveImpl.createOrGetComposite(preAct.getPrimitive(), postAct.getPrimitive());
//	}

}

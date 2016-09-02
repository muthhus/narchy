package ideal.vacuum.eca;


import ideal.vacuum.eca.construct.egomem.Area;
import ideal.vacuum.eca.construct.egomem.AreaImpl;
import ideal.vacuum.eca.construct.egomem.Aspect;
import ideal.vacuum.eca.spas.Place;
import ideal.vacuum.eca.spas.PlaceImpl;
import ideal.vacuum.eca.ss.enaction.Act;
import ideal.vacuum.eca.ss.enaction.ActImpl;
import ideal.vacuum.tracing.ITracer;
import spacegraph.math.v3;
import spacegraph.phys.math.Transform;


/**
 * An Act Instance is an occurrence of the enaction of an interaction memorized in spatio-temporal memory.
 * @author Olivier
 */
public class ActInstanceImpl implements ActInstance
{
	private final Primitive primitive;
	private Place place;
	private int clock;
	private Aspect aspect = Aspect.MOVE;
	private int modality;
	private boolean focus;
	
	/**
	 * Create a new place 
	 * (The provided position is cloned so the place can be moved without changing the provided position).
	 * @param primitive The interaction at this place.
	 * @param position This place's position.
	 */
	public ActInstanceImpl(Primitive primitive, v3 position){
		this.primitive = primitive;
		this.place = new PlaceImpl(position);
	}
	
	@Override
    public Act getAct() {
		return ActImpl.createOrGetPrimitiveAct(primitive, this.place.getArea());
	}

	@Override
    public Primitive getPrimitive() {
		return this.primitive;
	}

	@Override
    public v3 getPosition() {
		return this.place.getPosition();
	}
	
	/**
	 * Clone an Act Instance
	 * @return The cloned Act Instance
	 */
	@Override
    public ActInstance clone(){
		ActInstanceImpl clonePlace = null;
		try {
			clonePlace = (ActInstanceImpl) super.clone();
		} catch(CloneNotSupportedException cnse) {
			cnse.printStackTrace(System.err);
		}

		// We must clone the objects because they are passed by reference by default
		clonePlace.place = this.place.clone();
		//clonePlace.setPosition(this.position);
		//clonePlace.setOrientation(this.orientation);

		return clonePlace;
	}
	
	@Override
    public void transform(Transform transform)
	{
		this.place.transform(transform);
	}		
	
//	public float getDirection() 
//	{
//		return this.place.getDirection();
//	}

	@Override
    public float getDistance(){
		return this.place.getDistance();
	}

	@Override
    public void setPosition(v3 position){
		this.place.setPosition(position);
	}

	@Override
    public int getClock(){
		return this.clock;
	}

	@Override
    public float getOrientationAngle(){
		return this.place.getOrientationAngle();
	}
	
//	public int getValue(){
//		return this.aspect.getCode();
//	}

//	public void setOrientation(v3 orientation){
//		this.place.setOrientation(orientation);
//	}

//	public v3 getOrientation(){
//		return this.getOrientation();
//	}

	@Override
    public void incClock(){
		this.place.fade();
		this.clock++;
	}

	/**
	 * Act instances are equal if they have the same primitive and place and clock
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
			ActInstance other = (ActInstance)o;
			//ret  = (this.getPrimitive().equals(other.getPrimitive()) 
			ret  = (this.getDisplayLabel().equals(other.getDisplayLabel()) 
					//&& this.position.epsilonEquals(other.getPosition(), .1f)
					&& this.place.equals(other.getPlace())
					&& (this.clock == other.getClock()));
			//ret = isInCell(other.getPosition()) && other.getClock() == getClock() && other.getType() == getType();
		}		
		return ret;
	}

	@Override
    public boolean isInCell(v3 position){
		return this.place.isInCell(position);
	}
	
	@Override
    public void normalize(float scale) {
		this.place.normalize(scale);
	}
	
	@Override
    public Area getArea(){
		return this.place.getArea();
	}
	
	@Override
    public String getDisplayLabel(){
		return this.primitive.getLabel();
	}

	@Override
    public void trace(ITracer tracer, Object e) {
		
		Object p = tracer.addSubelement(e, "place");		
		tracer.addSubelement(p, "primitive", this.primitive.getLabel());
		tracer.addSubelement(p, "position", "(" + this.getPosition().x + ',' + this.getPosition().y + ')');
		tracer.addSubelement(p, "area", AreaImpl.createOrGet(this.getPosition()).getLabel());
		tracer.addSubelement(p, "modality", this.modality + "");
		tracer.addSubelement(p, "aspect", this.aspect.toString());
	}

	@Override
    public Aspect getAspect() {
		return aspect;
	}

	@Override
    public void setAspect(Aspect aspect) {
		this.aspect = aspect;
	}

	@Override
    public int getModality() {
		return modality;
	}

	@Override
    public void setModality(int modality) {
		this.modality = modality;
	}

	@Override
    public Place getPlace() {
		return this.place;
	}

	@Override
    public int getDisplayCode() {
		return this.aspect.getCode();
	}

	@Override
    public boolean isFocus() {
		return focus;
	}

	@Override
    public void setFocus(boolean focus) {
		this.focus = focus;
	}

}

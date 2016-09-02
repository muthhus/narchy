package ideal.vacuum.eca.construct.egomem;

import ideal.vacuum.eca.spas.Place;
import ideal.vacuum.eca.spas.PlaceImpl;
import ideal.vacuum.ernest.ErnestUtils;
import ideal.vacuum.tracing.ITracer;
import spacegraph.math.v3;
import spacegraph.phys.math.Transform;


/**
 * An instance of phenomenon known to be present in the surrounding environment
 * @author Olivier
 */
public class PhenomenonInstanceImpl implements PhenomenonInstance {

	private PhenomenonType phenomenonType;
	private Place place;
	private int clock;
	private boolean focus;
	
	/**
	 * @param phenomenonType The type of this phenomenon instance.
	 * @param position The position.
	 */
	public PhenomenonInstanceImpl(PhenomenonType phenomenonType, v3 position){
		this.phenomenonType = phenomenonType;
		place = new PlaceImpl(position);
	}
	
	/**
	 * Clone A phenomenon Instance
	 * @return The cloned phenomenon Instance
	 */
	@Override
    public PhenomenonInstanceImpl clone(){
		PhenomenonInstanceImpl clonePlace = null;
		try {
			clonePlace = (PhenomenonInstanceImpl) super.clone();
		} catch(CloneNotSupportedException cnse) {
			cnse.printStackTrace(System.err);
		}
		// We must clone the objects because they are passed by reference by default
		clonePlace.place = this.place.clone();
		return clonePlace;
	}

	@Override
    public PhenomenonType getPhenomenonType() {
		return this.phenomenonType;
	}

	@Override
    public Place getPlace() {
		return this.place;
	}
	
	@Override
    public void transform(Transform transform){
		this.place.transform(transform);
	}
	

	@Override
    public void setPhenomenonType(PhenomenonType phenomenonType) {
		this.phenomenonType = phenomenonType;
	}
	
	public String toString(){
		return ("Type " + this.phenomenonType.getLabel() + " in area " + place.getArea().getLabel()); 
	}

	@Override
    public Area getArea() {
		return this.place.getArea();
	}

	@Override
    public void setPosition(v3 position) {
		this.place.setPosition(position);
	}

	@Override
    public v3 getPosition() {
		return this.place.getPosition();
	}

	@Override
    public int getDisplayCode() {
		return this.phenomenonType.getAspect().getCode();
	}

	@Override
    public int getClock() {
		return this.clock;
	}

	@Override
    public String getDisplayLabel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
    public float getOrientationAngle() {
		return this.place.getOrientationAngle();
	}

	@Override
    public void incClock() {
		this.clock++;
	}

	@Override
    public boolean isInCell(v3 position) {
		return this.place.isInCell(position);
	}

	@Override
    public float getDistance()
	{
		return this.place.getDistance();
	}
	
	@Override
    public void trace(ITracer tracer, Object e) {
		
		Object pe = tracer.addSubelement(e, "phenomenon_instance");		
		this.phenomenonType.trace(tracer, pe);
		tracer.addSubelement(pe, "position", '(' + ErnestUtils.format(this.place.getPosition().x, 1) + ',' + ErnestUtils.format(this.place.getPosition().y, 1) + ')');
		tracer.addSubelement(pe, "area", this.place.getArea().getLabel());
	}

	@Override
    public void setClock(int clock) {
		this.clock=clock;
	}

	@Override
    public boolean isFocus() {
		return focus;
	}

	@Override
    public void setFocus(boolean focus) {
		this.focus = focus;
	}
	
	/**
	 * Phenomenon instances are equal if they have the same position and the same phenomenon type
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
			PhenomenonInstance other = (PhenomenonInstance)o;
//			ret  = (this.getDisplayLabel().equals(other.getDisplayLabel()) 
//					//&& this.position.epsilonEquals(other.getPosition(), .1f)
//					&& this.place.equals(other.getPlace())
//					&& (this.clock == other.getClock()));
			ret = isInCell(other.getPosition()) && this.phenomenonType.equals(other.getPhenomenonType()) ;
		}		
		return ret;
	}


}

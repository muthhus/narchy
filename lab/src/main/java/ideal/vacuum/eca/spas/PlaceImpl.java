package ideal.vacuum.eca.spas;

import ideal.vacuum.eca.construct.egomem.Area;
import ideal.vacuum.eca.construct.egomem.AreaImpl;
import ideal.vacuum.ernest.ErnestUtils;
import spacegraph.math.v3;
import spacegraph.phys.math.Transform;





/**
 * A place in egocentric spatial memory where an ActInstance or a PhenomenonInstance is located.
 * @author Olivier
 */
public class PlaceImpl implements Place {

	private v3 position = new v3();
	private v3 orientation = new v3(1,0,0);
	
	/**
	 * Create a new place 
	 * (The provided position is cloned so the place can be moved without changing the provided position).
	 * @param position This place's position.
	 */
	public PlaceImpl(v3 position){
		this.position.set(position);
	}
	
	/**
	 * Clone a place
	 * Warning: the bundle and act that this place contain are not cloned 
	 * @return The cloned place
	 */
	@Override
    public PlaceImpl clone(){
		PlaceImpl clonePlace = null;
		try {
			clonePlace = (PlaceImpl) super.clone();
		} catch(CloneNotSupportedException cnse) {
			cnse.printStackTrace(System.err);
		}

		// We must clone the objects because they are passed by reference by default
		clonePlace.setPosition(this.position);
		clonePlace.setOrientation(this.orientation);

		return clonePlace;
	}
	
	@Override
    public void transform(Transform transform) {
		transform.transform(this.position);
		transform.transform(this.orientation);
	}

	@Override
    public void setPosition(v3 position) {
		// Create a new instance of the vector so it can be used to clone this place.
		this.position = new v3(position);
	}

	@Override
    public v3 getPosition() {
		return this.position;
	}

	@Override
    public boolean isInCell(v3 position) {
		boolean ret;
		// Is in the same cell.
		ret = (Math.round(this.position.x) == Math.round(position.x)) && (Math.round(this.position.y) == Math.round(position.y)); 
		
		// Is in the same cell in egocentric polar referential.
		
		// Does not work for the cell behind !!
//		if (m_position.length() < .5f && position.length() < .5f)
//			ret = true;
//		else if (Math.round(ErnestUtils.polarAngle(m_position) / (float)Math.PI * 4) ==
// 			     Math.round(ErnestUtils.polarAngle(  position) / (float)Math.PI * 4) &&
// 			     (Math.round(m_position.length()) == Math.round(position.length())))
//			ret = true;
//		else 
//			ret = false;
		
		return ret;		
	}

	@Override
    public void setOrientation(v3 orientation) {
		this.orientation = new v3(orientation);
	}

	@Override
    public v3 getOrientation() {
		return this.orientation;
	}

	@Override
    public float getDirection() {
		return ErnestUtils.polarAngle(new v3(this.position));
	}

	@Override
    public float getDistance() {
		return this.position.length();
	}

	@Override
    public float getOrientationAngle() {
		return ErnestUtils.polarAngle(this.orientation);
	}

	@Override
    public void normalize(float scale) {
		float d = this.position.length();
		if (d > 0) this.position.scale(scale / d);
	}

	@Override
    public Area getArea() {
		return AreaImpl.createOrGet(position);
	}
	
	/**
	 * Places are equal if they are in the same position
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
			Place other = (Place)o;
			ret  = this.position.epsilonEquals(other.getPosition(), .1f);
		}		
		return ret;
	}

	@Override
    public void fade() {
		this.setPosition(new v3(this.position.x * 1.1f, this.position.y * 1.1f, 0f));
	}

}

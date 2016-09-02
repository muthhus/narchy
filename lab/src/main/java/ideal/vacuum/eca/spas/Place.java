package ideal.vacuum.eca.spas;

import ideal.vacuum.eca.construct.egomem.Area;
import spacegraph.math.v3;
import spacegraph.phys.math.Transform;


/**
 * A place in egocentric spatial memory where an ActInstance or a PhenomenonInstance is located.
 * @author Olivier
 */
public interface Place extends Cloneable{
	
	/**
	 * @param transform The transformation applied to spatial memory.
	 */
    void transform(Transform transform);

	/**
	 * @return A clone of this place
	 */
    PlaceImpl clone();
	
	/**
	 * @param position The place's position.
	 */
    void setPosition(v3 position);
	
	/**
	 * @return The location's position.
	 */
    v3 getPosition();
	
	/**
	 * Test if this place is at this position.
	 * @param position The position to test
	 * @return true if this place is in the same cell as thi position.
	 */
    boolean isInCell(v3 position);
	
	/**
	 * @param orientation This place's orientation.
	 */
    void setOrientation(v3 orientation);
	
	/**
	 * @return This place's orientation.
	 */
    v3 getOrientation();
	
	/**
	 * @return The direction of this place.
	 */
    float getDirection();
	
	/**
	 * @return The place's distance.
	 */
    float getDistance();
	
	/**
	 * @return The orientation angle of this place
	 */
    float getOrientationAngle();
	
	/**
	 * @param scale The unity value for normalization.
	 */
    void normalize(float scale);
	
	/**
	 * @return The are of this place
	 */
    Area getArea();
	
	void fade();
	
}

package ideal.vacuum.eca.spas;

import ideal.vacuum.eca.construct.egomem.Area;
import spacegraph.math.v3;
import spacegraph.phys.math.Transform;


/**
 * An object that can be placed and tracked in spatial memory
 * @author Olivier
 */
public interface Placeable extends Cloneable {

	/**
	 * @return A clone of this Placeable
	 */
    Placeable clone();
	
	/**
	 * @param position The new position of this object.
	 */
    void setPosition(v3 position);
	
	/**
	 * @return The place of this object.
	 */
    Place getPlace();
	
	/**
	 * @param transform The transformation to move this object in spatial memory.
	 */
    void transform(Transform transform);

	/**
	 * @return The area of this object
	 */
    Area getArea();
	
	/**
	 * @param position
	 * @return true if this placeable is in the cell designated by this position.
	 */
    boolean isInCell(v3 position);
	
	v3 getPosition();
	
	int getDisplayCode();
	
	int getClock();
	
	String getDisplayLabel();
	
	float getOrientationAngle();
	
	void incClock();
	
	/**
	 * @return The place's distance.
	 */
    float getDistance();
	
	void setFocus(boolean focus);
	boolean isFocus();

}

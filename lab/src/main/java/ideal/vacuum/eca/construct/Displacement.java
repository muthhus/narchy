package ideal.vacuum.eca.construct;

import ideal.vacuum.eca.construct.egomem.Area;
import spacegraph.phys.math.Transform;


/**
 * A displacement in spatial memory
 * @author Olivier
 */
public interface Displacement {

	/**
	 * @return The displacement's label.
	 */
    String getLabel();
	
	/**
	 * @return The 3D transformation
	 */
    Transform getTransform();
	
	void setTransform(Transform t);

	Area getPreArea();

	Area getPostArea();

}

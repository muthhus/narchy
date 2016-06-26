package nars.gui.graph;

import com.jogamp.opengl.GL2;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import java.util.List;

/**
 * (fractal) 2D Surface embedded relative to a parent 2D surface or 3D space
 */
public class Surface {

    Vector3f translateLocal;
    Vector2f sizeLocal;

    public Surface parent;
    public List<Surface> children;

    public Surface() {

    }

    public void setParent(Surface s) {
        parent = s;
    }

    /** returns true if the event has been absorbed, false if it should continue propagating */
    public boolean onTouch(float x, float y, short[] buttons) {
        return false;
    }

    /** returns true if the event has been absorbed, false if it should continue propagating */
    public boolean onKey(float x, float y, char charCode) {
        return false;
    }

    public void paint(GL2 gl) {

    }

    public final void render(GL2 gl) {
        paint(gl);

        List<Surface> c = this.children;
        if (c !=null) {
            for (int i = 0, childrenSize = c.size(); i < childrenSize; i++) {
                c.get(i).render(gl);
            }
        }
    }



}

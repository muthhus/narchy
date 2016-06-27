package nars.gui.graph.matter;

import bulletphys.ui.ShapeDrawer;
import com.jogamp.opengl.GL2;
import nars.gui.graph.Surface;

import javax.vecmath.Vector2f;

/**
 * Created by me on 6/26/16.
 */
public class XYPadSurface extends Surface {

    final Vector2f knob = new Vector2f(0.5f, 0.5f);

    public XYPadSurface() {
        super();
    }

    @Override
    protected boolean onTouching(Vector2f hitPoint, short[] buttons) {
        if (leftButton(buttons)) {
            knob.set(hitPoint);
            return true;
        }
        return false;
    }


    @Override
    protected void paint(GL2 gl) {

        //float margin = 0.1f;
        //float mh = margin / 2.0f;

        float px = knob.x;
        float py = knob.y;

        float W = 0.1f;
        float H = 0.1f;


        gl.glColor3f(0.8f, 0.4f, 0f);
        ShapeDrawer.rect(gl, 0, py - H / 2f, 1, H); //horiz
        ShapeDrawer.rect(gl, px - W / 2f, 0, W, 1); //vert

    }

}

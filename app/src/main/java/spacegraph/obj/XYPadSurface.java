package spacegraph.obj;

import com.jogamp.opengl.GL2;
import spacegraph.Surface;
import spacegraph.render.ShapeDrawer;

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


        gl.glColor3f(0f, 0.2f, 0.8f);
        float h1 = py - H / 2f;
        ShapeDrawer.rect(gl, 0, h1, 1, H); //horiz
        float w1 = px - W / 2f;
        ShapeDrawer.rect(gl, w1, 0, W, 1); //vert

        gl.glColor3f(0f, 0.4f, 0.9f);
        ShapeDrawer.rect(gl, w1, h1, W, H, 0.25f); //knob
    }

}

package spacegraph.obj;

import com.jogamp.opengl.GL2;
import spacegraph.Surface;
import spacegraph.render.Draw;

import javax.vecmath.Vector2f;

/**
 * Created by me on 6/26/16.
 */
public class XYSlider extends Surface {

    final Vector2f knob = new Vector2f(0.5f, 0.5f);
    private float knobSize = 0.25f;

    public XYSlider() {
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

        float knobSize = this.knobSize;
        float W = knobSize / 3f;
        float H = knobSize / 3f;

        gl.glColor4f(0f, 0.2f, 0.8f, 0.75f);
        float h1 = py - H / 2f;
        Draw.rect(gl, 0, h1, 1, H); //horiz
        float w1 = px - W / 2f;
        Draw.rect(gl, w1, 0, W, 1); //vert


        gl.glColor4f(0.2f, 0.8f, 0f, 0.75f);
        Draw.rect(gl, w1-knobSize/2f, h1-knobSize/2f, knobSize, knobSize, 0.25f); //knob
    }

}

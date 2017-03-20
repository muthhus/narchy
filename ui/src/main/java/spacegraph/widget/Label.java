package spacegraph.widget;

import com.jogamp.opengl.GL2;
import spacegraph.Surface;
import spacegraph.render.Draw;

/**
 * Created by me on 7/29/16.
 */
public class Label extends Surface {

    private String value = "";
    float fontScale;

    public Label() {
        this("");
    }

    public Label(String s) {
        super();
        set(s);
    }

    @Override
    public void paint(GL2 gl) {
        gl.glColor4f(1f,1f,1f,1f); //TODO color params
        gl.glLineWidth(1.5f);
        float dz = 0.1f;
        Draw.text(gl, value(), fontScale, 0.75f, 0f, 0f, dz, Draw.TextAlignment.Left);
    }

    public void set(String newValue) {
        if (newValue == null)
            newValue = "(null)";
        this.value = newValue;
        int len = newValue.length();
        this.fontScale =
                1f / (len);
                //0.5f;
                //0.5f;
        align(Align.Center, 1.25f / len);
                //0.5f; //(1f/ConsoleSurface.fontWidth)/value.length();
//        if (len > 0) {
//            this.aspect = 0.5f; ///0.5f / len;
//        } else {
//            this.aspect = Float.NaN;
//        }
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return "Label[" + value + ']';
    }
}

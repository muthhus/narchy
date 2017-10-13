package spacegraph.widget;

import com.jogamp.opengl.GL2;
import spacegraph.Surface;
import spacegraph.math.Color4f;
import spacegraph.render.Draw;

/**
 * Created by me on 7/29/16.
 */
public class Label extends Surface {

    private String value = "(null)";

    public float fontScale = 1f;
    public final Color4f color = new Color4f(1f,1f,1f,1f);
    public float lineWidth = 3f;

    public Label() {
        this("");
    }

    public Label(String s) {
        super();
        //align(Align.Center);
        set(s);
    }

    @Override
    public void paint(GL2 gl) {

        color.apply(gl);
        gl.glLineWidth(lineWidth);
        //float dz = 0.1f;
        Draw.text(gl, value(), fontScale, 1, 0f, 0f, 0, Draw.TextAlignment.Left);

    }

    public void set(String newValue) {

       if (this.value==newValue)
            return;

        this.value = newValue;

        int len = newValue.length();
        this.aspect = 1.6f / (len);
        this.fontScale = 1f / len;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return "Label[" + value + ']';
    }
}

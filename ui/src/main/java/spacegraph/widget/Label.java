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

    public float fontScaleX = 1f, fontScaleY = 1f;
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
        Draw.text(gl, value(), fontScaleX, fontScaleY, 0, 0.5f-fontScaleY/2f, 0, Draw.TextAlignment.Left);

    }

    public void set(String newValue) {

       if (this.value==newValue)
            return;

        this.value = newValue;

        int len = newValue.length();
        float ratio = 1.5f;
        float fontScaleX = 1f / len;
        float fontScaleY = ratio * fontScaleX;
        if (fontScaleX > fontScaleY) {
            //wider than tall, limit by width
//            fontScaleY =
        } else {
            //taller than wide, limit by height
            fontScaleY = 1f;
            fontScaleX = 1f / (ratio*len);
        }
        this.fontScaleX = fontScaleX;
        this.fontScaleY = fontScaleY;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return "Label[" + value + ']';
    }


}

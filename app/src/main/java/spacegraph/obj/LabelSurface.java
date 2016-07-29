package spacegraph.obj;

import com.jogamp.opengl.GL2;
import spacegraph.Surface;
import spacegraph.render.Draw;

/**
 * Created by me on 7/29/16.
 */
public class LabelSurface extends Surface {

    private String value = "";
    float fontScale;

    public LabelSurface(String s) {
        super();
        set(s);
    }

    @Override
    public void paint(GL2 gl) {
        float dz = 0.1f;
        Draw.renderLabel(gl, fontScale, fontScale, value(), scaleLocal.x/2f, scaleLocal.y/2f, dz);
    }

    public void set(String newValue) {
        if (newValue == null)
            newValue = "(null)";
        this.value = newValue;
        this.fontScale = (1f/ConsoleSurface.fontWidth)/value.length();

    }

    public String value() {
        return value;
    }
}

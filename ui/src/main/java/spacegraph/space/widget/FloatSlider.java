package spacegraph.space.widget;

import com.jogamp.opengl.GL2;
import jcog.Texts;
import jcog.Util;
import jcog.data.FloatParam;
import spacegraph.render.Draw;

/**
 * Created by me on 11/18/16.
 */
public class FloatSlider extends BaseSlider {

    private final float max;
    private final float min;
    String label;

    public FloatSlider(float v, float min, float max) {
        super((v - min) / (max - min));
        this.min = min;
        this.max = max;
    }

    public FloatSlider(String label, float v, float min, float max) {
        this(v, min, max);
        this.label = label;
    }

    public FloatSlider(FloatParam f) {
        this(f.floatValue(), f.min, f.max);
        on((s,v)-> f.setValue(v));
    }

    public spacegraph.space.widget.FloatSlider label(String label) {
        this.label = label;
        return this;
    }

    @Override
    protected void paint(GL2 gl) {
        super.paint(gl);

        gl.glLineWidth(1f);
        gl.glColor3f(1, 1, 1);
        String label = this.label;
        if (label == null)
            label = Texts.n2(value());
        Draw.text(gl, label, 0.5f / label.length(), 0.5f, 0.5f, 0);

    }

    @Override
    protected float p(float v) {
        return (Util.clamp(v, min, max) - min) / (max - min);
    }

    @Override
    protected float v(float p) {
        return Util.clamp(p, 0, 1f) * (max - min) + min;
    }
}

package spacegraph.widget.slider;

import com.jogamp.opengl.GL2;
import jcog.Texts;
import jcog.Util;
import jcog.data.FloatParam;
import jcog.math.FloatSupplier;
import spacegraph.render.Draw;
import spacegraph.widget.Label;

/**
 * Created by me on 11/18/16.
 */
public class FloatSlider extends BaseSlider {

    private final float max;
    private final float min;
    final Label label;
    public FloatSupplier input;

    public FloatSlider(float v, float min, float max) {
        super((v - min) / (max - min));


        label = new Label();
        set(label);

        this.min = min;
        this.max = max;
    }

    public FloatSlider(String label, float v, float min, float max) {
        this(v, min, max);
        this.label.set(label);
    }

    public FloatSlider(FloatParam f) {
        this(f.floatValue(), f.min, f.max);
        on((s,v)-> f.setValue(v));
        input = f;
    }

    public FloatSlider label(String label) {
        this.label.set(label);
        return this;
    }

    @Override
    protected void paint(GL2 gl) {

        super.paint(gl);

        if (input!=null)
            value(input.asFloat());

//        gl.glLineWidth(1f);
//        gl.glColor3f(1, 1, 1);
        this.label.set(labelText());
//        Draw.text(gl, label, 0.5f / label.length(), 0.5f, 0.5f, 0);

    }

    public String labelText() {
        return Texts.n2(value());
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

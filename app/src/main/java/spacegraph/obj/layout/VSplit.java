package spacegraph.obj.layout;

import spacegraph.Surface;
import spacegraph.obj.layout.Layout;

/**
 * Splits a surface into a top and bottom vertical column
 */
public class VSplit extends Layout {

    public float proportion; //0.5f = middle, 0.0 = all top, 1.0 = all bottom

    public VSplit(Surface top, Surface bottom) {
        this(top, bottom, 0.5f);
    }

    public VSplit(Surface top, Surface bottom, float proportion) {
        super(top, bottom);
        this.proportion = proportion;
    }


    @Override
    protected void layout() {
        float margin = 0.0f;
        //float content = 1f - margin;
        float x = margin/2f;

        Surface top = top();
        top.scaleLocal.set(1f, proportion);
        top.translateLocal.set(x, 1f - proportion, 0);

        Surface bottom = bottom();
        bottom.scaleLocal.set(1f, 1f-proportion);
        bottom.translateLocal.set(x, proportion, 0);

    }

    public final Surface top() {
        return children.get(0);
    }
    public final Surface bottom() {
        return children.get(1);
    }

}


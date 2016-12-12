package spacegraph.space.layout;

import spacegraph.Surface;

/**
 * Splits a surface into a top and bottom vertical column
 */
public class VSplit extends Layout {

    public float split; //0.5f = middle, 0.0 = all top, 1.0 = all bottom

    public VSplit() {
        this(null, null);
    }

    public VSplit(Surface top, Surface bottom) {
        this(top, bottom, 0.5f);
    }

    public VSplit(Surface top, Surface bottom, float split) {
        super(null,null);
        set(top, bottom, split);
    }

    public void set(Surface top, Surface bottom, float split) {
        this.split = split;
        top(top);
        bottom(bottom);
        layout();
    }

    @Override
    public void layout() {

        float margin = 0.0f;
        //float content = 1f - margin;
        float x = margin / 2f;

        Surface top = top();
        if (top != null) {
            top.scale(1f, split);
            top.translateLocal.set(x,  1f - (split), 0);
        }

        Surface bottom = bottom();
        if (bottom!=null) {
            bottom.scale(1f, 1f - split);
            bottom.translateLocal.set(x, -split /2f, 0);
        }

    }

    public final void top(Surface s) { children.set(0, s); }
    public final void bottom(Surface s) { children.set(1, s); }

    public final Surface top() {
        return children.get(0);
    }
    public final Surface bottom() {
        return children.get(1);
    }

}


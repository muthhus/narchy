package spacegraph.layout;

import spacegraph.Surface;

/**
 * Splits a surface into a top and bottom vertical column
 */
public class VSplit<X extends Surface, Y extends Surface> extends Layout {

    public float split; //0.5f = middle, 0.0 = all top, 1.0 = all bottom

    public VSplit() {
        this(null, null);
    }

    public VSplit(X top, Y bottom) {
        this(top, bottom, 0.5f);
    }

    public VSplit(X top, Y bottom, float split) {
        super(null,null);
        set(top, bottom, split);
    }


    public void set(X top, Y bottom, float split) {
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
            top.pos.set(x,  1f - (split), 0);
        }

        Surface bottom = bottom();
        if (bottom!=null) {
            bottom.scale(1f, 1f - split);
            bottom.pos.set(x, 0, 0);
        }

    }

    public final void top(X s) { children.set(0, s); }
    public final void bottom(Y s) { children.set(1, s); }

    public final X top() {
        return (X) children.get(0);
    }
    public final Y bottom() {
        return (Y) children.get(1);
    }

}


package spacegraph;

import org.jetbrains.annotations.Nullable;
import spacegraph.layout.Layout;

import java.util.function.Consumer;

import static spacegraph.Surface.Align.Center;

public class AspectAlign extends Layout {

    /**
     * not used unless aspect ratio is set to non-NaN value
     */
    protected Align align;

    /**
     * height/width target aspect ratio; if aspect is NaN, no adjustment applied
     */
    protected float aspect;

    public final Surface the;

    public AspectAlign(Surface the) {
        this(the, Center, 1f);
    }

    public AspectAlign(Surface the, Align a, float w, float h) {
        this(the, a, h / w);
    }

    public AspectAlign(Surface the, Align a, float aspect) {
        this.the = the;
        this.aspect = aspect;
        this.align = a;
    }

    @Override
    public void start(@Nullable Surface parent) {
        super.start(parent);
        the.start(this);
    }

    @Override
    public void stop() {
        the.stop();
        super.stop();
    }

    @Override
    protected void doLayout() {
        //        v2 scale = this.scale;
//
//        float sx, sy;

        float vw = w(); //TODO factorin scale
        float vh = h();
        float tw = vw;
        float th = vh;
        float aspect = this.aspect;
        if (aspect == aspect /* not NaN */) {
//
            if (vh / vw > aspect) {
                //wider, shrink y
                tw = vw;
                th = vw * aspect;
            } else {
                //taller, shrink x
                tw = vh * aspect;
                th = vh;
            }
        }

        float tx = 0, ty = 0;
        switch (align) {

            //TODO others

            case Center:
                //HACK TODO figure this out
                tx += (vw - tw) / 2f;
                ty += (vh - th) / 2f;
                break;

            case None:
            default:
                break;

        }

        the.pos(tx, ty, tx+tw, ty+th);
    }

    public spacegraph.AspectAlign align(Align align) {
        this.align = align;
        return this;
    }

    public spacegraph.AspectAlign align(Align align, float aspect) {
        this.aspect = aspect;
        return align(align);
    }

    public spacegraph.AspectAlign align(Align align, float width, float height) {
        return align(align, height / width);
    }

    @Override
    public void forEach(Consumer<Surface> o) {
        o.accept(the);
    }
}

package spacegraph.input;

import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.Surface;
import spacegraph.widget.Windo;

/**
 * resizes a rectangular surface in one of the four cardinal or four diagonal directions
 */
public class FingerResize extends FingerDragging {

    protected final static float aspectRatioRatioLimit = 0.1f;

    private final Surface resized;
    private final Windo.WindowDragging mode;

    private RectFloat2D before;

    public FingerResize(Surface target, Windo.WindowDragging mode) {
        super(0);
        this.resized = target;
        this.mode = mode;
    }

    @Override
    public void start(Finger f) {
        this.before = resized.bounds;
    }

    @Override
    public boolean drag(Finger finger) {

        float fx = finger.hit.x;
        float fy = finger.hit.y;

        switch (mode) {
            case RESIZE_N: {
                float pmy = before.min.y;
                float bh = before.h();
                float ty = (fy - finger.hitOnDown[0].y);
                resized.pos(before.min.x, pmy, before.max.x, Math.max(pmy + aspectRatioRatioLimit * bh, bh + pmy + ty));
            }
            break;
            case RESIZE_NE: {
                float pmx = before.min.x;
                float pmy = before.min.y;
                float bw = before.w();
                float bh = before.h();
                float tx = (fx - finger.hitOnDown[0].x);
                float ty = (fy - finger.hitOnDown[0].y);
                resized.pos(pmx, pmy, Math.max(pmx + aspectRatioRatioLimit * bw, bw + pmx + tx), Math.max(pmy + aspectRatioRatioLimit * bh, bh + pmy + ty));
            }
            break;

            case RESIZE_SW: {
                float pmx = before.max.x;
                float pmy = before.max.y;
                float bw = before.w();
                float bh = before.h();
                float tx = (fx - finger.hitOnDown[0].x);
                float ty = (fy - finger.hitOnDown[0].y);
                resized.pos(pmx - bw + tx, pmy - bh + ty, pmx, pmy); //TODO limit aspect ratio change
            }
            break;
        }

        return true;
    }
}

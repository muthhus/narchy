package spacegraph.input;

import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.Surface;

public class FingerMove extends FingerDragging {
    private final Surface moving;
    private RectFloat2D before;

    public FingerMove(Surface moving) {
        super(0 /* LEFT BUTTON */);
        this.moving = moving;
    }

    @Override
    public void start(Finger f) {
        this.before = moving.bounds;
    }

    @Override
    public boolean drag(Finger finger) {
        float pmx = before.min.x;
        float pmy = before.min.y;
        float tx = pmx + (finger.hit.x - finger.hitOnDown[0].x);
        float ty = pmy + (finger.hit.y - finger.hitOnDown[0].y);
        moving.pos(tx, ty, moving.w() + tx, moving.h() + ty);
        return true;
    }
}

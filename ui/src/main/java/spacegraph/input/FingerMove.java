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

    @Override public void start(Finger f) {
        this.before = moving.bounds;
    }

    @Override public boolean drag(Finger finger) {
        float pmx = before.min.x;
        float pmy = before.min.y;
        float tx = pmx + (moveX() ? (finger.hit.x - finger.hitOnDown[0].x) : 0);
        float ty = pmy + (moveY() ? (finger.hit.y - finger.hitOnDown[0].y) : 0);
        moved(tx, ty, moving.w() + tx, moving.h() + ty);
        return true;
    }

    protected void moved(float x1, float y1, float x2, float y2) {
        moving.pos(x1, y1, x2, y2);
    }

    /** allow movement in x-axis dirctions */
    public boolean moveX() { return true; }

    /** allow movement in y-axis dirctions */
    public boolean moveY() { return true; }
}

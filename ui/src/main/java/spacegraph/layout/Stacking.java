package spacegraph.layout;

import spacegraph.Surface;

/**
 * TODO
 */
public class Stacking extends Layout {

    public Stacking(Surface... children) {
        super(children);
//        clipTouchBounds = false;
    }

    @Override
    public void layout() {
        children.forEach((c) ->
            c.pos(0, 0, w(), h()));
        super.layout();
    }
}

package spacegraph.layout;

import spacegraph.Surface;

/**
 * TODO
 */
public class Stacking extends MutableLayout {

    public Stacking(Surface... children) {
        super(children);
//        clipTouchBounds = false;
    }

    @Override
    public void doLayout() {
        children.forEach((c) -> c.pos(bounds));
        super.doLayout();
    }

}

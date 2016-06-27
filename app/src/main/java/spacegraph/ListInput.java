package spacegraph;

import nars.Global;

/**
 * Created by me on 6/26/16.
 */
public class ListInput<X,Y extends Spatial<X>> extends SpaceInput<X,Y> {

    private final X[] items;

    public ListInput(X... xx) {
        super();
        this.items = xx;

    }

    @Override
    public void start(SpaceGraph space) {
        super.start(space);
        refresh();
    }

    @Override
    protected void updateImpl() {
//        if (items.length!=visible.size()) {
//            refresh();
//        }
    }

    private void refresh() {
        int n = 0;
        this.visible = Global.newArrayList(items.length);
        for (X x : items) {
            visible.add((Y) space.update(n++, x));
        }
    }

    @Override
    public float now() {
        return 0;
    }
}

package nars.gui.graph;

import nars.Global;

/**
 * Created by me on 6/26/16.
 */
public class FixedAtomatterList<X,Y extends Atomatter<X>> extends GraphInput<X,Y> {

    private final X[] items;

    public FixedAtomatterList(X... xx) {
        super();
        this.items = xx;

    }

    @Override
    public void start(GraphSpace grapher) {
        super.start(grapher);
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
            visible.add((Y) grapher.update(n++, x));
        }
    }

    @Override
    public float now() {
        return 0;
    }
}

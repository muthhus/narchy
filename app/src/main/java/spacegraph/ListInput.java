package spacegraph;

import nars.$;

/**
 * Created by me on 6/26/16.
 */
public class ListInput<X,Y extends Spatial<X>> extends SpaceInput<X,Y> {

    private X[] items;

    public ListInput(X... xx) {
        super();
        this.items = xx;
    }

    public void commit(X[] xx) {
        this.items = xx;
        if (space!=null)
            refresh();
    }

    @Override
    public void start(SpaceGraph space) {
        super.start(space);
        commit(items);
    }

    @Override
    protected void updateImpl() {

    }

    private void refresh() {
        int n = 0;
        this.visible = $.newArrayList(items.length);
        for (X x : items) {
            visible.add((Y) space.update(n++, x));
        }
    }

    @Override
    public float now() {
        return 0;
    }
}

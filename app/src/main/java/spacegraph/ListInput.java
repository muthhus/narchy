package spacegraph;

import nars.$;
import nars.util.data.list.FasterList;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by me on 6/26/16.
 */
public class ListInput<X,Y extends Spatial<X>> extends SpaceInput<X,Y> {

    protected List<Y> active = new FasterList<>(0);

    private X[] items;

    public ListInput(X... xx) {
        super();
        this.items = xx;
    }

    @Override
    public final Iterator<Y> iterator() {
        return active.iterator();
    }

    @Override
    public final void forEach(Consumer<? super Y> action) {
        active.forEach(action);
    }

    public void commit(X... xx) {
        this.items = xx;
        if (space!=null)
            refresh();
    }

    @Override
    public void start(SpaceGraph space) {
        super.start(space);
        commit(items);
    }

    /**
     * rewinds the buffer of visible items, when collecting a new batch
     */
    public <Y extends Spatial> List<Y> rewind(int capacity) {
        active.forEach(Spatial::unpreactivate);
        return new FasterList<>(capacity);
    }

    @Override
    protected void updateImpl() {

    }

    private void refresh() {
        int n = 0;
        this.active = $.newArrayList(items.length);
        for (X x : items) {
            active.add((Y) space.update(x));
        }
    }

    @Override
    public int size() {
        return active.size();
    }


    @Override
    public long now() {
        return 0;
    }

    @Override
    public void update(SpaceGraph<? extends X> space) {
        active.forEach(a -> a.update(space));
    }

}

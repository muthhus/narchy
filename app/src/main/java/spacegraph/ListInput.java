package spacegraph;

import com.google.common.collect.Lists;
import nars.$;
import nars.util.data.list.FasterList;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by me on 6/26/16.
 */
public class ListInput<X,Y extends Spatial<X>> extends SpaceInput<X,Y> {

    protected List<Y> active = new FasterList<>(0);

    public ListInput() {
        super();
    }

    public ListInput(Function<X,Y> materialize, X... xx) {
        this();
        set(materialize, xx);
    }

    @Override
    public final Iterator<Y> iterator() {
        return active.iterator();
    }

    @Override
    public final void forEach(Consumer<? super Y> action) {
        active.forEach(action);
    }

    @Override
    public void start(SpaceGraph space) {
        super.start(space);
    }

    /**
     * rewinds the buffer of visible items, when collecting a new batch
     */
    public <Z extends Spatial> List<Z> rewind(int capacity) {
        active.forEach(Spatial::unpreactivate);
        return new FasterList<>(capacity);
    }

    public void set(Function<X, Y> materializer, X... items) {
        set(Lists.newArrayList(items), materializer);
    }

    public void set(List<X> items, Function<X, Y> materializer) {
        int n = 0;
        FasterList<Y> v = $.newArrayList(items.size());
        for (X x : items) {
            v.add(space.update(x, materializer));
        }
        this.active = v;
    }

    @Override
    public final int size() {
        return active.size();
    }

    @Override
    public final Y get(int i) {
        return active.get(i);
    }

    @Override
    public long now() {
        return 0;
    }


}

package spacegraph;

import com.google.common.collect.Lists;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import nars.$;
import nars.util.data.list.FasterList;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by me on 6/26/16.
 */
public class ListSpace<X,Y extends Spatial<X>> extends AbstractSpace<X,Y> {

    protected FasterList<Y> active = new FasterList<>(0);

    public ListSpace() {
        super();
    }

    public ListSpace(Y... xx) {
        this();
        Collections.addAll(active, xx);
    }

    public ListSpace(Function<X,Y> materialize, X... xx) {
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
    public int forEachIntSpatial(int offset, IntObjectPredicate<Spatial<X>> each) {
        return active.forEachIntSpatial(offset ,each);
    }

    @Override
    public long now() {
        return 0;
    }


}

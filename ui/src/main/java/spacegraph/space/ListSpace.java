package spacegraph.space;

import jcog.list.FasterList;
import spacegraph.AbstractSpace;

import java.util.List;
import java.util.function.Consumer;


public class ListSpace<X,Y> extends AbstractSpace<X,Y> {

    public List<Y> active;

    public ListSpace(Y... xx) {
        super();
        this.active = new FasterList<>(xx);
    }

    @Override
    public final void forEach(Consumer<? super Y> action) {
        active.forEach(action);
    }


    @Override
    public final int size() {
        return active.size();
    }

}

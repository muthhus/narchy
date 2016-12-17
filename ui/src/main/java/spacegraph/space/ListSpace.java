package spacegraph.space;

import jcog.list.FasterList;
import org.eclipse.collections.api.block.procedure.primitive.IntObjectProcedure;
import spacegraph.AbstractSpace;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;


public class ListSpace<X,Y> extends AbstractSpace<X,Y> {

    public Collection<Y> active = Collections.emptyList();

    public ListSpace() {
        super();
    }

    public ListSpace(Collection<Y> xx) {
        this();
        active = xx;
    }
    public ListSpace(Y... xx) {
        this();
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


    @Override
    public int forEachWithInt(int offset, IntObjectProcedure<Y> each) {
        //return active.forEach(offset ,each);
        int[] o = { offset };
        active.forEach(x -> each.value(o[0]++, x));
        return o[0];
    }

    @Override
    public long now() {
        return 0;
    }


}
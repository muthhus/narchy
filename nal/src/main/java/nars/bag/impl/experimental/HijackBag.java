package nars.bag.impl.experimental;

import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.budget.merge.BudgetMerge;
import nars.link.BLink;
import nars.util.data.map.nbhm.HijaCache;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by me on 9/4/16.
 */
public class HijackBag<X> extends HijaCache<X,BLink<X>> implements Bag<X> {


    public HijackBag(int capacity) {
        this(capacity, 1);
    }

    public HijackBag(int capacity, int reprobes) {
        super(capacity, reprobes);
    }


    @Override
    public void put(X i, Budgeted b, float scale, MutableFloat overflowing) {

        merge(i, (BLink<X>) newLink(i, b).priMult(scale), (prev, next) -> {
            //warning due to lossy overwriting, the key of next may not be equal to the key of prev
            if (prev.get().equals(next.get())) {
                BudgetMerge.plusBlend.apply(prev, next);
                return prev;
            } else {
                return next; //overwrite
            }
        });
    }


    @Override
    public void topWhile(Predicate<? super BLink<X>> each, int n) {
        throw new UnsupportedOperationException("yet");
    }

    @NotNull
    @Override
    public Bag<X> sample(int n, Predicate<? super BLink<X>> target) {
        //return this;
        throw new UnsupportedOperationException("yet");
    }

    @NotNull
    @Override
    public Iterator<BLink<X>> iterator() {
        return values().iterator();
    }

    @NotNull
    @Override
    public Bag<X> commit() {
        return commit(null);
    }
    @NotNull
    @Override
    public Bag<X> commit(Consumer<BLink> each) {
        if (!isEmpty()) {

            Iterator<Entry<X, BLink<X>>> es = entrySet().iterator();
            while (es.hasNext()) {
                Entry<X, BLink<X>> e = es.next();
                BLink<X> l = e.getValue();

                boolean delete = false;
                if (l.isDeleted())
                    delete = true;
                X x = l.get();
                if (x == null)
                    delete = true;

                if (delete) {
                    es.remove();
                } else {
                    if (each!=null)
                        each.accept(l);
                }
            }
        }

        return this;
    }

    @Override
    public boolean setCapacity(int c) {
        throw new UnsupportedOperationException();
    }



    @Override
    public X boost(Object key, float boost) {
        throw new UnsupportedOperationException("yet");
    }
}

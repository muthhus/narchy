package nars.bag.impl.experimental;

import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.budget.Forget;
import nars.budget.merge.BudgetMerge;
import nars.link.BLink;
import nars.util.data.map.nbhm.HijaCache;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by me on 9/4/16.
 */
public class HijackBag<X> extends HijaCache<X,BLink<X>> implements Bag<X> {


    public static final float finalTolerance = 0.25f;

    /** max # of times allowed to scan through until either the next item is
     *  accepted with final tolerance or gives up
     */
    private static final float SCAN_ITERATIONS = 1.5f;

    private float pending = 0;



    public HijackBag(int capacity, int reprobes) {
        super(capacity, reprobes);
    }


    @Override
    public void put(X x, Budgeted b, float scale, MutableFloat overflowing) {

        BLink next = (BLink) newLink(x, b).priMult(scale);



        BLink prev = get(x);
        if (prev!=null) {
            float pBefore = prev.pri();
            BudgetMerge.plusBlend.apply(prev, next);
            pending += prev.pri() - pBefore;
        } else {
            BLink displaced = put(x, next);
            if (displaced == null) {
                pending += next.pri();
            } else {
                //System.out.println("displaced: " + displaced + " vs " + next);
                //                float n = next.pri();
//                float probNext = n / (n + prev.pri());
//                if (rng.nextFloat() < probNext)
//                    return next; //overwrite
//                else
//                    return prev; //keep old value
            }
        }

//        BLink result = merge(i, l, (prev, next) -> {
//            //warning due to lossy overwriting, the key of next may not be equal to the key of prev
//            if (prev.get().equals(next.get())) {
//                //float pBefore = prev.pri();
//                BudgetMerge.plusBlend.apply(prev, next);
//                return prev;
//            } else {
//                float n = next.pri();
//                float probNext = n / (n + prev.pri());
//                if (rng.nextFloat() < probNext)
//                    return next; //overwrite
//                else
//                    return prev; //keep old value
//            }
//        });
    }


    @Override
    public void topWhile(Predicate<? super BLink<X>> each, int n) {
        throw new UnsupportedOperationException("yet");
    }

    @NotNull
    @Override
    public Bag<X> sample(int n, Predicate<? super BLink<X>> target) {
        Object[] data = _kvs;
        int c = (data.length - 2)/2;
        int jLimit = (int)Math.ceil(c * SCAN_ITERATIONS);
        float dBeam = 1f/c * finalTolerance; //(float)Math.sqrt(c);
        float beam = 0;

        int start = rng.nextInt(c); //starting index
        int i = start, j = 0;
        float r = rng.nextFloat(); //randomized threshold

        while ((n > 0) && (j < jLimit) /* prevent infinite looping */) {
            Object v = data[ ((i++) % c)*2 + 3 ]; /* capacity may change during the loop so always get the latest value */;

            if (v != null) {
                BLink<X> x = (BLink<X>) v;
                float p = x.priIfFiniteElseNeg1();
                if (p >= 0) {
                    if (r < p + beam ) {
                        if (target.test(x)) {
                            n--;
                            r = rng.nextFloat();
                        }
                    }
                }
                beam += dBeam; //widen the search beam to make it more likely to accept something
            }
            j++;
        }
        return this;
    }

    @NotNull
    @Override
    public Iterator<BLink<X>> iterator() {
        return values().iterator();
    }

    @NotNull
    @Override
    public Bag<X> commit() {
        if (isEmpty())
            return this;

        float mass = 0;

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
                mass += l.pri();
            }
        }

        float p = this.pending;
        this.pending = 0;

        float forgetRate = p / mass;
        Forget f;
        if (forgetRate > 0) {
            f = new Forget(forgetRate);
        } else {
            f = null;
        }

        return commit(f);
    }
    @NotNull
    @Override
    public Bag<X> commit(Consumer<BLink> each) {
        if (each!=null && !isEmpty())
            forEach(each);


        return this;
    }


    @Override
    public X boost(Object key, float boost) {
        BLink<X> b = get(key);
        if (b!=null && !b.isDeleted()) {
            float before = b.pri();
            b.priMult(boost);
            float after = b.pri();
            pending += (after - before);
            return b.get();
        }
        return null;
    }
}

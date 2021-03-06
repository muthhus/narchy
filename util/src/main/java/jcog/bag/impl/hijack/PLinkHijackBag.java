package jcog.bag.impl.hijack;

import jcog.bag.impl.HijackBag;
import jcog.pri.PriReference;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Created by me on 2/17/17.
 */
public class PLinkHijackBag<X> extends HijackBag<X, PriReference<X>> {

    public PLinkHijackBag(int initialCapacity, int reprobes) {
        super(initialCapacity, reprobes);
    }

    @Override
    public final float pri( PriReference<X> key) {
        return key.pri();
    }

    @NotNull
    @Override
    public X key(PriReference<X> value) {
        return value.get();
    }

    /** optimized for PLink */
    @Override
    public void forEachKey(@NotNull Consumer<? super X> each) {
        forEach(x -> each.accept(x.get()));
    }

//    @NotNull
//    @Override
//    public HijackBag<X, PLink<X>> commit() {
//        flatForget(this);
//        return this;
//    }

//    public static void flatForget(HijackBag<?,? extends PLink> b) {
//        int s = b.size();
//        if (s > 0) {
//
//            double p = b.pressure.get() /* MULTIPLIER TO ANTICIPATE NEXT period */;
//            //float ideal = s * b.temperature();
//
//            if (p > EPSILON_DEFAULT) {
//                if (b.pressure.compareAndSet(p, 0)) {
//
//                    b.commit(null); //precommit to get accurate mass
//                    float mass = b.mass;
//
//                    float deduction = //(float) ((p + mass) - ideal);
//                            ((float) p / ((float) p + mass)) / s;
//                    if (deduction > EPSILON_DEFAULT) {
//                        b.commit(x -> x.priSub(deduction));
//                    }
//                }
//
//            }
//        }
//
//    }


    @Override
    protected PriReference<X> merge(PriReference<X> existing, PriReference<X> incoming, MutableFloat overflowing) {
        float overflow = existing.priAddOverflow(incoming.priElseZero() );
        if (overflow > 0) {
            //pressurize(-overflow);
            if (overflowing!=null) overflowing.add(overflow);
        }
        return existing;
    }



//    @Override
//    public PriForget forget(float avgToBeRemoved) {
//        return new PriForget(avgToBeRemoved);
//    }
}

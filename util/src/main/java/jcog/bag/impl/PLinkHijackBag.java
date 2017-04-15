package jcog.bag.impl;

import jcog.bag.PForget;
import jcog.bag.PLink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

import static jcog.bag.PLink.EPSILON_DEFAULT;

/**
 * Created by me on 2/17/17.
 */
public class PLinkHijackBag<X> extends HijackBag<X, PLink<X>> {

    public PLinkHijackBag(int initialCapacity, int reprobes, Random random) {
        super(initialCapacity, reprobes, random);
    }

    @Override
    public float pri(@NotNull PLink<X> key) {
        return key.pri();
    }

    @NotNull
    @Override
    public X key(PLink<X> value) {
        return value.get();
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
    protected float merge(@Nullable PLink<X> existing, @NotNull PLink<X> incoming, float scale) {
        incoming.priMult(scale);
        float pAdd = incoming.priSafe(0);
        if (existing != null) {
            existing.priAdd(pAdd);
        }
        return pAdd;
    }

    @Override
    protected float priEpsilon() {
        return EPSILON_DEFAULT;
    }


    @Override
    public PForget forget(float avgToBeRemoved) {
        return new PForget(avgToBeRemoved);
    }
}

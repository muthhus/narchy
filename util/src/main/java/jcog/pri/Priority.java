package jcog.pri;

import jcog.Texts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jcog.Util.*;

/**
 * Mutable Prioritized
 */
public interface Priority extends Prioritized {
    /**
     * Change priority value
     *
     * @param p The new priority
     * @return whether the operation had any effect
     */
    float setPri(float p);

    default float setPri(@NotNull Prioritized p) {
        return setPri(p.pri());
    }

    /**
     * returns null if already deleted
     */
    @Nullable Priority clonePri();


    default float priMax(float max) {
        return setPri(Math.max(priElseZero(), max));
    }

    default void priMin(float min) {
        setPri(Math.min(priElseZero(), min));
    }

    default float priAdd(float toAdd) {
        notNaN(toAdd);
        float e = pri();
        if (e != e) {
            if (toAdd <= 0) {
                return Float.NaN; //subtracting from deleted has no effect
            } /*else {
                e = 0; //adding to deleted resurrects it to pri=0 before adding
            }*/
        } else {
            toAdd += e;
        }

        return setPri(toAdd);
    }

    default float priAddAndGetDelta(float toAdd) {
        float before = priElseZero();
        return setPri(before + notNaN(toAdd)) - before;
    }

    default float priSub(float toSubtract) {
        //setPri(priElseZero() - toSubtract);
        return priAdd(-toSubtract);
    }

    default void priSub(float maxToSubtract, float minFractionRetained) {
        float p = priElseZero();
        if (p > 0) {
            float pMin = minFractionRetained * p;
            float pNext = Math.max((p - maxToSubtract), pMin);
            setPri(pNext);
        }
    }


//    default void priAvg(float pOther, float rate) {
//        float cu = priElseZero();
//        setPriority(Util.lerp(rate, (cu + pOther)/2f, cu));
//    }

//    default float priAddOverflow(float toAdd) {
//        return priAddOverflow(toAdd, null);
//    }

    @Override
    default float priAddOverflow(float toAdd, @Nullable float[] pressurized) {
        if (Math.abs(toAdd) <= EPSILON) {
            return 0; //no change
        }

        float before = priElseZero();
        float next = priAdd(toAdd);
        float delta = next - before;
        float excess = toAdd - delta;

        if (pressurized != null)
            pressurized[0] += delta;

        return excess;
    }

    /**
     * returns overflow
     */
    @Override
    default float priAddOverflow(float toAdd) {
        if (Math.abs(toAdd) <= EPSILON) {
            return 0; //no change
        }

        float before = priElseZero();
        float next = priAdd(toAdd);
        float delta = next - before;

        return toAdd - delta;
    }


    @NotNull
    default float priMult(float factor) {
        float p = pri();
        if (p == p)
            return setPri(p * notNaNOrNeg(factor));
        return Float.NaN;
    }


    @NotNull
    default Prioritized priLerp(float target, float speed) {
        float pri = pri();
        if (pri == pri)
            setPri(lerp(speed, pri, target));
        return this;
    }

    default void take(Priority source, float p, boolean amountOrFraction, boolean copyOrMove) {
        float amount;
        if (!amountOrFraction) {
            if (p < Pri.EPSILON) return;
            amount = source.priElseZero() * p;
            if (amount < Pri.EPSILON) return;
        } else {
            amount = p;
        }

        float toAdd;
        if (copyOrMove) {
            toAdd = (amount); //COPY
        } else {
            //TRANSFER
            float afterReceived = priElseZero() + amount;
            float overflow = afterReceived - 1f;

            //cap at 1, and only transfer what is necessary to reach it
            if (overflow > 0) {
                amount -= overflow;
            }

            //subtract first to ensure the funds are available
            source.priSub(amount);
            toAdd = amount;
        }
        priAdd(toAdd);
    }

//    /** returns the delta */
//    default float priLerpMult(float factor, float speed) {
//
////        if (Util.equals(factor, 1f, Param.BUDGET_EPSILON))
////            return 0; //no change
//
//        float p = pri();
//        float target = unitize(p * factor);
//        float delta = target - p;
//        setPriority(lerp(speed, target, p));
//        return delta;
//
//    }

//    default void absorb(@Nullable MutableFloat overflow) {
//        if (overflow!=null) {
//            float taken = Math.min(overflow.floatValue(), 1f - priElseZero());
//            if (taken > EPSILON_DEFAULT) {
//                overflow.subtract(taken);
//                priAdd(taken);
//            }
//        }
//    }



    /**
     * Briefly display the BudgetValue
     *
     * @return String representation of the value with 2-digit accuracy
     */
    @Override
    @NotNull
    default Appendable toBudgetStringExternal() {
        return toBudgetStringExternal(null);
    }

    @Override
    default @NotNull StringBuilder toBudgetStringExternal(StringBuilder sb) {
        return Prioritized.toStringBuilder(sb, Texts.n2(pri()));
    }

    @Override
    @NotNull
    default String toBudgetString() {
        return toBudgetStringExternal().toString();
    }

    @Override
    @NotNull
    default String getBudgetString() {
        return Prioritized.toString(this);
    }

    @Override
    default void normalizePri(float min, float range) {
        //setPri( (p - min)/range );
        normalizePri(min, range, 1f);
    }

    /**
     * normalizes the current value to within: min..(range+min), (range=max-min)
     */
    @Override
    default void normalizePri(float min, float range, float lerp) {
        float p = priElseNeg1();
        if (p < 0) return; //dont normalize if deleted

        priLerp((p - min) / range, lerp);
    }


//    void orPriority(float v);
//
//    void orPriority(float x, float y);

}

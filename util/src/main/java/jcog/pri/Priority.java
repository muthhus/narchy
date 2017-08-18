package jcog.pri;

import jcog.Texts;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

import static jcog.Util.*;

/**
 * Created by me on 2/17/17.
 */
public interface Priority extends Prioritized {

    /**
     * common instance for a 'Deleted budget'.
     */
    Priority Deleted = new PriRO(Float.NaN);

    /**
     * common instance for a 'full budget'.
     */
    Priority One = new PriRO(1f);

    /**
     * common instance for a 'half budget'.
     */
    Priority Half = new PriRO(0.5f);

    /**
     * common instance for a 'zero budget'.
     */
    Priority Zero = new PriRO(0);

    /**
     * default minimum difference necessary to indicate a significant modification in budget float number components
     */
    float EPSILON = 0.00001f;

    /**
     * decending order (highest first)
     */
    Comparator<Priority> IdentityComparator = (Priority a, Priority b) -> {
        if (a == b) return 0;

        float ap = a.priElseNeg1();
        float bp = b.priElseNeg1();

        int q = Float.compare(bp, ap);
        if (q == 0) {
            //if still not equal, then use system identiy
            return Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
        }
        return q;
    };


    static String toString(@NotNull Priority b) {
        return toStringBuilder(null, Texts.n4(b.pri())).toString();
    }

    @NotNull
    static StringBuilder toStringBuilder(@Nullable StringBuilder sb, @NotNull String priorityString) {
        int c = 1 + priorityString.length();
        if (sb == null)
            sb = new StringBuilder(c);
        else {
            sb.ensureCapacity(c);
        }

        sb.append('$')
                .append(priorityString);
        //.append(Op.BUDGET_VALUE_MARK);

        return sb;
    }

    @NotNull
    static Ansi.Color budgetSummaryColor(@NotNull Prioritized tv) {
        int s = (int) Math.floor(tv.priElseZero() * 5);
        switch (s) {
            default:
                return Ansi.Color.DEFAULT;

            case 1:
                return Ansi.Color.MAGENTA;
            case 2:
                return Ansi.Color.GREEN;
            case 3:
                return Ansi.Color.YELLOW;
            case 4:
                return Ansi.Color.RED;

        }
    }

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

    @Override
    @NotNull
    default Priority priority() {
        return this;
    }

//    default void priAvg(float pOther, float rate) {
//        float cu = priElseZero();
//        setPriority(Util.lerp(rate, (cu + pOther)/2f, cu));
//    }

//    default float priAddOverflow(float toAdd) {
//        return priAddOverflow(toAdd, null);
//    }

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
    default float priAddOverflow(float toAdd) {
        if (Math.abs(toAdd) <= EPSILON) {
            return 0; //no change
        }

        float before = priElseZero();
        float next = priAdd(toAdd);
        float delta = next - before;

        return toAdd - delta;
    }

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

    @NotNull
    default float priMult(float factor) {
        float p = pri();
        if (p == p)
            return setPri(p * notNaNOrNeg(factor));
        return Float.NaN;
    }


    @NotNull
    default Priority priLerp(float target, float speed) {
        float pri = pri();
        if (pri == pri)
            setPri(lerp(speed, pri, target));
        return this;
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
     * returns null if already deleted
     */
    @Nullable Priority clonePri();


    /**
     * Briefly display the BudgetValue
     *
     * @return String representation of the value with 2-digit accuracy
     */
    @NotNull
    default Appendable toBudgetStringExternal() {
        return toBudgetStringExternal(null);
    }

    default @NotNull StringBuilder toBudgetStringExternal(StringBuilder sb) {
        return Priority.toStringBuilder(sb, Texts.n2(pri()));
    }

    @NotNull
    default String toBudgetString() {
        return toBudgetStringExternal().toString();
    }

    @NotNull
    default String getBudgetString() {
        return toString(this);
    }

    default void normalizePri(float min, float range) {
        //setPri( (p - min)/range );
        normalizePri(min, range, 1f);
    }

    /**
     * normalizes the current value to within: min..(range+min), (range=max-min)
     */
    default void normalizePri(float min, float range, float lerp) {
        float p = priElseNeg1();
        if (p < 0) return; //dont normalize if deleted

        priLerp((p - min) / range, lerp);
    }

    static <X extends Priority> void normalize(X[] xx, float target) {
        int l = xx.length;
        assert (target == target);
        assert (l > 0);

        float ss = sum(Priority::priElseZero, xx);
        if (ss <= Pri.EPSILON)
            return;

        float factor = target / ss;

        for (X x : xx)
            x.priMult(factor);

    }


//    void orPriority(float v);
//
//    void orPriority(float x, float y);

}

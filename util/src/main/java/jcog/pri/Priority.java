package jcog.pri;

import jcog.Texts;
import jcog.bag.Bag;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jcog.Util.lerp;

/**
 * Created by me on 2/17/17.
 */
public interface Priority extends Prioritized {

    /**common instance for a 'Deleted budget'.*/
    Priority Deleted = new ROBudget(Float.NaN);

    /** common instance for a 'full budget'.*/
    Priority One = new ROBudget(1f);

    /** common instance for a 'half budget'.*/
    Priority Half = new ROBudget(0.5f);

    /** common instance for a 'zero budget'.*/
    Priority Zero = new ROBudget(0);

    /** minimum difference necessary to indicate a significant modification in budget float number components */
    float EPSILON_DEFAULT = 0.000001f;


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

        return  sb;
    }

    @NotNull
    static Ansi.Color budgetSummaryColor(@NotNull Prioritized tv) {
        int s = (int)Math.floor(tv.priSafe(0) *5);
        switch (s) {
            default: return Ansi.Color.DEFAULT;

            case 1: return Ansi.Color.MAGENTA;
            case 2: return Ansi.Color.GREEN;
            case 3: return Ansi.Color.YELLOW;
            case 4: return Ansi.Color.RED;

        }
    }

    /**
     * simple additive Priority merging
     * if existing is not null, then priority from incoming is applied to it * scale
     * if existing is null, then incoming enters existence with incoming priority * scale
     * returns the delta change in ambient pressure
     */
    static float combine(@Nullable Priority existing, @NotNull Priority incoming, float scale) {
        float pAdd = incoming.priSafe(0) * scale;
        float pressure;

        if (existing != null) {
            float before = existing.priSafe(0);

            //modify existing
            existing.priAdd(pAdd);

            pressure = existing.priSafe(0) - before;
        } else {
            //modify incoming
            incoming.setPriority(pAdd);
            pressure = (pAdd);
        }
        return pressure;
    }

    default void priMax(float max) {
        setPriority(Math.max(priSafe(0), max));
    }
    default void priAdd(float toAdd) {
        setPriority(priSafe(0) + toAdd);
    }
    default void priSub(float toSubtract) { setPriority(priSafe(0) - toSubtract); }
    default void priSub(float maxToSubtract, float minFractionRetained) {
        float p = priSafe(0);
        if (p > 0) {
            float pMin = minFractionRetained * p;
            float pNext = Math.max((p - maxToSubtract), pMin);
            setPriority(pNext);
        }
    }

    @Override @NotNull
    default Priority priority() {
        return this;
    }

//    default void priAvg(float pOther, float rate) {
//        float cu = priSafe(0);
//        setPriority(Util.lerp(rate, (cu + pOther)/2f, cu));
//    }

//    default float priAddOverflow(float toAdd) {
//        return priAddOverflow(toAdd, null);
//    }

    default float priAddOverflow(float toAdd, @Nullable Bag pressurized) {
        float before = priSafe(0);
        float next = before + toAdd;
        float change;
        if (next > 1) {
            change = next - 1;
            next = 1;
        } else {
            change = 0;
        }

        setPriority(next);

        if (pressurized!=null)
            pressurized.pressurize(next - before);

        return change;
    }

    static float validPriority(float p) {
        if (p!=p /* fast NaN test */)
            throw new PriorityException();
        if (p > 1.0f)
            p = 1.0f;
        else if (p < 0.0f)
            p = 0.0f;
        return p;
    }

    /**
     * Change priority value
     *
     * @param p The new priority
     * @return whether the operation had any effect
     */
    void setPriority(float p);

    default void setPriority(@NotNull Prioritized p) {
        setPriority(p.pri());
    }

    //    default Budget mult(float priFactor, int durFactor, float quaFactor) {
    //        if (priFactor!=1) priMult(priFactor);
    //        if (durFactor!=1) durMult(durFactor);
    //        if (quaFactor!=1) quaMult(quaFactor);
    //        return this;
    //    }
    //

    @NotNull
    default Priority priMult(float factor) {
        float p = pri();
        if (p==p)
            setPriority(p * factor);
        return this;
    }


    @NotNull
    default Priority priLerp(float target, float speed) {
        setPriority(lerp(speed, target, pri()));
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
//            float taken = Math.min(overflow.floatValue(), 1f - priSafe(0));
//            if (taken > EPSILON_DEFAULT) {
//                overflow.subtract(taken);
//                priAdd(taken);
//            }
//        }
//    }

    /** returns null if already deleted */
    @Nullable Priority clone();


    /**
     * Briefly display the BudgetValue
     *
     * @return String representation of the value with 2-digit accuracy
     */
    @NotNull
    default Appendable toBudgetStringExternal()  {
        return toBudgetStringExternal(null);
    }

    default @NotNull StringBuilder toBudgetStringExternal(StringBuilder sb)  {
        return Priority.toStringBuilder(sb, Texts.n2(pri()));
    }

    @NotNull
    default String toBudgetString() {
        return toBudgetStringExternal().toString();
    }

    @NotNull default String getBudgetString() {
        return toString(this);
    }

//    void orPriority(float v);
//
//    void orPriority(float x, float y);

    final class PriorityException extends RuntimeException {
        public PriorityException() {
            super("NaN");
        }
        public PriorityException(String message) {
            super(message);
        }

        @Override
        public Throwable fillInStackTrace() {
            return null;
        }
    }

}

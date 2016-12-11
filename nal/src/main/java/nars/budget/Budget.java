package nars.budget;

import jcog.Texts;
import jcog.Util;
import nars.Param;
import nars.Symbols;
import nars.Task;
import nars.task.util.SoftException;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import static java.lang.Math.pow;
import static jcog.Util.lerp;
import static jcog.Util.unitize;
import static nars.util.UtilityFunctions.and;
import static nars.util.UtilityFunctions.or;

/**
 * Created by me on 12/11/15.
 */
public interface Budget extends Budgeted {


    /**common instance for a 'Deleted budget'.*/
    Budget Deleted = new ROBudget(Float.NaN, 0);

//    /** common instance for a 'full budget'.*/
//    Budget One = new ROBudget(1f, 1f);

    /** common instance for a 'half budget'.*/
    Budget Half = new ROBudget(0.5f, 0.5f);

    /** common instance for a 'zero budget'.*/
    Budget Zero = new ROBudget(0, 0);

    //@Contract(pure = true)
    public static boolean aveGeoNotLessThan(float min, float a, float b, float c) {
        float minCubed = min * min * min; //cube both sides
        return a * b * c >= minCubed;
    }
    public static boolean aveGeoNotLessThan(float min, float a, float b) {
        float minCubed = min * min; //cube both sides
        return a * b >= minCubed;
    }

    public static float aveGeo(float a, float b, float c) {
        return (float) pow(a * b * c, 1.0 / 3.0);
    }
    public static float aveGeo(float a, float b) {
        return (float) Math.sqrt(a * b);
    }

//    //@Contract(pure = true)
//    public static boolean isDeleted(float pri) {
//        return !Float.isFinite(pri);
//    }


//    @Override
//    default float priIfFiniteElseZero() {
//        float p = pri();
//        return /*Float.isFinite(p)*/ (p==p) ? p : 0;
//    }


    public static String toString(@NotNull Budget b) throws IOException {
        return toStringBuilder(null, Texts.n4(b.pri()), Texts.n4(b.qua())).toString();
    }

    @NotNull
    public static Appendable toStringBuilder(@Nullable Appendable sb, @NotNull CharSequence priorityString, @NotNull CharSequence qualityString) throws IOException {
        int c = 1 + priorityString.length() + 1 + qualityString.length() + 1;
        if (sb == null)
            sb = new StringBuilder(c);
        else {
            if (sb instanceof StringBuilder)
                ((StringBuilder)sb).ensureCapacity(c);
        }

        sb.append(Symbols.BUDGET_VALUE_MARK)
                .append(priorityString).append(Symbols.VALUE_SEPARATOR)
                .append(qualityString)
                .append(Symbols.BUDGET_VALUE_MARK);

        return  sb;
    }

    /**
     * set all quantities to zero
     */
    @Nullable
    default Budget zero() {
        return setBudget(0, 0);
    }



    @NotNull
    @Override
    default Budget budget() {
        return this;
    }

    default void priAdd(float toAdd) {
        setPriority(priIfFiniteElseZero() + toAdd);
    }
    default void priSub(float toSubtract) {
        priAdd(-toSubtract);
    }

    @NotNull
    default Budgeted cloneMult(float p, float d, float q) {
        Budget x = clone();
        x.mul(p, q);
        return x;
    }

    default void absorb(@Nullable MutableFloat overflow) {
        if (overflow!=null) {
            float taken = Math.min(overflow.floatValue(), 1f - priIfFiniteElseZero());
            if (taken > Param.BUDGET_EPSILON) {
                overflow.subtract(taken);
                priAdd(taken);
            }
        }
    }


    final static class BudgetException extends SoftException {
        public BudgetException() {
            super("NaN");
        }
        public BudgetException(String message) {
            super(message);
        }

    }

    /**
     * Change priority value
     *
     * @param p The new priority
     * @return whether the operation had any effect
     */
    void setPriority(float p);

    public static float validPriority(float p) {
        if (p!=p /* fast NaN test */)
            throw new BudgetException();
        if (p > 1.0f)
            p = 1.0f;
        else if (p < 0.0f)
            p = 0.0f;
        return p;
    }

    public static float validQuality(float q) {
        if (q!=q /* fast NaN test */)
            return Float.NaN;
        else {
//            if (p > 1f - Param.BUDGET_EPSILON)
//                throw new BudgetException("quality must be < 1.0");
//            else if (p < 0f)
//                throw new BudgetException("quality must be > 0");
            return Util.clamp(q, 0, 1f - Param.BUDGET_EPSILON);
        }
    }

   

//    default Budget mult(float priFactor, float durFactor, float quaFactor) {
//        if (priFactor!=1) priMult(priFactor);
//        if (durFactor!=1) durMult(durFactor);
//        if (quaFactor!=1) quaMult(quaFactor);
//        return this;
//    }
//
    @NotNull
    default Budget priMult(float factor) {
        float p = pri();
        if (p==p)
            setPriority(p * factor);
        return this;
    }

    @NotNull
    default Budget priLerp(float target, float speed) {
        setPriority(lerp(target, pri(), speed));
        return this;
    }

    /** returns the delta */
    default float priLerpMult(float factor, float speed) {

        if (Util.equals(factor, 1f, Param.BUDGET_EPSILON))
            return 0; //no change

        float p = pri();
        float target = unitize(p * factor);
        float delta = target - p;
        setPriority(lerp(target, p, speed));
        return delta;

    }

    default void quaMult(float factor) {
        setQuality(qua() * factor);
    }

    default boolean equals(Budgeted b, float epsilon) {
        return
                Util.equals(priIfFiniteElseNeg1(), b.priIfFiniteElseNeg1(), epsilon) &&
                Util.equals(qua(), qua(), epsilon);

    }

//    default void durMult(float factor) {
//        setDurability(dur() * factor);
//    }
//    default void quaMult(float factor) {
//        setQuality(qua() * factor);
//    }



    void setQuality(float q);


    default void orPriority(float v) {
        setPriority(or(pri(), v));
    }
    default void orPriority(float x, float y) {
        setPriority(or(pri(), x, y));
    }

    default void orQuality(float v) {
        setQuality(BudgetFunctions.or(qua(), v));
    }




    /** returns null if already deleted */
    @Nullable Budget clone();

    default boolean summaryLessThan(float s) {
        return !summaryNotLessThan(s);
    }

    /**
     * uses optimized aveGeoNotLessThan to avoid a cube root operation
     */
    default boolean summaryNotLessThan(float min) {
        return min == 0f || aveGeoNotLessThan(min, pri(), qua());
    }


//    default void maxDurability(final float otherDurability) {
//        setDurability(Util.max(getDurability(), otherDurability)); //max durab
//    }
//
//    default void maxQuality(final float otherQuality) {
//        setQuality(Util.max(getQuality(), otherQuality)); //max durab
//    }



    /**
     * AND's (multiplies) priority with another value
     */
    default void andPriority(float v) {
        setPriority(and(pri(), v));
    }

//    /**
//     * Whether the budget should get any processing at all
//     * <p>
//     * to be revised to depend on how busy the system is
//     * tests whether summary >= threhsold
//     *
//     * @return The decision on whether to process the Item
//     */
//    default boolean summaryGreaterOrEqual(float budgetThreshold) {
//
//        if (isDeleted()) return false;
//
//        /* since budget can only be positive.. */
//        if (budgetThreshold <= 0) return true;
//
//
//        return summaryNotLessThan(budgetThreshold);
//    }

    /**
     * copies a budget into this; if source is null, it deletes the budget
     */
    @NotNull
    default Budget setBudget(@Nullable Budgeted srcCopy) {
        if (srcCopy == null) {
            zero();
        } else {
            setBudget(srcCopy.pri(), srcCopy.qua());
        }

        return this;
    }

    Budget setBudget(float p, float q);

//    /**
//     * returns this budget, after being modified
//     */
//    @NotNull
//    default Budget setBudget(float p, float d, float q) {
//        setPriority(p);
//        setDurability(d);
//        setQuality(q);
//        return this;
//    }

    /**
     * Briefly display the BudgetValue
     *
     * @return String representation of the value with 2-digit accuracy
     */
    @NotNull
    default Appendable toBudgetStringExternal() throws IOException {
        return toBudgetStringExternal(null);
    }

    @NotNull
    default Appendable toBudgetStringExternal(Appendable sb) throws IOException {
        return toStringBuilder(sb, Texts.n2(pri()), Texts.n2(qua()));
    }

    @NotNull
    default String toBudgetString() {
        try {
            return toBudgetStringExternal().toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    default String getBudgetString() {
        try {
            return toString(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    default void set(@NotNull Budgeted b) {
        setBudget(b.pri(), b.qua());
    }

    @NotNull
    public static Ansi.Color budgetSummaryColor(@NotNull Task tv) {
        int s = (int)Math.floor(tv.summary()*5);
        switch (s) {
            default: return Ansi.Color.DEFAULT;

            case 1: return Ansi.Color.MAGENTA;
            case 2: return Ansi.Color.GREEN;
            case 3: return Ansi.Color.YELLOW;
            case 4: return Ansi.Color.RED;

        }
    }

    default void mul(float pf, float qf) {
        setPriority(pri()*pf);
        setQuality(qua()*qf);
    }
    default Budget multiplied(float pf, float qf) {
        mul(pf, qf);
        return this;
    }

//    public static class BudgetException extends RuntimeException {
//        public BudgetException() {
//            super();
//        }
//        public BudgetException(String reason) {
//            super(reason);
//        }
//    }



}

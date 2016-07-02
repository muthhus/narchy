package nars.budget;

import nars.Symbols;
import nars.task.Task;
import nars.util.Texts;
import nars.util.Util;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import static java.lang.Math.pow;
import static nars.nal.UtilityFunctions.and;
import static nars.nal.UtilityFunctions.or;

/**
 * Created by me on 12/11/15.
 */
public interface Budget extends Budgeted {



    //@Contract(pure = true)
    public static boolean aveGeoNotLessThan(float min, float a, float b, float c) {
        float minCubed = min * min * min; //cube both sides
        return a * b * c >= minCubed;
    }

    public static float aveGeo(float a, float b, float c) {
        return (float) pow(a * b * c, 1.0 / 3.0);
    }

//    //@Contract(pure = true)
//    public static boolean isDeleted(float pri) {
//        return !Float.isFinite(pri);
//    }


    @Override
    default float priIfFiniteElseZero() {
        float p = pri();
        return /*Float.isFinite(p)*/ (p==p) ? p : 0;
    }


    public static String toString(@NotNull Budget b) {

        return toStringBuilder(new StringBuilder(), Texts.n4(b.pri()), Texts.n4(b.dur()), Texts.n4(b.qua())).toString();
    }

    @NotNull
    public static StringBuilder toStringBuilder(@org.jetbrains.annotations.Nullable StringBuilder sb, @NotNull CharSequence priorityString, @NotNull CharSequence durabilityString, @NotNull CharSequence qualityString) {
        int c = 1 + priorityString.length() + 1 + durabilityString.length() + 1 + qualityString.length() + 1;
        if (sb == null)
            sb = new StringBuilder(c);
        else
            sb.ensureCapacity(c);

        sb.append(Symbols.BUDGET_VALUE_MARK)
                .append(priorityString).append(Symbols.VALUE_SEPARATOR)
                .append(durabilityString).append(Symbols.VALUE_SEPARATOR)
                .append(qualityString)
                .append(Symbols.BUDGET_VALUE_MARK);

        return  sb;
    }

    /**
     * set all quantities to zero
     */
    @Nullable
    default Budget zero() {
        return budget(0, 0, 0);
    }



    @NotNull
    @Override
    default Budget budget() {
        return this;
    }

    default void priAdd(float toAdd) {
        setPriority(pri() + toAdd);
    }
    default void priSub(float toSubtract) {
        priAdd(-toSubtract);
    }

    @NotNull
    default Budgeted cloneMult(float p, float d, float q) {
        Budget x = clone();
        x.mul(p, d, q);
        return x;
    }


    final static class InvalidPriorityException extends RuntimeException {
        public InvalidPriorityException() {
            super();
        }

        @Override
        public String getMessage() {
            return "NaN priority";
        }
    }

    /**
     * Change priority value
     *
     * @param p The new priority
     * @return whether the operation had any effect
     */
    default void setPriority(float p) {
        if (p!=p /* fast NaN test */)
            throw new InvalidPriorityException();

        _setPriority(Util.clamp(p));
    }

    /** called from setPriority after validation */
    void _setPriority(float p);

   

//    default Budget mult(float priFactor, float durFactor, float quaFactor) {
//        if (priFactor!=1) priMult(priFactor);
//        if (durFactor!=1) durMult(durFactor);
//        if (quaFactor!=1) quaMult(quaFactor);
//        return this;
//    }
//
    default void priMult(float factor) {
        float pri = pri();
        if (pri==pri) //if not deleted
            setPriority(pri * factor);
    }

    default void durMult(float factor) {
        setDurability(dur() * factor);
    }
    default void quaMult(float factor) {
        setQuality(qua() * factor);
    }


//    default void durMult(float factor) {
//        setDurability(dur() * factor);
//    }
//    default void quaMult(float factor) {
//        setQuality(qua() * factor);
//    }


    default void setDurability(float d) {
        _setDurability(Util.clamp(d));
    }

    public abstract void _setDurability(float d);


    default void setQuality(float q) {
        _setQuality(Util.clamp(q));
    }

    public abstract void _setQuality(float q);

    default boolean equalsByPrecision(@NotNull Budget t, float epsilon) {
        return Util.equals(pri(), t.pri(), epsilon) &&
                Util.equals(dur(), t.dur(), epsilon) &&
                Util.equals(qua(), t.qua(), epsilon);
    }

    /**
     * Increase priority value by a percentage of the remaining range.
     * Uses the 'or' function so it is not linear
     *
     * @param v The increasing percent
     */
    default void orPriority(float v) {
        setPriority(or(pri(), v));
    }
    default void orPriority(float x, float y) {
        setPriority(or(pri(), x, y));
    }





    @NotNull
    Budget clone();

    default boolean summaryLessThan(float s) {
        return !summaryNotLessThan(s);
    }

    /**
     * uses optimized aveGeoNotLessThan to avoid a cube root operation
     */
    default boolean summaryNotLessThan(float min) {
        return min == 0f || aveGeoNotLessThan(min, pri(), dur(), qua());
    }


//    default void maxDurability(final float otherDurability) {
//        setDurability(Util.max(getDurability(), otherDurability)); //max durab
//    }
//
//    default void maxQuality(final float otherQuality) {
//        setQuality(Util.max(getQuality(), otherQuality)); //max durab
//    }

    /**
     * Increase durability value by a percentage of the remaining range
     *
     * @param v The increasing percent
     */
    default void orDurability(float v) {
        setDurability(or(dur(), v));
    }

    /**
     * Decrease durability value by a percentage of the remaining range
     *
     * @param v The decreasing percent
     */
    default void andDurability(float v) {
        setDurability(and(dur(), v));
    }

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
    default Budget budget(@Nullable Budgeted source) {
        if (source == null) {
            zero();
        } else {
            budget(source.pri(), source.dur(), source.qua());
        }

        return this;
    }

    /**
     * returns this budget, after being modified
     */
    @NotNull
    default Budget budget(float p, float d, float q) {
        setPriority(p);
        setDurability(d);
        setQuality(q);
        return this;
    }

    /**
     * Briefly display the BudgetValue
     *
     * @return String representation of the value with 2-digit accuracy
     */
    @NotNull
    default StringBuilder toBudgetStringExternal() {
        return toBudgetStringExternal(null);
    }

    @NotNull
    default StringBuilder toBudgetStringExternal(StringBuilder sb) {
        return toStringBuilder(sb, Texts.n2(pri()), Texts.n2(dur()), Texts.n2(qua()));
    }

    @NotNull
    default String toBudgetString() {
        return toBudgetStringExternal().toString();
    }

    @NotNull
    default String getBudgetString() {
        return toString(this);
    }

    default void set(@NotNull Budgeted b) {
        budget(b.pri(), b.dur(), b.qua());
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

    default void mul(float pf, float df, float qf) {
        setPriority(pri()*pf);
        setDurability(dur()*df);
        setQuality(qua()*qf);
    }
    @NotNull
    default Budget multiplied(float pf, float df, float qf) {
        mul(pf, df, qf);
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

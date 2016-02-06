package nars.budget;

import nars.Symbols;
//import nars.data.BudgetedStruct;
import nars.task.Task;
import nars.util.Texts;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import static java.lang.Math.pow;
import static nars.nal.UtilityFunctions.and;
import static nars.nal.UtilityFunctions.or;
import static nars.util.data.Util.equal;

/**
 * Created by me on 12/11/15.
 */
public abstract class Budget extends BudgetedHandle {


    //@Contract(pure = true)
    public static boolean aveGeoNotLessThan(float min, float a, float b, float c) {
        float minCubed = min * min * min; //cube both sides
        return a * b * c >= minCubed;
    }

    public static float aveGeo(float a, float b, float c) {
        return (float) pow(a * b * c, 1.0 / 3.0);
    }

    //@Contract(pure = true)
    public static boolean isDeleted(float pri) {
        return !Float.isFinite(pri);
    }
    
    public final boolean isDeleted() {
        return getDeleted();
    }

    @Override
    public final boolean getDeleted() {
        return isDeleted(pri());
    }   

    public static String toString(@NotNull Budget b) {

        return toStringBuilder(new StringBuilder(), Texts.n4(b.pri()), Texts.n4(b.dur()), Texts.n4(b.qua())).toString();
    }

    @org.jetbrains.annotations.Nullable
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
    @org.jetbrains.annotations.Nullable
    public Budget zero() {
        return budget(0, 0, 0);
    }

    /** the result of this should be that pri() is not finite (ex: NaN) */
    abstract public void delete();


    @NotNull
    @Override
    public final Budget budget() {
        return this;
    }

    
    @Override
    public abstract void setPriority(float p);

    /**
     * returns the period in time: currentTime - lastForgetTime and sets the lastForgetTime to currentTime
     */

    @Override
    public abstract long setLastForgetTime(long currentTime);

   

//    public Budget mult(float priFactor, float durFactor, float quaFactor) {
//        if (priFactor!=1) priMult(priFactor);
//        if (durFactor!=1) durMult(durFactor);
//        if (quaFactor!=1) quaMult(quaFactor);
//        return this;
//    }
//
    public void priMult(float factor) {
        setPriority(pri() * factor);
    }
//    public void durMult(float factor) {
//        setDurability(dur() * factor);
//    }
//    public void quaMult(float factor) {
//        setQuality(qua() * factor);
//    }


    public abstract void setDurability(float d);



    public abstract void setQuality(float q);

    public boolean equalsByPrecision(@NotNull Budget t, float epsilon) {
        return equal(pri(), t.pri(), epsilon) &&
                equal(dur(), t.dur(), epsilon) &&
                equal(qua(), t.qua(), epsilon);
    }

    /**
     * Increase priority value by a percentage of the remaining range.
     * Uses the 'or' function so it is not linear
     *
     * @param v The increasing percent
     */
    public void orPriority(float v) {
        setPriority(or(pri(), v));
    }



    /**
     * To summarize a BudgetValue into a single number in [0, 1]
     *
     * @return The summary value
     */
    public float summary() {
        return aveGeo(pri(), dur(), qua());
    }

    @NotNull
    @Override
    public abstract Budget clone();

    public boolean summaryLessThan(float s) {
        return !summaryNotLessThan(s);
    }

    /**
     * uses optimized aveGeoNotLessThan to avoid a cube root operation
     */
    public boolean summaryNotLessThan(float min) {
        return min == 0f || aveGeoNotLessThan(min, pri(), dur(), qua());
    }


//    public void maxDurability(final float otherDurability) {
//        setDurability(Util.max(getDurability(), otherDurability)); //max durab
//    }
//
//    public void maxQuality(final float otherQuality) {
//        setQuality(Util.max(getQuality(), otherQuality)); //max durab
//    }

    /**
     * Increase durability value by a percentage of the remaining range
     *
     * @param v The increasing percent
     */
    public void orDurability(float v) {
        setDurability(or(dur(), v));
    }

    /**
     * Decrease durability value by a percentage of the remaining range
     *
     * @param v The decreasing percent
     */
    public void andDurability(float v) {
        setDurability(and(dur(), v));
    }

    /**
     * AND's (multiplies) priority with another value
     */
    public void andPriority(float v) {
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
//    public boolean summaryGreaterOrEqual(float budgetThreshold) {
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
    public BudgetedStruct budget(@Nullable Budget source) {
        if (source == null) {
            zero();
        } else {
            budget(source.pri(), source.dur(), source.qua());
            setLastForgetTime(source.getLastForgetTime());
        }

        return this;
    }

    /**
     * returns this budget, after being modified
     */
    @NotNull
    public Budget budget(float p, float d, float q) {
        setPriority(p);
        setDurability(d);
        setQuality(q);
        return this;
    }

    @NotNull
    public BudgetedStruct budget(@NotNull BudgetedHandle source) {
        return budget(source.budget());
    }

    public float priIfFiniteElseZero() {
        return priIfFiniteElse(0);
    }

    public float priIfFiniteElse(float ifNonFinite) {
        float p = pri();
        return Float.isFinite(p) ? p : ifNonFinite;
    }

    /**
     * Briefly display the BudgetValue
     *
     * @return String representation of the value with 2-digit accuracy
     */
    @org.jetbrains.annotations.Nullable
    public StringBuilder toBudgetStringExternal() {
        return toBudgetStringExternal(null);
    }

    @org.jetbrains.annotations.Nullable
    public StringBuilder toBudgetStringExternal(StringBuilder sb) {
        //return MARK + priority.toStringBrief() + SEPARATOR + durability.toStringBrief() + SEPARATOR + quality.toStringBrief() + MARK;

        CharSequence priorityString = Texts.n2(pri());
        CharSequence durabilityString = Texts.n2(dur());
        CharSequence qualityString = Texts.n2(qua());

        return toStringBuilder(sb, priorityString, durabilityString, qualityString);
    }

    @NotNull
    public String toBudgetString() {
        return toBudgetStringExternal().toString();
    }

    @NotNull
    public String getBudgetString() {
        return toString(this);
    }

    public void set(@NotNull Budget b) {
        budget(b.pri(), b.dur(), b.qua());
    }

    public static Ansi.Color budgetSummaryColor(Task tv) {
        int s = (int)Math.floor(tv.summary()*5);
        switch (s) {
            case 1: return Ansi.Color.MAGENTA;
            case 2: return Ansi.Color.GREEN;
            case 3: return Ansi.Color.YELLOW;
            case 4: return Ansi.Color.RED;

            default: return Ansi.Color.DEFAULT;
        }
    }

}

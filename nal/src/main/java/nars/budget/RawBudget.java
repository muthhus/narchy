package nars.budget;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.budget.Budget.validBudgetValue;
import static nars.budget.Budget.validBudgetValueOrNaN;
import static nars.nal.UtilityFunctions.and;

/**
 * Contains only the 3 p,d,q as floats.  For general purpose usage, you probably want to use UnitBudget
 * because this includes timestamp.
 */
public class RawBudget implements Budget {

    /**
     * The relative share of time resource to be allocated
     */
    protected float priority;


    /**
     * The overall (context-independent) evaluation
     */
    protected float quality;

    public RawBudget() {
    }

    public RawBudget(@NotNull Budgeted b, float scale) {
        this(b.pri()*scale, b.qua());
    }
    public RawBudget(@NotNull Budgeted b) {
        this(b.pri(), b.qua());
    }

    public RawBudget(float p, float q) {
        this.priority = validBudgetValue(p);
        this.quality = validBudgetValueOrNaN(q);
    }


    @Nullable
    @Override
    public Budget clone() {
        float p = priority;
        return p != p /* deleted? */ ? null : new RawBudget(p, qua());
    }

    /**
     * Get priority value
     *
     * @return The current priority
     */
    @Override
    public final float pri() {
        return priority;
    }

    @Override
    public final float priIfFiniteElseZero() {
        float p = pri(); return /*Float.isFinite(p)*/ (p==p) ? p : 0;
    }

    @Override
    public final float priIfFiniteElseNeg1() {
        float p = pri(); return /*Float.isFinite(p)*/ (p==p) ? p : -1;
    }


    @Override
    public boolean isDeleted() {
        float p = priority;
        return p!=p; //fast NaN check
    }




    /**
     * Get quality value
     *
     * @return The current quality
     */
    @Override
    public final float qua() {
        return quality;
    }


    @Override
    public boolean delete() {
        float p = priority;
        if (p==p) {
        //if (!isDeleted()) { //dont call isDeleted it may be overridden in a cyclical way
            this.priority = Float.NaN;
            return true;
        }
//            throw new RuntimeException("Already Deleted");
        return false;
    }




    public boolean equals(Object that) {
        throw new RuntimeException("RawBudget equality is N/A");
    }

    @Override
    public int hashCode() {
        throw new RuntimeException("RawBudget hashcode is N/A");
     }

    /**
     * Fully display the BudgetValue
     *
     * @return String representation of the value
     */
    @NotNull
    @Override
    public String toString() {
        return getBudgetString();
    }


    @NotNull
    @Override
    public final Budget setBudget(float p, float q) {
        this.priority = validBudgetValue(p);
        this.quality = validBudgetValue(q);
        return this;
    }

    @Override
    public final void setPriority(float p) {
        this.priority = validBudgetValue(p);
    }



    /**
     * Change quality value
     * allows storing NaN, used as a flag to indicate an unknown value
     *
     * @param q The new quality
     */
    @Override
    public final void setQuality(float q) {
        this.quality = validBudgetValue(q);
    }




}

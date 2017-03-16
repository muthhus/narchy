package nars.budget;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static jcog.bag.Priority.validPriority;
import static nars.budget.Budget.validQuality;

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

    private static final Logger logger = LoggerFactory.getLogger(RawBudget.class);

    public RawBudget() {
    }

    public RawBudget(@NotNull Budgeted b, float scale) {
        this(b.pri()*scale, b.qua());
    }
    public RawBudget(@NotNull Budgeted b) {
        this(b.pri(), b.qua());
    }

    public RawBudget(float p, float q) {
        this.priority = validPriority(p);
        if (q==q)
            this.quality = validQuality(q);
        else
            this.quality = Float.NaN;
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
        //logger.warn("alredy deleted");
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
        this.priority = validPriority(p);
        this.quality = validQuality(q);
        return this;
    }

    @Override
    public final void setPriority(float p) {
        this.priority = validPriority(p);
    }



    /**
     * Change quality value
     * allows storing NaN, used as a flag to indicate an unknown value
     *
     * @param q The new quality
     */
    @Override
    public final void setQua(float q) {
        this.quality = validQuality(q);
    }


    /** sets the budget even if 'b' has been deleted; priority will be zero in that case */
    @NotNull
    public final RawBudget budgetSafe(@NotNull Budget b) {
        budgetSafe(b.pri(), b.qua());
        return this;
    }

    /** if p is NaN (indicating deletion), p <== 0 */
    @NotNull public RawBudget budgetSafe(float p, float q) {
        priority = p;
        quality = q;
        return this;
    }




}

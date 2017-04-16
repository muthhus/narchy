package nars.budget;


import jcog.bag.Prioritized;
import jcog.bag.Priority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jcog.bag.Priority.validPriority;

/**
 * Contains only the 3 p,d,q as floats.  For general purpose usage, you probably want to use UnitBudget
 * because this includes timestamp.
 */
public class RawBudget implements Budget {

    /**
     * The relative share of time resource to be allocated
     */
    protected float priority;


    public RawBudget() {
    }

    public RawBudget(@NotNull Prioritized b, float scale) {
        this(b.pri()*scale);
    }

    public RawBudget(@NotNull Prioritized b) {
        this(b.pri());
    }

    public RawBudget(float p) {
        this.priority = validPriority(p);

    }


    @Nullable
    @Override
    public Priority clone() {
        float p = priority;
        return p != p /* deleted? */ ? null : new RawBudget(p);
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



    @Override
    public final void setPriority(float p) {
        this.priority = validPriority(p);
    }





    /** sets the budget even if 'b' has been deleted; priority will be zero in that case */
    @NotNull
    public final RawBudget budgetSafe(@NotNull Priority b) {
        budgetSafe(b.priSafe(0));
        return this;
    }

    /** if p is NaN (indicating deletion), p <== 0 */
    @NotNull public RawBudget budgetSafe(float p) {
        priority = p;
        return this;
    }




}

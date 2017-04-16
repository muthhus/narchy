package jcog.pri;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jcog.pri.Priority.validPriority;

/**
 * default mutable prioritized implementation
 */
public class Pri implements Priority {

    /**
     * The relative share of time resource to be allocated
     */
    protected float priority;


    public Pri() {
    }

    public Pri(@NotNull Prioritized b, float scale) {
        this(b.pri()*scale);
    }

    public Pri(@NotNull Prioritized b) {
        this(b.pri());
    }

    public Pri(float p) {
        this.priority = validPriority(p);
    }



    @Nullable
    @Override
    public Priority clone() {
        float p = priority;
        return p != p /* deleted? */ ? null : new Pri(p);
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





    /** if p is NaN (indicating deletion), p <== 0 */
    @NotNull public Pri pri(float p) {
        priority = p;
        return this;
    }




}

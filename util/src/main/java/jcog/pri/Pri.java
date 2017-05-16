package jcog.pri;


import jcog.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * default mutable prioritized implementation
 */
public class Pri implements Priority {

    /**
     * The relative share of time resource to be allocated
     */
    protected float priority;


    public Pri() {
        priority = Float.NaN;
    }

    public Pri(@NotNull Prioritized b, float scale) {
        this(b.pri()*scale);
    }

    public Pri(@NotNull Prioritized b) {
        this(b.pri());
    }

    public Pri(float p) {
        setPri(p);
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
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
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
    public final float setPri(float p) {
        return this.priority = Util.unitize(p);
    }

    @NotNull public Pri setPriThen(float p) {
        setPri(p);
        return this;
    }


}

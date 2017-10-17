package jcog.pri;


import jcog.Util;
import org.jetbrains.annotations.Nullable;

/**
 * default mutable prioritized implementation
 * float 32 bit
 */
public class Pri implements Priority {

    /**
     * The relative share of time resource to be allocated
     */
    protected float pri;

    public Pri() {
        pri = Float.NaN;
    }

    public Pri(Prioritized b) {
        this(b.pri());
    }

    public Pri(float p) {
        setPri(p);
    }



    @Nullable
    @Deprecated @Override
    public Priority clonePri() {
//        throw new UnsupportedOperationException();
        float p = pri();
        return p != p /* deleted? */ ? null : new Pri(p);
    }

    /**
     * Get priority value
     *
     * @return The current priority
     */
    @Override
    public float pri() {
        return pri;
    }


    /** duplicate of Prioritized's impl, for speed (hopefully) */
    @Override public float priElseNeg1() {
        float p = pri; //pri() if there are any subclasses they should call pri()
        return p == p ? p : -1;
    }
    /** duplicate of Prioritized's impl, for speed (hopefully) */
    @Override public float priElseZero() {
        float p = pri; //pri() if there are any subclasses they should call pri()
        return p == p ? p : 0;
    }

    @Override
    public boolean delete() {
        float p = pri;
        if (p==p) {
        //if (!isDeleted()) { //dont call isDeleted it may be overridden in a cyclical way
            this.pri = Float.NaN;
            return true;
        }
        //logger.warn("alredy deleted");
//            throw new RuntimeException("Already Deleted");
        return false;
    }




//    public boolean equals(Object that) {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public int hashCode() {
//        throw new UnsupportedOperationException();
//     }

    /**
     * Fully display the BudgetValue
     *
     * @return String representation of the value
     */
    @Override
    public String toString() {
        return getBudgetString();
    }

    @Override
    public float setPri(float p) {
        return this.pri = Util.unitize(p);
    }

    public Pri setPriThen(float p) {
        setPri(p);
        return this;
    }



    public static float sum(Prioritized... src) {
        return Util.sum(Prioritized::priElseZero, src);
    }


}

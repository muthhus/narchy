package nars.truth;

import nars.Global;
import nars.util.data.Util;
import org.jetbrains.annotations.NotNull;


public abstract class AbstractTruth<T> implements MetaTruth<T> {

    /**
     * The confidence factor of the truth value
     */
    protected float confidence;


    @Override
    public final float getConfidence() {
        return confidence;
    }


//    @Override
//    public float getConfidenceMax() {
//        return Global.MAX_CONFIDENCE;
//    }

    /**
     * Compare two truth values
     *
     * @param that The other TruthValue
     * @return Whether the two are equivalent
     */

    @Override
    public boolean equals(Object that) {
        if (that == this) return true;
        if (that instanceof Truth) {
            Truth t = ((Truth) that);
            return equalsConfidence(t) && equalsFrequency(t);
        }
        return false;
    }

    public final boolean equalsConfidence(@NotNull Truth t) {
        float e = Global.TRUTH_EPSILON;//getEpsilon();
        return Util.equal(confidence, t.getConfidence(), e);
    }

    public abstract boolean equalsFrequency(Truth t);



    /**
     * The String representation of a TruthValue, as used internally by the system
     *
     * @return The String
     */
    @NotNull
    @Override
    public String toString() {
        //return DELIMITER + frequency.toString() + SEPARATOR + confidence.toString() + DELIMITER;

        //1 + 6 + 1 + 6 + 1
        return toCharSequence().toString();
    }

}

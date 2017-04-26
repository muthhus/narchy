package nars.truth;

import jcog.Util;
import nars.Param;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.truth.TruthFunctions.c2w;
import static nars.truth.TruthFunctions.w2c;

/**
 * represents a freq,evi pair precisely but does not
 * allow hashcode usage
 */
public class PreciseTruth implements Truth {

    final float f, e;

    public PreciseTruth(float freq, float conf) {
        this.f = freq;
        this.e = c2w(conf);
    }

    @Override
    public boolean equals(Object that) {
        return equals( (Truth)that, Param.TRUTH_EPSILON );
        //throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

     @NotNull
    @Override
    public String toString() {
        //return DELIMITER + frequency.toString() + SEPARATOR + confidence.toString() + DELIMITER;

        //1 + 6 + 1 + 6 + 1
        return appendString(new StringBuilder(7)).toString();
    }

    @Override
    public float freq() {
        return f;
    }

    @Override
    public float evi() {
        return e;
    }

    @Override
    public float conf() {
        return w2c(e);
    }

}

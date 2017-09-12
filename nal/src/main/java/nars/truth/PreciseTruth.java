package nars.truth;

import nars.Param;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.truth.TruthFunctions.c2wSafe;
import static nars.truth.TruthFunctions.w2c;

/**
 * represents a freq,evi pair precisely but does not
 * allow hashcode usage
 */
public class PreciseTruth implements Truth {

    final float f, e;

    public PreciseTruth(float freq, float conf) {
        this(freq, conf, true);
    }

    public PreciseTruth(float freq, float ce, boolean xIsConfOrEvidence) {
        assert ((freq == freq) && (freq >= 0) && (freq <= 1)):
                "invalid freq: " + freq;
        this.f = freq;
        assert ((ce == ce) && (ce > 0)):
                "invalid evidence/conf: " + ce;
        float e;
        if (xIsConfOrEvidence) {
            assert(ce <= TruthFunctions.MAX_CONF): ce + " is gte max (" + TruthFunctions.MAX_CONF + ')';
            e = c2wSafe(ce, Param.HORIZON);
        } else {
            e = ce;
        }
        this.e = e;
    }

    @NotNull @Override
    public Truth neg() {
        return new PreciseTruth(1f - f, e, false);
    }

    @Override
    public boolean equals(@Nullable Object that) {
        return this == that ||  (that!=null && equals( (Truth)that, Param.TRUTH_EPSILON ));
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

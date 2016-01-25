package nars.truth;

import nars.Global;
import nars.Memory;
import nars.util.data.Util;
import org.jetbrains.annotations.NotNull;

import static nars.util.data.Util.round;


public class DefaultTruth extends AbstractScalarTruth {


    private final int hash;

    /** unspecified confidence, will be invalid unless updated later */
    public DefaultTruth(float f) {
        super(f, Float.NaN);
        this.hash = 0;
    }

    public DefaultTruth(float f, float c) {
        super(
            round(f, Global.TRUTH_EPSILON),
            round(c, Global.TRUTH_EPSILON)
        );
        this.hash = Truth.hash(this, hashDiscreteness);
    }

    public DefaultTruth(char punctuation, @NotNull Memory m) {
        this(1.0f, m.getDefaultConfidence(punctuation));
    }

    public DefaultTruth(@NotNull AbstractScalarTruth toClone) {
        this(toClone.freq(), toClone.conf());
    }

    public DefaultTruth(@NotNull Truth truth) {
        this(truth.freq(), truth.conf());
    }

    @NotNull
    @Override
    public final Truth withConfMult(float factor) {
        return factor == 1 ? this : new DefaultTruth(freq, conf() * factor);
    }

    @Override public final Truth withConf(float newConf) {
        newConf = round(newConf, Global.TRUTH_EPSILON);
        return (conf != newConf) ? new DefaultTruth(freq, newConf) : this;
    }

    @Override
    public boolean equals(Object that) {
        if (that instanceof DefaultTruth) {
            return ((DefaultTruth)that).hash == hash; //shortcut, since perfect hash for this instance
        } else if (that instanceof Truth) {
            return equalsTruth((Truth)that);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    protected boolean equalsFrequency(@NotNull Truth t) {
        return (Util.equal(freq, t.freq(), Global.TRUTH_EPSILON));
    }

    private static final int hashDiscreteness = (int)(1.0f / Global.TRUTH_EPSILON);

    /*    public float getEpsilon() {
        return DEFAULT_TRUTH_EPSILON;
    }*/

//    /** truth with 0.01 resolution */
//    public static class DefaultTruth01 extends DefaultTruth {
//
//        public DefaultTruth01(float f, float c) {
//            super(f, c);
//        }
//    }
//
//
//
//    /** truth with 0.1 resolution */
//    public static class DefaultTruth1 extends AbstractDefaultTruth {
//
//        @Override
//        public float getEpsilon() {
//            return 0.1f;
//        }
//    }
//
//
//    /** truth with 0.001 resolution */
//    public static class DefaultTruth001 extends AbstractDefaultTruth {
//
//        @Override
//        public float getEpsilon() {
//            return 0.001f;
//        }
//    }
//
//
//    /** truth with 0.05 resolution */
//    public static class DefaultTruth05 extends AbstractDefaultTruth {
//
//        @Override
//        public float getEpsilon() {
//            return 0.05f;
//        }
//    }

}

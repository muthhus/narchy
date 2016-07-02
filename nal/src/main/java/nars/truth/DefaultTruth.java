package nars.truth;

import nars.Global;
import nars.Memory;
import nars.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.util.Util.clampround;


public class DefaultTruth implements Truth  {

    public final float freq, conf;
    //public final int hash;

    @Override
    public final float freq() {
        return freq;
    }

    @Override
    public final float conf() {
        return conf;
    }

    /** use with caution */
    public DefaultTruth(float... fc) {
        this(fc[0], fc[1]);
    }

    public DefaultTruth(float f, float c) {
        this(f, c, Global.TRUTH_EPSILON, hashDiscreteness);
    }

    public DefaultTruth(float f, float c, float epsilon) {
        this(f, c, Global.TRUTH_EPSILON, (int)(1f/epsilon));
    }

    public DefaultTruth(float f, float c, float epsilon, int discreteness) {

        //assert(Float.isFinite(f) && Float.isFinite(c));

        this.freq /*= f*/ = clampround(f, epsilon);
        this.conf = c = clampround(c, epsilon);

        if (c==0)
            throw new RuntimeException("zero conf");

        //this.hash = Truth.hash(f, c, discreteness);
    }

    public DefaultTruth(char punctuation, @NotNull Memory m) {
        this(1.0f, m.confidenceDefault(punctuation));
    }


    public DefaultTruth(@NotNull Truth truth) {
        this(truth.freq(), truth.conf());
    }

    @Nullable
    @Override
    public Truth confMult(float factor) {
        return withConf(conf() * factor);
    }

    @Nullable
    @Override public final Truth withConf(float newConf) {
//        if (newConf < Global.TRUTH_EPSILON)
//            return null;
        //return !Util.equals(conf, newConf, Global.TRUTH_EPSILON) ? new DefaultTruth(freq, newConf) : this;
        return new DefaultTruth(freq, newConf);
    }

    @NotNull
    @Override
    public String toString() {
        //return DELIMITER + frequency.toString() + SEPARATOR + confidence.toString() + DELIMITER;

        //1 + 6 + 1 + 6 + 1
        return toCharSequence().toString();
    }

    @Override
    public final boolean equals(@NotNull Object that) {

        //if (that instanceof DefaultTruth) {

            //return ((DefaultTruth)that).hash == hash; //shortcut, since perfect hash for this instance
        /*} else */if (that instanceof Truth) {
            //Truth t = (Truth)that;
            //return freq == t.freq() && conf == t.conf();
            return hashCode() == that.hashCode();
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Truth.hash(freq, conf, Global.TRUTH_DISCRETION);
        //return hash;
    }

    @NotNull
    @Override
    public final DefaultTruth negated() {
        //float fPos = freq;

        //if = 0.5, negating will produce same result
        //return Util.equals(fPos, 0.5f, Global.TRUTH_EPSILON) ? this :

        return new DefaultTruth(1.0f - freq, conf);
    }

    protected boolean equalsFrequency(@NotNull Truth t) {
        return (Util.equals(freq, t.freq(), Global.TRUTH_EPSILON));
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

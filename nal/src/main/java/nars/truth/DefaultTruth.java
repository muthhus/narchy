package nars.truth;

import jcog.Util;
import nars.NAR;
import nars.Param;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import static jcog.Util.*;


public class DefaultTruth implements Truth  {

    public final float freq, conf;

    @Override
    public final float freq() {
        return freq;
    }

    @Override
    public final float conf() {
        return conf;
    }

    public DefaultTruth(float f, float c) {
        this(f, c, Param.TRUTH_EPSILON);
    }

    public DefaultTruth(float f, float c, float epsilon) {
        float C = Truth.conf(c, epsilon);
        assert(C > 0);
        this.freq = Truth.freq(f, epsilon);
        this.conf = C;
    }


    public DefaultTruth(char punctuation, @NotNull NAR m) {
        this(1.0f, m.confidenceDefault(punctuation));
    }


    public DefaultTruth(@NotNull Truth truth) {
        this(truth.freq(), truth.conf());
    }


    @Nullable
    @Override public final Truth withConf(float newConf) {
        return new DefaultTruth(freq, newConf);
    }

    @NotNull
    @Override
    public String toString() {
        //return DELIMITER + frequency.toString() + SEPARATOR + confidence.toString() + DELIMITER;

        //1 + 6 + 1 + 6 + 1
        try {
            return appendString(new StringBuilder(7)).toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        return Truth.truthToInt(freq, conf);
    }

    @NotNull
    @Override
    public final DefaultTruth negated() {
        //float fPos = freq;

        //if = 0.5, negating will produce same result
        //return Util.equals(fPos, 0.5f, Global.TRUTH_EPSILON) ? this :

        return new DefaultTruth(1.0f - freq, conf);
    }

//    protected boolean equalsFrequency(@NotNull Truth t) {
//        return (Util.equals(freq, t.freq(), Param.TRUTH_EPSILON));
//    }
//
//    private static final int hashDiscreteness = (int)(1.0f / Param.TRUTH_EPSILON);



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

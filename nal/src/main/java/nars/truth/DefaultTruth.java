package nars.truth;

import nars.NAR;
import nars.Param;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class DefaultTruth implements Truth {

    public final float freq, conf;
    private final int hash;

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
        this.hash = Truth.truthToInt(
            this.freq = Truth.freq(f, epsilon),
            this.conf = Truth.conf(c, epsilon)
        );
    }


    @Nullable
    @Override
    public final Truth withConf(float newConf) {
        return new DefaultTruth(freq, newConf);
    }

    @NotNull
    @Override
    public String toString() {
        //return DELIMITER + frequency.toString() + SEPARATOR + confidence.toString() + DELIMITER;

        //1 + 6 + 1 + 6 + 1
        return appendString(new StringBuilder(7)).toString();

    }

    @Override
    public final boolean equals(Object that) {
        return (that instanceof Truth) && (hash == that.hashCode());
    }

    @Override
    public final int hashCode() {
        return hash;
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

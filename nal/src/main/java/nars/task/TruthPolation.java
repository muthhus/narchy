package nars.task;

import com.google.common.collect.Lists;
import nars.Task;
import nars.truth.PreciseTruth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static nars.truth.TruthFunctions.w2c;

/**
 * Truth Interpolation and Extrapolation of Temporal Beliefs/Goals
 * see:
 * https://en.wikipedia.org/wiki/Category:Intertemporal_economics
 * https://en.wikipedia.org/wiki/Discounted_utility
 */
public enum TruthPolation {
    ;


    /**
     * dt > 0
     */
    public static float evidenceDecay(float evi, int dur, long dt) {

        //hard linear with half duration on either side of the task -> sum to 1.0 duration
//        float scale = ((float)dt) / dur;
//        if (scale > 0.5f) return 0;
//        else return evi * (1f - scale*2f);


        return evi / (1 + (((float) dt) / dur)); //inverse linear

        //return evi / ( 1f + sqr( ((float)dt)/dur) ); //inverse square

        //return evi / (1 + (((float) Math.log(1+dt)) / dur)); //inverse log

        //return evi * 1f/( 1 + 2 * (dt/dur) ); //inverse linear * 2 (nyquist recovery period)



        //return evi / (1f + dt / dur ); //first order decay
        //return evi / (1f + (dt*dt) / (dur*dur) ); //2nd order decay

    }

    @Nullable
    public static PreciseTruth truth(long when, int dur, @NotNull Task... tasks) {
        return truth(null, when, dur, Lists.newArrayList(tasks));
    }

    public static float eternalize(float conf) {
        return w2c(conf);
    }

    /**
     * computes truth at a given time from iterative task samples
     * includes variance calculation for reduction of evidence in proportion to confusion/conflict
     * uses "waldorf method" to calculate a running variance
     * additionally, the variance is weighted by the contributor's confidences
     */
    public static class TruthPolationBasic implements Consumer<Task> {
        float eviSum = 0, wFreqSum = 0;
        final long when;
        final int dur;

        public TruthPolationBasic(long when, int dur) {
            this.when = when;
            this.dur = dur;
        }

        @Override
        public void accept(Task t) {
            float tw = t.evi(when, dur);
            if (tw > 0) {
                eviSum += tw;

                float f = t.freq();
                wFreqSum += tw * f;
            }

        }


        public PreciseTruth truth() {
            if (eviSum > 0) {
                float f = wFreqSum / eviSum;
                return new PreciseTruth(f, eviSum, false);

            } else {
                return null;
            }

        }
    }

    /**
     * computes truth at a given time from iterative task samples
     * includes variance calculation for reduction of evidence in proportion to confusion/conflict
     * uses "waldorf method" to calculate a running variance
     * additionally, the variance is weighted by the contributor's confidences
     */
    public static class TruthPolationWithVariance implements Consumer<Task> {
        float eviSum = 0, wFreqSum = 0;
        float meanSum = 0.5f, deltaSum = 0;
        int count = 0;

        final long when;
        final int dur;

        public TruthPolationWithVariance(long when, int dur) {
            this.when = when;
            this.dur = dur;
        }

        @Override
        public void accept(Task t) {
             float tw = t.evi(when, dur);

            if (tw > 0) {
                eviSum += tw;

                float f = t.freq();
                wFreqSum += tw * f;

                //        double delta = value - tmpMean;
                //        mean += delta / ++count;
                //        sSum += delta * (value - mean);
                float tmpMean = meanSum;
                float delta = f - tmpMean;
                meanSum += delta / ++count;
                deltaSum += delta * (f - meanSum) * w2c(tw); //scale the delta sum by the conf so that not all tasks contribute to the variation equally
            }

        }


        public PreciseTruth truth() {
            if (eviSum > 0) {
                float f = wFreqSum / eviSum;

                float var =
                        deltaSum / count;

                return new PreciseTruth(f, eviSum * (1f / (1f + var)), false);

            } else {
                return null;
            }

        }
    }

    @Nullable
    public static PreciseTruth truth(@Nullable Task topEternal, long when, int dur, @NotNull Iterable<Task> tasks) {

        assert(dur>0);

        TruthPolationWithVariance t =
                //new TruthPolationBasic(when, dur);
                new TruthPolationWithVariance(when, dur);

        // Contribution of each task's truth
        // use forEach instance of the iterator(), since HijackBag forEach should be cheaper
        tasks.forEach(t);
        if (topEternal != null) {
            t.accept(topEternal);
        }

        return t.truth();
    }

//    /**
//     * returns (freq, evid) pair
//     */
//    @Nullable
//    public static PreciseTruth truthRaw(@Nullable Task topEternal, long when, int dur, @NotNull Iterable<Task> tasks) {
//
//        float[] fe = new float[2];
//
//
//        // Contribution of each task's truth
//        // use forEach instance of the iterator(), since HijackBag forEach should be cheaper
//        tasks.forEach(t -> {
//
//            float tw = t.evi(when, dur);
//
//            if (tw > 0) {
//                freqSum += tw;
//                wFreqSum += tw * t.freq();
//            }
//
//        });
//        float evidence = freqSum;
//        float freqEvi = wFreqSum;
//
//        if (topEternal != null) {
//            float ew = topEternal.evi();
//            evidence += ew;
//            freqEvi += ew * topEternal.freq();
//        }
//
//        if (evidence > 0) {
//            float f = freqEvi / evidence;
//            return new PreciseTruth(f, evidence, false);
//        } else {
//            return null;
//        }
//    }

}

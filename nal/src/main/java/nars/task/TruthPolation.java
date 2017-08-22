package nars.task;

import jcog.Util;
import jcog.decide.DecideSoftmax;
import nars.Task;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static jcog.Util.sqr;
import static nars.truth.TruthFunctions.w2c;

/**
 * Truth Interpolation and Extrapolation of Temporal Beliefs/Goals
 * see:
 * https://en.wikipedia.org/wiki/Category:Intertemporal_economics
 * https://en.wikipedia.org/wiki/Discounted_utility
 */
public enum TruthPolation { ;


    /**
     * dt > 0
     */
    public static float evidenceDecay(float evi, float dur, long dt) {

        //hard linear with half duration on either side of the task -> sum to 1.0 duration
//        float scale = dt / dur;
//        if (scale > 0.5f) return 0;
//        else return evi * (1f - scale*2f);


        //return evi / (1 + (((float) Math.log(1+dt)) / dur)); //inverse log

        return evi / (1 + ( dt / dur)); //inverse linear

        //return evi / ( sqr( 1 + dt/dur) ); //inverse square

        //return evi /( 1 + 2 * (dt/dur) ); //inverse linear * 2 (nyquist recovery period)


        //return evi / (1f + dt / dur ); //first order decay
        //return evi / (1f + (dt*dt) / (dur*dur) ); //2nd order decay

    }

    /**
     * computes truth at a given time from iterative task samples
     * includes variance calculation for reduction of evidence in proportion to confusion/conflict
     * uses "waldorf method" to calculate a running variance
     * additionally, the variance is weighted by the contributor's confidences
     */
    public static class TruthPolationBasic implements Consumer<Tasked> {
        float eviSum, wFreqSum;
        final long start, end;
        final int dur;

        public TruthPolationBasic(long start, long end, int dur) {
            this.start = start;
            this.end = end;
            this.dur = dur;
        }

        @Override
        public void accept(Tasked t) {
            Task task = t.task();
            float tw = task.evi(start, end, dur);
            if (tw > 0) {
                eviSum += tw;
                wFreqSum += tw * task.freq();
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

    public static class TruthPolationGreedy implements Consumer<Tasked> {

        final long when;
        final int dur;
        Truth best;

        public TruthPolationGreedy(long when, int dur) {
            this.when = when;
            this.dur = dur;
        }

        @Override
        public void accept(Tasked t) {
            Task task = t.task();
            Truth tt = task.truth(when,dur);
            if (best==null || best.conf() < tt.conf())
                best = tt;
        }


        public PreciseTruth truth() {
            if (best == null)
                return null;

            return new PreciseTruth(best.freq(), best.conf());
        }
    }

    public static class TruthPolationSoftMax implements Consumer<Tasked> {

        final long when;
        final int dur;
        final FloatArrayList freq = new FloatArrayList();
        final FloatArrayList conf = new FloatArrayList();

        public TruthPolationSoftMax(long when, int dur) {
            this.when = when;
            this.dur = dur;
        }

        @Override
        public void accept(Tasked t) {
            Task task = t.task();
            conf.add(task.conf(when, dur)); //TODO start,end
            freq.add(task.freq());
        }


        public PreciseTruth truth() {
            if (!conf.isEmpty()) {
                int which = new DecideSoftmax(0f, ThreadLocalRandom.current()).decide(conf.toArray(), -1);
                float f = freq.get(which);
                float c = conf.get(which);
                return new PreciseTruth(f, c);

            } else {
                return null;
            }

        }
    }
    public static class TruthPolationRoulette implements Consumer<Tasked> {

        final long when;
        final int dur;
        final FloatArrayList freq = new FloatArrayList();
        final FloatArrayList evi = new FloatArrayList();

        public TruthPolationRoulette(long when, int dur) {
            this.when = when;
            this.dur = dur;
        }

        @Override
        public void accept(Tasked t) {
            Task task = t.task();
            evi.add(task.evi(when, dur));
            freq.add(task.freq());
        }


        public PreciseTruth truth() {
            if (!evi.isEmpty()) {
                int which = Util.decideRoulette(freq.size(), evi::get, ThreadLocalRandom.current());
                float f = freq.get(which);
                float e = evi.get(which);
                return new PreciseTruth(f, e, false);

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
    public static class TruthPolationWithVariance implements Consumer<Tasked> {
        float eviSum, wFreqSum;
        float meanSum = 0.5f, deltaSum;
        int count;

        final long when;
        final int dur;

        public TruthPolationWithVariance(long when, int dur) {
            this.when = when;
            this.dur = dur;
        }

        @Override
        public void accept(Tasked tt) {
            Task task = tt.task();
            float tw = task.evi(when, dur);

            if (tw > 0) {

                if (!task.isEternal())
                    tw = tw/(1f + ((float)task.range())/dur); //dilute the long task in proportion to how many durations it consumes beyond point-like (=0)

                eviSum += tw;

                float f = task.freq();
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
    public static PreciseTruth truth(@Nullable Task topEternal, long start, long end, int dur, @NotNull Iterable<? extends Tasked> tasks) {

        assert (dur > 0);

        TruthPolationBasic t =
                new TruthPolationBasic(start, end, dur);
                //new TruthPolationGreedy(when, dur);
                //new TruthPolationRoulette(when, dur);
                //new TruthPolationWithVariance(when, dur);

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

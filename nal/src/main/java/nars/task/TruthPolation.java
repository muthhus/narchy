package nars.task;

import com.google.common.collect.Lists;
import nars.Task;
import nars.truth.PreciseTruth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        float scale = dt / dur;
        if (scale > 0.5f) return 0;
        else return evi * (1f - scale*2f);


        //return evi / (1 + (((float) dt) / dur)); //inverse linear
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


    /**
     * returns (freq, evid) pair
     */
    @Nullable
    public static PreciseTruth truth(@Nullable Task topEternal, long when, int dur, @NotNull Iterable<Task> tasks) {

        float[] fe = new float[2];


        // Contribution of each task's truth
        // use forEach instance of the iterator(), since HijackBag forEach should be cheaper
        tasks.forEach(t -> {

            float tw = t.evi(when, dur);

            if (tw > 0) {
                fe[0] += tw;
                fe[1] += tw * t.freq();
            }

        });
        float evidence = fe[0];
        float freqEvi = fe[1];

        if (topEternal != null) {
            float ew = topEternal.evi();
            evidence += ew;
            freqEvi += ew * topEternal.freq();
        }

        if (evidence > 0) {
            float f = freqEvi / evidence;
            return new PreciseTruth(f, evidence, false);
        } else {
            return null;
        }
    }

}

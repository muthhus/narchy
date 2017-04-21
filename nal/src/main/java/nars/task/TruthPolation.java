package nars.task;

import com.google.common.collect.Lists;
import nars.$;
import nars.Task;
import nars.concept.dynamic.DynamicBeliefTask;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jcog.Util.sqr;
import static nars.truth.TruthFunctions.w2c;

/**
 * Truth Interpolation and Extrapolation of Temporal Beliefs/Goals
 * see:
 *  https://en.wikipedia.org/wiki/Category:Intertemporal_economics
 *  https://en.wikipedia.org/wiki/Discounted_utility
 */
public enum TruthPolation  {
    ;

    /** dt > 0 */
    public static float evidenceDecay(float evi, int dur, long dt) {

        //return evi / ( 1f + sqr( ((float)dt)/dur) ); //inverse square
        return evi / ( 1 + (((float)dt)/dur) ); //inverse linear

        //return evi * 1f/( 1 + 2 * (dt/dur) ); //inverse linear * 2 (nyquist recovery period)

        //return evi * Math.max(0, 1f - dt / dur ); //hard linear

        //return evi / (1f + dt / dur ); //first order decay
        //return evi / (1f + (dt*dt) / (dur*dur) ); //2nd order decay

    }

    @Nullable
    public static Truth truth(long when, int dur, @NotNull Task... tasks) {
        return truth(null, when, dur, Lists.newArrayList(tasks));
    }

    @Nullable
    public static Truth truth(@Nullable Task topEternal, long when, int dur, @NotNull Iterable<Task> tasks) {

        float[] illWei = new float[2];
        // Contribution of each sample point to the illumination of the
        // microsphere's facets.
        // use forEach instance of the iterator(), since HijackBag forEach should be cheaper
        tasks.forEach(t -> {
            if (t instanceof DynamicBeliefTask)
                return; //ignore dynamic belief tasks

            float tw = t.evi(when, dur);
            if (tw > 0) {
                illWei[0] += tw;
                illWei[1] += tw * t.freq();
            }
        });
        float illumination = illWei[0];
        float weightedValue = illWei[1];

        if (topEternal!=null) {
            float ew = topEternal.evi();
            illumination += ew;
            weightedValue += ew * topEternal.freq();
        }

        float f = weightedValue / illumination;
        float c = w2c(illumination);

        return $.t(f, c);
    }

}

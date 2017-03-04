package nars.task;

import com.google.common.collect.Lists;
import nars.$;
import nars.Task;
import nars.concept.dynamic.DynamicBeliefTask;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static jcog.Util.sqr;
import static nars.Param.TRUTH_EPSILON;
import static nars.truth.TruthFunctions.c2w;
import static nars.truth.TruthFunctions.w2c;

/**
 * Truth Interpolation and Extrapolation of Temporal Beliefs/Goals
 */
public enum TruthPolation  {
    ;

    /** dt > 0 */
    public static float evidenceDecay(float evi, float dur, float dt) {
        return evi * 1f/( sqr( 1 + dt/dur) ); //inverse square
        //return evi * 1f/( 1f + sqr( dt/dur) ); //inverse square
        //return evi * Math.max(0, 1f - dt / dur ); //hard linear

        //return evi * 1f/( 1 + (dt/dur) ); //inverse
        //return evi / (1f + dt / dur ); //first order decay
        //return evi / (1f + (dt*dt) / (dur*dur) ); //2nd order decay

    }

    final static float MIN_ILLUMINATION = c2w(TRUTH_EPSILON);

    @Nullable
    public static Truth truth(long when, float dur, @NotNull Task... tasks) {
        return truth(null, when, dur, Lists.newArrayList(tasks));
    }

    @Nullable
    public static Truth truth(@Nullable Task topEternal, long when, float dur, @NotNull List<Task> tasks) {

        float weightedValue = 0, illumination = 0;

        int tasksSize = tasks.size();
        if (tasksSize > 0) {

            // Contribution of each sample point to the illumination of the
            // microsphere's facets.
            for (int i1 = 0; i1 < tasksSize; i1++) {
                Task t = tasks.get(i1);
                if (t instanceof DynamicBeliefTask)
                    continue; //ignore dynamic belief tasks

                float tw = t.confWeight(when, dur);
                if (tw > 0) {
                    illumination += tw;
                    weightedValue += tw * t.freq();
                }
            }
        }

        if (topEternal!=null) {
            float ew = topEternal.evi();
            illumination += ew;
            weightedValue += ew * topEternal.freq();
        }

        if (illumination < MIN_ILLUMINATION)
            return null;

        float f = weightedValue / illumination;
        float c = w2c(illumination);

        return $.t(f, c);
    }

}

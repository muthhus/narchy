package nars.task.util;

import jcog.pri.Pri;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.MetaGoal;
import nars.table.BeliefTable;
import nars.task.SignalTask;
import org.jetbrains.annotations.Nullable;

public class PredictionAccuracyFeedback {

    long last = Long.MIN_VALUE;
    final BeliefTable table;
    static final boolean deleteAny = false; //

    public PredictionAccuracyFeedback(BeliefTable table) {
        this.table = table;
    }

    public void accept(@Nullable Task x, NAR nar) {
        if (x == null)
            return;

        long now = x.end();
        feedback(x, Param.DELETE_INACCURATE_PREDICTIONS /* TODO make this adjustable threshold */, now, nar);
        this.last = now;
    }

    /** TODO handle stretched tasks */
    void feedback(Task x, boolean deleteIfIncorrect, long now, NAR nar) {
        float xFreq = x.freq();

        int dur = nar.dur();
        float xEvi = x.evi(now, dur);

        float strength = 1;
        long last = this.last;

        //sensor feedback
        //punish any non-signal beliefs at the current time which contradict this sensor reading, and reward those which it supports
        table.forEachTask(false, last, now, (y) -> {

            if (y instanceof SignalTask)
                return; //ignore previous signaltask

            short[] cause = y.cause();
            if (cause.length == 0)
                return;

            //only tasks created before now
            long leadTime = y.start() - y.creation();
            if (leadTime < 0)
                return;

            float headstart = 1f + leadTime/(1f+y.range()) / ((float)dur); //divide by range because it must be specific

            float coherence = 1f -
                    Math.abs(xFreq - y.freq());
                    //TruthFunctions.freqSimilarity(xFreq, y.freq());

            float yEvi = y.evi(now, dur);
            float confFraction = 2f * yEvi / (yEvi + xEvi); //allow > 1

            /** durations ago since the prediction was created */

            float v;
            if (coherence > 0.5f) {
                //reward
                v = (coherence - 0.5f) * 2f * confFraction * headstart * strength;
                if (v > Pri.EPSILON)
                    nar.emotion.value(MetaGoal.Accurate, cause, v);
            } else {
                //punish
                v = (0.5f - coherence) * 2f * confFraction * (1f - coherence) / headstart * strength;
                if (v > Pri.EPSILON) {
                    nar.emotion.value(MetaGoal.Inaccurate, cause, v);
                    if (deleteIfIncorrect)
                        y.delete();
                }
            }

        });
    }

}

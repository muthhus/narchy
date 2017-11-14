package nars.task.util;

import jcog.list.FasterList;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.MetaGoal;
import nars.table.BeliefTable;
import nars.task.NALTask;
import nars.task.SignalTask;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PredictionFeedback {

    final BeliefTable table;
    static final boolean deleteAny = false; //

    static final float REWARD_PUNISH_COHERENCE_THRESHOLD = 0.9f;
    float strength = 1;

    public PredictionFeedback(BeliefTable table) {
        this.table = table;
    }

    public void accept(@Nullable Task x, NAR nar) {
        if (x == null)
            return;

        if (x instanceof SignalTask) {
            feedbackNewSignal((SignalTask) x, Param.DELETE_INACCURATE_PREDICTIONS /* TODO make this adjustable threshold */, nar);
        } else {
            feedbackNewBelief(x, Param.DELETE_INACCURATE_PREDICTIONS /* TODO make this adjustable threshold */, nar);
        }
    }

    /**
     * TODO handle stretched tasks
     */
    void feedbackNewBelief(Task y, boolean deleteIfIncoherent, NAR nar) {

        long when = y.nearestTimeTo(nar.time());
        Truth x = table.truth(when, nar);
        if (x == null)
            return; //nothing to compare it with

        int dur = nar.dur();

        long start = y.start();
        long end = y.end();
        float yConf = y.conf(when, dur);
        float yFreq = y.freq();


        final boolean[] signalTaskPresent = {false};
        table.forEachTask(false, start, end, (xt) -> {
            if (xt instanceof SignalTask) {
                //only looking for SignalTask's that would invalidate this incoming
                signalTaskPresent[0] = true; //at least one signal task contributes to the measured value
            }
            //TODO early exit with a Predicate form of this query method
        });
        if (!signalTaskPresent[0])
            return; //just beliefs against beliefs

        float coherence = freqCoherence(x.freq(), y.freq());
        float confFraction = yConf/x.conf();

        if (coherence >= REWARD_PUNISH_COHERENCE_THRESHOLD) {
            reward(null, strength, y, nar, coherence, confFraction);
        } else {
            punish(deleteIfIncoherent, strength, null, y, nar, coherence, confFraction);
        }
    }

    /**
     * punish any held non-signal beliefs during the current signal task which has just been input.
     * time which contradict this sensor reading, and reward those which it supports
     */
    void feedbackNewSignal(SignalTask x, boolean deleteIfIncoherent, NAR nar) {

        int dur = nar.dur();

        long start = x.start();
        long end = x.end();

        float xConf = x.conf(x.nearestTimeTo(nar.time()), dur);
        float xFreq = x.freq();

        List<Task> trash = new FasterList();

        table.forEachTask(false, start, end, (y) -> {

            if (y instanceof SignalTask)
                return; //ignore previous signaltask

            float coherence = freqCoherence(xFreq, y.freq());
            float confFraction = y.conf(y.nearestTimeBetween(start, end), dur) / xConf;

            if (coherence >= REWARD_PUNISH_COHERENCE_THRESHOLD) {
                reward(x, strength, y, nar, coherence, confFraction);
            } else {
                punish(deleteIfIncoherent, strength, trash, y, nar, coherence, confFraction);
            }

        });

        trash.forEach(y -> {
            ((NALTask) y).delete(x); //forward to the actual sensor reading
            table.removeTask(y);
        });
    }

    /**
     * measures similarity of two frequencies. 0 = dissimilar, 1 = similar
     */
    static float freqCoherence(float xFreq, float yFreq) {
        return 1f - Math.abs(xFreq - yFreq);
        //TruthFunctions.freqSimilarity(xFreq, y.freq());

    }

    private void punish(boolean deleteIfIncoherent, float strength, List<Task> trash, Task y, NAR nar, float coherence, float confFraction) {
        float v = (1f - coherence) * 2f * confFraction /* * (1/headstart) */ * strength;

        if (deleteIfIncoherent) {

            if (trash != null) {
                trash.add(y);
            } else {
                y.delete(); //just delete it if in pre-filter non-deferred trash mode
            }

        } else {
            y.setPri(0); //drain priority  TODO maybe transfer it to nearby tasks?
        }

        MetaGoal.learn(MetaGoal.Inaccurate, y.cause(), v, nar);
    }

    private void reward(SignalTask x, float strength, Task y, NAR nar, float coherence, float confFraction) {
        float v = coherence * 2f * confFraction /* * headstart */ * strength;

        MetaGoal.learn(MetaGoal.Accurate, y.cause(), v, nar);

        if (x!=null)
            y.meta("@", x); //in case the task gets deleted, the link will point to the sensor value

    }

}

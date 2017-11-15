package nars.task.util;

import jcog.list.FasterList;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.MetaGoal;
import nars.table.BeliefTable;
import nars.table.DefaultBeliefTable;
import nars.task.NALTask;
import nars.task.SignalTask;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PredictionFeedback {

    final BeliefTable table;


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

        long start = y.start();
        long end = y.end();

        final SignalTask[] strongestSignal = new SignalTask[1];
        ((DefaultBeliefTable)table).temporal.whileEach(start, end, (xt) -> {
            if (xt instanceof SignalTask) {
                //only looking for SignalTask's that would invalidate this incoming
                strongestSignal[0] = ((SignalTask)xt);
                return false;
            }
            return true;
            //TODO early exit with a Predicate form of this query method
        });
        SignalTask signal = strongestSignal[0];
        if (signal==null)
            return; //just beliefs against beliefs

        long when = y.nearestTimeTo(nar.time());
        int dur = nar.dur();
        Truth x = signal.truth(when, dur);
        if (x == null)
            return; //nothing to compare it with

        float yConf = y.conf(when, dur);

        float coherence = coherence(signal, y);
        float confFraction = yConf/x.conf();
        if (confFraction > 1f)
            return; //prediction stronger than the sensor value

        absorb(signal, y, nar, coherence, confFraction);

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

        List<Task> trash = new FasterList(0);
        ((DefaultBeliefTable)table).temporal.whileEach(start, end, (y) -> {

            if (y instanceof SignalTask)
                return true; //ignore previous signaltask

            float confFraction = y.conf(y.nearestTimeBetween(start, end), dur) / xConf;
            if (confFraction > 1f)
                return true; //prediction stronger than the sensor value

            float coherence = coherence(x, y);

            absorb(x, y, nar, coherence, confFraction);
            trash.add(y);
            return true; //continue
        });

        trash.forEach(table::removeTask);
    }

    /**
     * measures similarity of two frequencies. 0 = dissimilar, 1 = similar
     */
    static float coherence(Task actual, Task predict) {
        float xFreq = actual.freq();
        float yFreq = predict.freq();
        float overtime = Math.max(0, predict.range() - actual.range()); //penalize predictions spanning longer than the actual signal because we aren't checking in that time range for accuracy, it could be wrong before and after the signal
        return 1f - Math.abs(xFreq - yFreq) / (1f + overtime);
        //TruthFunctions.freqSimilarity(xFreq, y.freq());
    }


    /** rewards/punishes the causes of this task,
     *  then removes it in favor of a stronger sensor signal  */
    private void absorb(SignalTask x, Task y, NAR nar, float coherence, float confFraction) {
        float v = (coherence - 0.5f) * 2f * confFraction /* * headstart */ * strength;

        MetaGoal.learn(MetaGoal.Accurate, y.cause(), v, nar);

        ((NALTask)y).delete(x); //forward to the actual sensor reading
    }

}

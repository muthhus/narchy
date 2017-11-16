package nars.task.util;

import jcog.list.FasterList;
import nars.NAR;
import nars.Task;
import nars.control.MetaGoal;
import nars.table.BeliefTable;
import nars.table.DefaultBeliefTable;
import nars.task.NALTask;
import nars.task.SignalTask;
import nars.truth.Truth;

import java.util.List;

public class PredictionFeedback {

    //final BeliefTable table;


    static final float strength = 1;

    /*public PredictionFeedback(BeliefTable table) {
        this.table = table;
    }*/

    static public void accept(Task x, BeliefTable table, NAR nar) {
        if (x == null)
            return;

        if (x instanceof SignalTask) {
            feedbackNewSignal((SignalTask) x, table, nar);
        } else {
            feedbackNewBelief(x, table, nar);
        }
    }

    /**
     * TODO handle stretched tasks
     */
    static void feedbackNewBelief(Task y, BeliefTable table, NAR nar) {

        long start = y.start();
        long end = y.end();

        final SignalTask[] strongestSignal = new SignalTask[1];
        ((DefaultBeliefTable) table).temporal.whileEach(start, end, (xt) -> {
            if (xt instanceof SignalTask) {
                //only looking for SignalTask's that would invalidate this incoming
                strongestSignal[0] = ((SignalTask) xt);
                return false;
            }
            return true;
            //TODO early exit with a Predicate form of this query method
        });
        SignalTask signal = strongestSignal[0];
        if (signal == null)
            return; //just beliefs against beliefs

        long when = y.nearestTimeTo(nar.time());
        int dur = nar.dur();
        Truth x = signal.truth(when, dur);
        if (x == null)
            return; //nothing to compare it with

        absorb(signal, y, nar);
    }

    /**
     * punish any held non-signal beliefs during the current signal task which has just been input.
     * time which contradict this sensor reading, and reward those which it supports
     */
    static void feedbackNewSignal(SignalTask signal, BeliefTable table, NAR nar) {

        int dur = nar.dur();

        long start = signal.start();
        long end = signal.end();

        float xConf = signal.conf(signal.nearestTimeTo(nar.time()), dur);

        List<Task> trash = new FasterList(0);
        ((DefaultBeliefTable) table).temporal.whileEach(start, end, (y) -> {

            if (y instanceof SignalTask)
                return true; //ignore previous signaltask

            if (absorb(signal, y, nar))
                trash.add(y);

            return true; //continue
        });

        trash.forEach(table::removeTask);
    }

    /**
     * measures similarity of two frequencies. 0 = dissimilar, 1 = similar
     */
    public static float coherence(Task actual, Task predict) {
        float xFreq = actual.freq();
        float yFreq = predict.freq();
        float overtime = Math.max(0, predict.range() - actual.range()); //penalize predictions spanning longer than the actual signal because we aren't checking in that time range for accuracy, it could be wrong before and after the signal
        return 1f - Math.abs(xFreq - yFreq) / (1f + overtime);
        //TruthFunctions.freqSimilarity(xFreq, y.freq());
    }


    /**
     * rewards/punishes the causes of this task,
     * then removes it in favor of a stronger sensor signal
     * returns whether the 'y' task was absorbed into 'x'
     */
    public static boolean absorb(Task x, Task y, NAR nar) {
        if (x == y)
            return false;

        //maybe also factor originality to prefer input even if conf is lower but has more originality thus less chance for overlap
        float confContention = y.conf(x.start(), x.end(), nar.dur()) * x.conf();
        float coherence = coherence(x, y);
        float value = (coherence - 0.5f) * 2f * confContention /* * headstart */ * strength;

        MetaGoal.learn(MetaGoal.Accurate, y.cause(), value, nar);

        if (confContention <= 1f) {
            ((NALTask) y).delete(x); //forward to the actual sensor reading
            return true;
        } else {
            return false;
        }
    }

}

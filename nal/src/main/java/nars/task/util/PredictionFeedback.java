package nars.task.util;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.MetaGoal;
import nars.table.BeliefTable;
import nars.task.SignalTask;
import org.jetbrains.annotations.Nullable;

public class PredictionFeedback {

    final BeliefTable table;
    static final boolean deleteAny = false; //

    static final float REWARD_PUNISH_COHERENCE_THRESHOLD = 0.75f;

    public PredictionFeedback(BeliefTable table) {
        this.table = table;
    }

    public void accept(@Nullable Task x, NAR nar) {
        if (x == null)
            return;



        feedback(x, Param.DELETE_INACCURATE_PREDICTIONS /* TODO make this adjustable threshold */, nar);

    }

    /** TODO handle stretched tasks */
    void feedback(Task x, boolean deleteIfIncoherent, NAR nar) {

        int dur = nar.dur();

        long start = x.start();
        long end = x.end();
        if (start == end)
            return; //no time in which to test


        float xConf = x.conf(start, dur);

        float strength = 1;

        float xFreq = x.freq();

        //sensor feedback
        //punish any non-signal beliefs at the current time which contradict this sensor reading, and reward those which it supports
        table.forEachTask(false, start, end, (y) -> {

            if (y instanceof SignalTask)
                return; //ignore previous signaltask


            //only tasks created before now
            long leadTime = y.start() - y.creation();
            if (leadTime < 0)
                return;

//            float yConf = y.conf(now, dur);
//            if (yConf!=yConf)
//                return;

//            float headstart = 1f + (1f+leadTime)/(1f+y.range()) / dur; //divide by range because it must be specific

            float coherence = 1f - Math.abs(xFreq - y.freq());
                    //TruthFunctions.freqSimilarity(xFreq, y.freq());

            float confFraction = (y.conf() / xConf);

            /** durations ago since the prediction was created */

            float v;

            if (coherence >= REWARD_PUNISH_COHERENCE_THRESHOLD) {

                //reward
                v = coherence * 2f * confFraction /* * headstart */ * strength;

                MetaGoal.learn(MetaGoal.Accurate, y.cause(), v, nar);

            } else {
                //punish
                v = (1f - coherence) * 2f * confFraction /* * (1/headstart) */ * strength;

                MetaGoal.learn(MetaGoal.Inaccurate, y.cause(), v, nar);

                if (deleteIfIncoherent)
                    y.delete();
                else
                    y.setPri(0); //drain priority  TODO maybe transfer it to nearby tasks?
            }

        });
    }

}

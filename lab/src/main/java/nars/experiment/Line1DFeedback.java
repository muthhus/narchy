package nars.experiment;

import nars.NAgentX;
import nars.Narsese;
import nars.nar.Default;
import nars.test.agent.Line1DSimplest;
import nars.time.CycleTime;
import nars.util.exe.FeedbackSynchronousExecutor;

public class Line1DFeedback {

    public static void main(String[] args) throws Narsese.NarseseException {
        FeedbackSynchronousExecutor exe = new FeedbackSynchronousExecutor();
        Default n = new Default(128, new Default.DefaultTermIndex(128), new CycleTime(),
                exe);
        exe.goalAdd(exe.classify(n.task("(o-->L)?")), -0.5f );
        exe.goalAdd(exe.classify(n.task("(o-->L). %1.0;0.1%")), -0.25f );
        exe.goalAdd(exe.classify(n.task("(o-->L)! %1.0;0.1%")));
        exe.goalAdd(exe.classify(n.task("(o-->L)! %0.0;0.1%")));
        exe.goalAdd(exe.classify(n.task("(o-->L)! %1.0;0.9%")));
        exe.goalAdd(exe.classify(n.task("(o-->L)! %0.0;0.9%")));
        exe.goalAdd(exe.classify(n.task("(o-->L)! %1.0;0.5%")));
        exe.goalAdd(exe.classify(n.task("(o-->L)! %0.0;0.5%")));
        exe.goalNormalize();

        //n.log();

        n.time.dur(4);
        //n.deriver.rate.setValue(1f);

        n.termVolumeMax.setValue(16);
        n.DEFAULT_BELIEF_PRIORITY = 0.5f;
        n.DEFAULT_GOAL_PRIORITY = 0.75f;
        n.DEFAULT_QUESTION_PRIORITY = 0.25f;
        n.DEFAULT_QUEST_PRIORITY = 0.25f;

        Line1DSimplest a = new Line1DSimplest(n);

        final int[] frame = {0};
        a.onFrame((z) -> {
            a.target(
                    (float) (0.5f * (Math.sin(n.time() / 1000f) + 1f))
                    //Util.sqr((float) (0.5f * (Math.sin(n.time()/90f) + 1f)))
                    //(0.5f * (Math.sin(n.time()/90f) + 1f)) > 0.5f ? 1f : 0f
            );

            if (frame[0]++ % 4 == 0) {
                exe.updateFilter();
                System.out.println("saved: " + exe.saving);
                exe.saving.clear();
            }
        });
        NAgentX.chart(a);

        a.runCycles(1000);

        n.log();

        a.runCycles(10000);
    }
}

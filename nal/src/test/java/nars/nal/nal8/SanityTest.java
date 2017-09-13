package nars.nal.nal8;

import nars.*;
import nars.concept.GoalActionConcept;
import nars.task.DerivedTask;
import org.junit.Test;

import java.util.SortedSet;
import java.util.TreeSet;

import static jcog.Texts.n4;

public class SanityTest {

    @Test
    public void testSanity1() {
        Param.DEBUG = true;

        NAR n = NARS.tmp();
        final int[] togglesPos = {0};
        final int[] togglesNeg = {0};
        NAgent a = new NAgent(n) {

            public final GoalActionConcept must;
            public boolean on;

            {
                must = (GoalActionConcept) actionToggle($.the("mustBeOn"), (b) -> {
                    (b ? togglesPos : togglesNeg)[0]++;
                    on = b;
                });

            }

            @Override
            protected float act() {
                float r = on ? 1f : -1f;

                return r;
            }
        };

        n.termVolumeMax.setValue(7);
        a.durations.setValue(1);
        a.curiosity.setValue(0.1f);


        SortedSet<DerivedTask> derived = new TreeSet<>((y,x) -> {
             int f1 = Float.compare(x.conf(), y.conf());
             if (f1 == 0) {
                 int f2 = x.term().compareTo(y.term());
                 if (f2 == 0) {
                     return Integer.compare(x.hashCode(), y.hashCode());
                 }
                 return f2;
             }
             return f1;
        });
        n.onTask(t -> {
            if (t instanceof DerivedTask && t.isBeliefOrGoal()) {
                derived.add((DerivedTask)t);
            }
        });

        //n.log();
        int timeBatch = 200;

        for (int i = 0; i < 15; i++) {
            n.run(timeBatch);


            float averageReward = a.rewardSum / timeBatch;
            System.out.println("time=" + n.time() + "\tavg reward=" + n4(averageReward) +
                    "\ttoggles +" + togglesPos[0] + "/-" + togglesNeg[0]);
            a.rewardSum = 0;


//            if (averageReward < 0) {
//                derived.forEach(t -> {
//                    if (t.isGoal())
//                        System.out.println(t.proof());
//                });
//            }
            derived.clear();
        }
    }
}

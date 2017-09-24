package nars.nal.nal8;

import nars.*;
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
        boolean[] target = new boolean[1];

        NAgent a = new NAgent(n) {


            float score = 0;

            {
                actionToggle($.the("mustBeOn"), (b) -> {
                    score += (b == target[0]) ? +1 : -1;
                });

            }

            @Override
            protected float act() {
                float r = score;
                score = 0;

                return r;
            }
        };

        n.termVolumeMax.setValue(7);
        a.durations.setValue(1);



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
        int timeBatch = 25;

        for (int i = 0; i < 8; i++) {
            a.curiosity.setValue(1+(1f/i));
            batchCycle(n, togglesPos, togglesNeg, target, a, derived, timeBatch);
        }
    }

    public void batchCycle(NAR n, int[] togglesPos, int[] togglesNeg, boolean[] target, NAgent a, SortedSet<DerivedTask> derived, int timeBatch) {
        //System.out.println("ON");
        target[0] = true;
        togglesPos[0] = togglesNeg[0] = 0;
        for (int i = 0; i < 1; i++) {
            batch(n, togglesPos[0], togglesNeg[0], a, derived, timeBatch);
        }

        //System.out.println("OFF");
        target[0] = false;
        togglesPos[0] = togglesNeg[0] = 0;

        for (int i = 0; i < 1; i++) {
            batch(n, togglesPos[0], togglesNeg[0], a, derived, timeBatch);
        }
    }

    public void batch(NAR n, int togglesPo, int i, NAgent a, SortedSet<DerivedTask> derived, int timeBatch) {
        n.run(timeBatch);


        float averageReward = a.rewardSum / timeBatch;
        //System.out.println("time=" + n.time() + "\tavg reward=" + n4(averageReward) );
        System.out.println(n.time() + ", "  + n4(averageReward) );
//                "\ttoggles +" + togglesPo + "/-" + i);
        a.rewardSum = 0;


            if (averageReward < 0) {
                derived.forEach(t -> {
                    if (t.isGoal())
                        System.out.println(t.proof());
                });
            }
        derived.clear();
    }
}

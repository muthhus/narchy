package nars.nal.nal8;

import nars.*;
import nars.concept.GoalActionConcept;
import nars.task.DerivedTask;
import org.junit.Test;

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
                must = actionToggle($.the("mustBeOn"), (b) -> {
                    (b ? togglesPos : togglesNeg)[0]++;
                    on = b;
                });
            }

            @Override
            protected float act() {
                float r = on ? 1f : -1f;

                if (nar.random().nextInt(10) == 0)
                    on = !on; //flip

                return r;
            }
        };

        a.durations.setValue(2);
        a.curiosity.setValue(0f);
        n.onTask(t -> {
            if (t instanceof DerivedTask) {
                System.out.println(t.toString(n));
            }
        });

        //n.log();
        n.run(1000);


        System.out.println("time=" + n.time() + "\tavg reward=" + n4(a.rewardSum / n.time()) +
                "\ttoggles +" + togglesPos[0] + "/-" + togglesNeg[0]) ;
    }
}

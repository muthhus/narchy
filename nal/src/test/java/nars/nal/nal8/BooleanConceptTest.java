package nars.nal.nal8;

import nars.$;
import nars.Global;
import nars.NAR;
import nars.concept.BooleanConcept;
import nars.concept.OperationConcept;
import nars.nal.Tense;
import nars.nar.Default;
import nars.util.signal.FloatConcept;
import org.junit.Test;

import static nars.nal.nal8.MotivationTest.timeUntil;

/**
 * Created by me on 4/7/16.
 */
public class BooleanConceptTest {

    @Test
    public void testAndConcept() {

        Global.DEBUG = true;

        NAR n = new Default(1024, 2, 2, 2).log();

        FloatConcept A = new FloatConcept("(a)", n).punc('!');

        FloatConcept B = new FloatConcept("(b)", n).punc('!');

        OperationConcept y = new OperationConcept("do(that)", n);

        n.believe(
                $.impl(
                        BooleanConcept.And(n, A, B), 0 /*concurrent =|>*/, y
                ),
                Tense.Eternal,
                1f, 0.95f
        );


        int loops = 12;
        int cyclesBetweenPhases = 2;
        for (int i = 0; i < loops; i++) {

            n.run(cyclesBetweenPhases);

            A.set(1f); B.set(1f);

            timeUntil("switch on", n, nn -> {
                //System.out.println(y.goals().top(nn) + " " +  y.motivation(nn));
                return y.motivation(nn) >= 0.6f;
            }, 150);

            n.run(cyclesBetweenPhases);

            A.set(0f);

            timeUntil("switch off", n, nn -> {
                //System.out.println(Joiner.on(',').join(y.goals()) + " " +  y.motivation(nn));
                return y.motivation(nn) <= 0.4f;
            }, 150);
        }

    }

    @Test
    public void testOrConcept() {

        Global.DEBUG = true;

        NAR n = new Default(1024, 3, 2, 2).log();

        FloatConcept A = new FloatConcept("(a)", n).punc('!');

        FloatConcept B = new FloatConcept("(b)", n).punc('!');

        OperationConcept y = new OperationConcept("do(that)", n);

        n.believe(
                $.impl(
                        BooleanConcept.Or(n, A, B), 0 /*concurrent =|>*/, y
                ),
                Tense.Eternal,
                1f, 0.95f
        );


        int loops = 12;
        int cyclesBetweenPhases = 4;
        for (int i = 0; i < loops; i++) {

            n.run(cyclesBetweenPhases);

            A.set(1f); B.set(1f);

            timeUntil("switch (on,on)", n, nn -> {
                //System.out.println(y.goals().top(nn) + " " +  y.motivation(nn));
                return y.motivation(nn) >= 0.6f;
            }, 150);

            n.run(cyclesBetweenPhases);

            A.set(0f);

            timeUntil("switch (on,off)", n, nn -> {
                //System.out.println(Joiner.on(',').join(y.goals()) + " " +  y.motivation(nn));
                float m = y.motivation(nn);
                return m >= 0.6f;
                //return m <= 0.55f && m >= 0.45f; //~=0.5
            }, 150);

            n.run(cyclesBetweenPhases);

            B.set(0f);

            timeUntil("switch (off,off)", n, nn -> {
                //System.out.println(Joiner.on(',').join(y.goals()) + " " +  y.motivation(nn));
                float m = y.motivation(nn);
                return m <= 0.4f;
            }, 350);

        }

    }

//        b.setValue(1f);
//        int t3 = timeUntil("switch half", n, nn -> {
//            //System.out.println(Joiner.on(',').join(y.goals()) + " " +  y.motivation(nn));
//            return y.motivation(nn) >= 0.5f;
//        }, 150);

}

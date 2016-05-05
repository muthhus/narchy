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
 * tests sensor/motor activation patterns:
 * --on/off phases, and lag time for truth to propagate from sensor to motor
 * --partial on
 * --delayed link
 * --and(sensorA, sensorB)
 * --or(sensorA, sensorB)
 * --xor(sensorA, sensorB)
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


        int loops = 3;
        int cyclesBetweenPhases = 2;
        for (int i = 0; i < loops; i++) {

            n.run(cyclesBetweenPhases);

            A.set(1f); B.set(1f);

            timeUntil("switch on", n, nn -> {
                //System.out.println(y.goals().top(nn) + " " +  y.motivation(nn));
                return y.desire(nn.time()).expectation() >= 0.51f;
            }, 150);

            n.run(cyclesBetweenPhases);

            A.set(0f);

            timeUntil("switch off", n, nn -> {
                //System.out.println(Joiner.on(',').join(y.goals()) + " " +  y.motivation(nn));
                return y.desire(nn.time()).expectation() <= 0.49f;
            }, 150);
        }

    }

    @Test
    public void testOrConcept() {

        Global.DEBUG = true;

        NAR n = new Default(1024, 6, 2, 2).log();

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


        int loops = 2;
        int cyclesBetweenPhases = 32;
        for (int i = 0; i < loops; i++) {

            n.run(cyclesBetweenPhases);

            A.set(1f); B.set(1f);

            timeUntil("switch (on,on)", n, nn -> {
                //System.out.println(y.goals().top(nn) + " " +  y.motivation(nn));
                return y.desire(nn.time()).expectation() >= 0.49f;
            }, 150);

            n.run(cyclesBetweenPhases);

            A.set(0f);

            timeUntil("switch (on,off)", n, nn -> {
                //System.out.println(Joiner.on(',').join(y.goals()) + " " +  y.motivation(nn));
                float m = y.desire(nn.time()).expectation();
                return m >= 0.51f;
                //return m <= 0.55f && m >= 0.45f; //~=0.5
            }, 150);

            n.run(cyclesBetweenPhases);

//            B.set(0f);
//
//            timeUntil("switch (off,off)", n, nn -> {
//                //System.out.println(Joiner.on(',').join(y.goals()) + " " +  y.motivation(nn));
//                float m = y.expectation(nn);
//                return m <= 0.49f;
//            }, 350);

        }

    }

//        b.setValue(1f);
//        int t3 = timeUntil("switch half", n, nn -> {
//            //System.out.println(Joiner.on(',').join(y.goals()) + " " +  y.motivation(nn));
//            return y.motivation(nn) >= 0.5f;
//        }, 150);

}

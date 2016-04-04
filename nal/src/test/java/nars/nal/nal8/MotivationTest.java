package nars.nal.nal8;

import com.google.common.base.Joiner;
import nars.$;
import nars.Global;
import nars.NAR;
import nars.concept.JunctionConcept;
import nars.concept.OperationConcept;
import nars.nal.Tense;
import nars.nar.Default;
import nars.term.Compound;
import nars.term.Term;
import nars.util.signal.SensorConcept;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * tests sensor/motor activation patterns:
 *  --on/off phases, and lag time for truth to propagate from sensor to motor
 *  --partial on
 *  --delayed link
 *  --and(sensorA, sensorB)
 *  --or(sensorA, sensorB)
 *  --xor(sensorA, sensorB)
 *
 */
public class MotivationTest {

    @Test
    public void testMotivation1() {

        Global.DEBUG = true;

        for (Tense t : new Tense[] { Tense.Present, Tense.Eternal }) {
            System.out.println("\n" + t + " test:");
            NAR n = new Default();
            n.log();

            MutableFloat v = new MutableFloat(0);
            SensorConcept x = new SensorConcept("(x)", n, v);
            OperationConcept y = new OperationConcept("do(that)", n);
            Term link = $.conj(0, x, y);
            //Term link = $.impl(y, 0, x);

            testOscillate(n, v, x, y, link, 0.80f,
                    t == Tense.Eternal ? 0f : -0.80f,  //eternal has a higher negative threshold due to revision with the positive that precedes it
                    t);
        }
    }


    public void testOscillate(@NotNull NAR n, @NotNull MutableFloat v, @NotNull SensorConcept x, @NotNull OperationConcept y, @NotNull Term impl, float positiveThreshold, float negationThreshold, @NotNull Tense tense) {
        n.step().step();

        assertEquals(0, y.motivation(), 0.01f);
        assertEquals(-0.9f, x.beliefs().top(n).motivation(), 0.01f);
        assertEquals(0.05, x.beliefs().top(n).expectation(), 0.01f);

        v.setValue(1f);
        n.step().step();

        assertEquals(0.9f, x.beliefs().top(n).motivation(), 0.01f);
        assertEquals(0.95f, x.beliefs().top(n).expectation(), 0.01f);


        //link the sensor to the motor
        n.goal(impl, tense, 1f, 0.9f); //TODO why didnt eternal work


        //motor should begin to run
        int t1 = timeUntil("switch on", n, nn -> y.motivation() >= positiveThreshold, 20);


        n.goal(impl, tense, 0f, 0.9f); //should not be necessary to repeat this if eternal was working

        //change sensor and watch motor stop
        v.setValue(0);


        int t2 = timeUntil("switch off", n, nn -> y.motivation() <=  negationThreshold, 150);
    }

    static int timeUntil(String name, @NotNull NAR n, @NotNull Predicate<NAR> test, int max) {
        int t = 0;
        while (!test.test(n)) {
            n.step();
            if (t++ >= max)
                assertTrue(name + ": time limit exceeded", false);
        }
        System.out.println(name + ": time=" + t);
        return t;
    }

    @Test
    public void testMotivationBoolean() {

        Global.DEBUG = true;

        NAR n = new Default();

        MutableFloat a = new MutableFloat(0);
        SensorConcept A = new SensorConcept("(a)", n, a).timing(0, 8).punc('!');
        MutableFloat b = new MutableFloat(0);
        SensorConcept B = new SensorConcept("(b)", n, b).timing(0, 8).punc('!');

        OperationConcept y = new OperationConcept("do(that)", n);


        Compound ab = (Compound) $.conj(A, B); //AND
        JunctionConcept.ConjunctionConcept abc = new JunctionConcept.ConjunctionConcept(ab, n);
        /*n.onFrame(nn->{
            if (abc.hasBeliefs())
                System.out.println(abc.beliefs().top(nn.time()));
            if (abc.hasGoals())
                System.out.println(abc.goals().top(nn.time()));
        });*/

        //Term ab = $.esect(A, B); //AND?
        //Term ab = $.disj(A, B); //OR
        //Term ab = $.disj( $.conj($.negate(A), B), $.conj($.negate(B), A) ) ; //XOR
        //$.negate($.intersect(A, B)) //XOR

        //Term antilink = $.conj(0, $.neg(ab),  y);
        //Term link = $.impl(ab, y);

        n.log();
        //n.believe($.impl( $.conj(A,B), y), Tense.Present, 1f, 0.95f);
        //n.believe($.impl(y, 0, B), Tense.Present, 1f, 0.95f);
        //n.goal(antilink, Tense.Eternal, 0f, 0.95f);

        //OR
        //n.believe($.impl( A, /*0,*/ y), Tense.Present, 0.75f, 0.75f);
        //n.believe($.impl( B, /*0,*/ y), Tense.Present, 0.75f, 0.75f);

        //n.input(abc + "?");
        n.believe($.impl( abc, 0, y/*0,*/), Tense.Present, 1f, 0.95f);


        //a.setValue(0f); b.setValue(0f); //start OFF
        a.setValue(1f); b.setValue(1f);

        n.run(2);


        int t1 = timeUntil("switch on", n, nn -> {
            System.out.println(y.goals().top(nn) + " " +  y.motivation());
            return y.motivation() >= 0.1f;
        }, 150);

        n.run(2);

        a.setValue(0f);
        b.setValue(0f);
        int t2 = timeUntil("switch off", n, nn -> {
            System.out.println(Joiner.on(',').join(y.goals()) + " " +  y.motivation());
            return y.motivation() <= -0.1f;
        }, 150);

        b.setValue(1f);
        int t3 = timeUntil("switch half", n, nn -> {
            System.out.println(Joiner.on(',').join(y.goals()) + " " +  y.motivation());
            return y.motivation() >= 0.01f;
        }, 150);

    }
}

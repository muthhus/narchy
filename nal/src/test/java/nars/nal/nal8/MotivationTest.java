package nars.nal.nal8;

import nars.$;
import nars.Global;
import nars.NAR;
import nars.concept.OperationConcept;
import nars.nal.Tense;
import nars.nar.Default;
import nars.term.Term;
import nars.util.signal.SensorConcept;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.junit.Test;

import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * tests sensor/motor activation patterns:
 *  --on/off phases, and lag time for truth to propagate from sensor to motor
 *  --partial on
 *  --and(sensorA, sensorB)
 *  --or(sensorA, sensorB)
 *  --xor(sensorA, sensorB)
 *
 */
public class MotivationTest {

    @Test
    public void testMotivation1() {

        Global.DEBUG = true;

        NAR n = new Default();
        n.log();

        MutableFloat v = new MutableFloat(0);
        SensorConcept x = new SensorConcept(
                "(x)",
                n, v);
        OperationConcept y = new OperationConcept("do(that)", n);
        Term impl = $.impl(x, y, 0);

        n.step().step();

        assertEquals(0, y.motivation(), 0.01f);
        assertEquals(-0.9f, x.beliefs().top(n).motivation(), 0.01f);
        assertEquals(0.05, x.beliefs().top(n).expectation(), 0.01f);

        v.setValue(1f);
        n.step().step();

        assertEquals(0.9f, x.beliefs().top(n).motivation(), 0.01f);
        assertEquals(0.95f, x.beliefs().top(n).expectation(), 0.01f);


        //link the sensor to the motor
        n.goal(impl, Tense.Present, 1f, 0.9f); //TODO why didnt eternal work


        //motor should begin to run
        int t1 = timeUntil("switch on", n, nn -> y.motivation() >= 0.81f, 20);


        n.goal(impl, Tense.Present, 0f, 0.9f); //should not be necessary to repeat this if eternal was working

        //change sensor and watch motor stop
        v.setValue(0);

        int t2 = timeUntil("switch off", n, nn -> y.motivation() <= -0.80f, 150);

    }

    static int timeUntil(String name, NAR n, Predicate<NAR> test, int max) {
        int t = 0;
        while (!test.test(n)) {
            n.step();
            if (t++ >= max)
                assertTrue(name + ": time limit exceeded", false);
        }
        System.out.println(name + ": time=" + t);
        return t;
    }
}

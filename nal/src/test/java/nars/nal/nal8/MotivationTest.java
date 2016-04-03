package nars.nal.nal8;

import nars.NAR;
import nars.concept.OperationConcept;
import nars.nar.Default;
import nars.util.signal.SensorConcept;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by me on 4/3/16.
 */
public class MotivationTest {

    @Test
    public void testMotivation1() {

        NAR n = new Default();

        MutableFloat v = new MutableFloat(0);
        OperationConcept y = new OperationConcept("do(it)", n);
        SensorConcept x = new SensorConcept("it:is", n, () -> {
            return v.floatValue();
        });

        n.step().step();

        assertEquals(0, y.motivation(), 0.01f);
        assertEquals(-0.9f, x.beliefs().top(n.time()).motivation(), 0.01f);
        assertEquals(0.05, x.beliefs().top(n.time()).expectation(), 0.01f);

        v.setValue(1f);
        n.step().step();

        assertEquals(0.9f, x.beliefs().top(n.time()).motivation(), 0.01f);
        assertEquals(0.95f, x.beliefs().top(n.time()).expectation(), 0.01f);

        //link the sensor to the motor
        n.input("(it:is ==>+0 do(it))! :|:");
        //n.input("(it:is ==>+0 do(it))!"); //TODO why didnt eternal work

        int maxLagTime = 20;

        //motor should begin to run
        for (int i = 0; i < maxLagTime; i++) {
            n.step();
            System.out.println(x.get() + " ==> " + y.motivation());
        }
        assertEquals(0.81f, y.motivation(), 0.01f);

        v.setValue(0);

        n.input("(it:is ==>+0 do(it))! :|:"); //should not be necessary to repeat this if eternal was working

        //change sensor and watch motor stop
        n.log();

        for (int i = 0; i < maxLagTime; i++) {
            n.step();
            System.out.println(x.get() + " ==> " + y.motivation());
        }
        assertEquals(-0.81f, y.motivation(), 0.01f);
    }
}

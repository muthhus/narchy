package nars.nal.nal8;

import nars.$;
import nars.Global;
import nars.NAR;
import nars.concept.OperationConcept;
import nars.nal.Tense;
import nars.nar.Default;
import nars.term.Term;
import nars.util.signal.FloatConcept;
import nars.util.signal.MotorConcept;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.function.Predicate;

import static org.junit.Assert.*;

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

        for (Tense t : new Tense[] { Tense.Eternal/*, Tense.Present*/ }) {
            System.out.println("\n" + t + " test:");
            NAR n = new Default();
            n.log();


            FloatConcept x = new FloatConcept("(x)", n).punc('!');
            OperationConcept y = new MotorConcept("do(that)", n, MotorConcept.relative);
            //Term link = $.conj(0, x, y);
            Term link = $.impl(x, 0, y);

            testOscillate(n, x, y, link, 0.6f,
                    t == Tense.Eternal ? 0.4f : 0.4f,  //eternal has a higher negative threshold due to revision with the positive that precedes it
                    t);
        }
    }


    public void testOscillate(@NotNull NAR n, @NotNull FloatConcept x, @NotNull OperationConcept y, @NotNull Term impl, float positiveThreshold, float negationThreshold, @NotNull Tense tense) {
        n.step().step();

        assertEquals(0.5f, y.motivation(n), 0.01f);
        assertFalse(x.hasGoals());
//        assertEquals(0f, x.goals().top(n).motivation(), 0.01f);
//        assertEquals(0.5, x.goals().top(n).expectation(), 0.01f);



        x.set(1f);
        n.step().step();

        assertEquals(0.9f, x.goals().top(n).motivation(), 0.01f);
        assertEquals(0.95f, x.goals().top(n).expectation(), 0.01f);


        //link the sensor to the motor
        n.believe(impl, tense, 1f, 0.99f);

        //n.run(2);

        //motor should begin to run
        int t1 = timeUntil("switch on", n, nn-> {
            //System.out.println(y.motivation(nn));
            return y.motivation(nn) >= positiveThreshold;
        }, 60);

        n.run(2);

        //change sensor and watch motor stop
        x.set(0);

        int t2 = timeUntil("switch off", n, nn -> {
            //System.out.println(y.motivation(nn));
            return y.motivation(nn) <=  negationThreshold; }, 150
        );

        n.run(2);
    }

    /** will return >=1 cycles */
    static int timeUntil(String name, @NotNull NAR n, @NotNull Predicate<NAR> test, int max) {
        int t = 0;
        System.out.println(name + "...");
        do {
            n.step();
            if (t++ >= max)
                assertTrue(name + ": time limit exceeded", false);
        } while (!test.test(n));
        System.out.println(name + ": time=" + t);
        return t;
    }


}

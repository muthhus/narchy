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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.junit.Assert.*;

/**
 * tests sensor/motor activation patterns:
 * --on/off phases, and lag time for truth to propagate from sensor to motor
 * --partial on
 * --delayed link
 * --and(sensorA, sensorB)
 * --or(sensorA, sensorB)
 * --xor(sensorA, sensorB)
 */
public class MotivationTest {


    @Test
    public void testMotivation1() {

        Global.DEBUG = true;

        NAR n = new Default();
        n.log();

        AtomicInteger execs = new AtomicInteger(0);

        FloatConcept x = new FloatConcept("(x)", n).punc('!');
        MotorConcept y = new MotorConcept("(y)", n, (b,d)->{

            if (d > 0.5f && d < b) return Float.NaN;
            if (d < 0.5f && b < d) return Float.NaN;

            float sat = d - b;
            System.out.println("exec: " + b + " ," + d + " -> " + sat);

            execs.addAndGet(1);

            return sat;
        });

        n.believe($.impl(x, y));

        n.run(4);
        assertEquals(0, execs.get());

        //ON
        x.set(1);

        n.run(4);
        assertEquals(1, execs.get());

        n.run(16); //remains stable with one execution
        assertEquals(1, execs.get());

        //OFF
        x.set(0);

        n.run(16);

        assertEquals(2, execs.get());

    }

//    @Test
//    public void testMotivation1() {
//
//        Global.DEBUG = true;
//
//        for (Tense t : new Tense[] { Tense.Eternal/*, Tense.Present*/ }) {
//            System.out.println("\n" + t + " test:");
//            NAR n = new Default();
//            n.log();
//
//
//            FloatConcept x = new FloatConcept("(x)", n).punc('!');
//            OperationConcept y = new MotorConcept("do(that)", n, MotorConcept.relative);
//            //Term link = $.conj(0, x, y);
//            Term link = $.impl(x, 0, y);
//
//            testOscillate(n, x, y, link, 0.6f,
//                    t == Tense.Eternal ? 0.4f : 0.4f,  //eternal has a higher negative threshold due to revision with the positive that precedes it
//                    t);
//        }
//    }
//
//
//    public void testOscillate(@NotNull NAR n, @NotNull FloatConcept x, @NotNull OperationConcept y, @NotNull Term impl, float positiveThreshold, float negationThreshold, @NotNull Tense tense) {
//        n.step().step();
//
//        assertEquals(0.5f, y.motivation(n), 0.01f);
//        assertFalse(x.hasGoals());
////        assertEquals(0f, x.goals().top(n).motivation(), 0.01f);
////        assertEquals(0.5, x.goals().top(n).expectation(), 0.01f);
//
//
//
//        x.set(1f);
//        n.step().step();
//
//        assertEquals(0.9f, x.goals().top(n).motivation(), 0.01f);
//        assertEquals(0.95f, x.goals().top(n).expectation(), 0.01f);
//
//
//        //link the sensor to the motor
//        n.believe(impl, tense, 1f, 0.99f);
//
//        //n.run(2);
//
//        //motor should begin to run
//        int t1 = timeUntil("switch on", n, nn-> {
//            //System.out.println(y.motivation(nn));
//            return y.motivation(nn) >= positiveThreshold;
//        }, 60);
//
//        n.run(2);
//
//        //change sensor and watch motor stop
//        x.set(0);
//
//        int t2 = timeUntil("switch off", n, nn -> {
//            //System.out.println(y.motivation(nn));
//            return y.motivation(nn) <=  negationThreshold; }, 150
//        );
//
//        n.run(2);
//    }
//
    /** will return >=1 cycles */
    public static int timeUntil(String name, @NotNull NAR n, @NotNull Predicate<NAR> test, int max) {
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
//

}


//package nars.nal.nal8;
//
//import nars.Global;
//import nars.NAR;
//import nars.nar.Default;
//import nars.task.Task;
//import org.jetbrains.annotations.NotNull;
//import org.junit.Test;
//
///**
// * Created by me on 1/30/16.
// */
//public class DesireTest {
//
//    @Test
//    public void testIndirectDesireEnvelope1() {
//        int t1 = 20;
//        testDesireEnvelope(
//                t1,
//                "(a:b ==>+" + t1 + " c:d). :|:",
//                "a:b! :|:");
//        //n.input("c:d! :|:");
//    }
//    @Test
//    public void testIndirectDesireEnvelope2() {
//        int t1 = 20;
//        testDesireEnvelope(
//                t1,
//                "(a:b &&+0 c:d). :|:",
//                "a:b! :|:");
//        //n.input("c:d! :|:");
//    }
//    public void testDesireEnvelope(int t1, @NotNull String... inputs) {
//        Global.DEBUG = true;
//
//        NAR n = new Default();
//
//        //n.log();
//
//        for (String s : inputs)
//            n.input(s);
//
//        for (int i = 0; i < t1 * 2; i++) {
//            n.step();
//            long now = n.time();
//            //System.out.println(n.concept("a:b").goals().topTemporal(now,now));
//            print(n, "c:d", now);
//            print(n, "a:b", now);
//        }
//
//        //n.concept("c:d").print();
//    }
//
//    private float print(@NotNull NAR n, @NotNull String concept, long now) {
//        Task tt = n.concept(concept).goals().top(now);
//        System.out.println(now + ": " + "\t" + tt);
//            /*if (tt!=null)
//                System.out.println(tt.getExplanation());*/
//        if (tt!=null)
//            return tt.expectation();
//        return -1f;
//    }
//}

package nars.nal.nal8;

import com.gs.collections.api.tuple.primitive.IntObjectPair;
import com.gs.collections.impl.tuple.primitive.PrimitiveTuples;
import nars.$;
import nars.Global;
import nars.NAR;
import nars.Symbols;
import nars.concept.CompoundConcept;
import nars.nal.Tense;
import nars.nar.Default;
import nars.term.Compound;
import nars.term.Term;
import nars.util.Texts;
import nars.util.signal.MotorConcept;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.Test;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 5/6/16.
 */
public class ControlFlowTest {

    @Test public void testSequence2()   { testSequence(n, 2, 5);     }
    @Test public void testSequence3()   { testSequence(n, 3, 20);     }
    @Test public void testSequence4()   { testSequence(n, 4, 30);    }
    @Test public void testSequence8()   { testSequence(n, 8, 30);    }
    @Test public void testSequence16()  { testSequence(n, 16, 40);     }



    public ExeTracker testSequence(Supplier<NAR> nn, int length, int delay) {

        System.out.println("sequence execution:  states=" + length + " inter-state delay=" + delay);

        int runtime = (delay * length) * 10;


        ExeTracker exeTracker = new ExeTracker();
        NAR n = nn.get();

        for (int i = 0; i < length; i++)
            newExeState(n, s(i), exeTracker);

        for (int i = 0; i < length - 1; i++)
            n.goal($.conj(delay, s(i), s(i + 1)));

        //start
        n.goal(s(0), Tense.Present, 1f);

        n.run(runtime);

        exeTracker.assertLength(length, delay);

        return exeTracker;

    }

    @Test public void testBranchThen()  {
        testBranch(n, 10, 1f);
    }
    @Test public void testBranchThenThen()  {
        testBranch(n, 10,
                        1f, 1f);
    }
    @Test public void testBranchElse()  {
        testBranch(n, 10, 0f);
    }

    public ExeTracker testBranch(Supplier<NAR> nn, int delay, float... conditionSequence) {


        int beforeBranchLength = 2;
        int afterBranchLength = 2;

        int length = (beforeBranchLength + afterBranchLength );
        System.out.println("branch execution:  states=" + length + " inter-state delay=" + delay);

        int runtime = (delay * length) * 10;


        ExeTracker exeTracker = new ExeTracker();
        NAR n = nn.get();

        final String PRE = "pre";
        final String THEN = "then";
        final String ELSE = "else";


        for (int i = 0; i < beforeBranchLength; i++) {
            newExeState(n, s(PRE, i), exeTracker);
        }
        for (int i = 0; i < beforeBranchLength - 1; i++) {
            n.goal($.conj(delay, s(PRE, i), s(PRE, i + 1)));
        }

        Term condition = b(0); //newExeState(n, b(0), exeTracker).term();

        $.conj(delay, s(PRE, beforeBranchLength-1), condition);

        for (int i = 0; i < afterBranchLength; i++) {
            newExeState(n, s(THEN, i), exeTracker);
            newExeState(n, s(ELSE, i), exeTracker);
        }
        for (int i = 0; i < afterBranchLength - 1; i++) {
            n.goal($.conj(delay, s(THEN, i), s(THEN, i + 1)));
            n.goal($.conj(delay, s(ELSE, i), s(ELSE, i + 1)));
        }

        //n.goal($.conj(delay, $.conj(0, s(PRE, beforeBranchLength-1), condition), s(THEN, 0)));
        //n.goal($.conj(delay, $.conj(0, s(PRE, beforeBranchLength-1), $.neg(condition)), s(ELSE, 0)));

        //n.goal($.conj(delay, $.conj(  s(PRE, beforeBranchLength-1), condition), s(THEN, 0)));
        //n.goal($.conj(delay, $.conj(  s(PRE, beforeBranchLength-1), $.neg(condition)), s(ELSE, 0)));

        n.goal($.conj( delay, $.conj(  delay, condition, s(PRE, beforeBranchLength-1) ), s(THEN, 0)));
        n.goal($.conj( delay, $.conj(  delay, $.neg(condition), s(PRE, beforeBranchLength-1) ), s(ELSE, 0)));

        //n.believe($.impl(condition, delay, s(THEN, 0)));
        //n.believe($.impl($.neg(condition), delay, s(ELSE, 0)));


        Compound start = s(PRE, 0);

        n.log();

        for (float B : conditionSequence ) {
            System.out.println("Execute Forward branch w/ condition=" + B);
            n.believe(condition, Tense.Present, B, 1f).step();

            n.goal(start, Tense.Present, 1f);

            n.run(runtime);

            exeTracker.assertLength(length, delay);

            String postBranch = B > 0.5f ? THEN : ELSE;
            exeTracker.assertPath(s(PRE, 0), s(PRE, 1), s(postBranch, 0), s(postBranch, 1));

            exeTracker.clear();

            //pause between
            n.run(delay);
        }



        return exeTracker;

    }


        static final Supplier<NAR> n = () -> {
        Default x = new Default(512, 2, 1, 3);
        x.cyclesPerFrame.set(2);
        return x;
    };


    public static Compound s(int i) {
        return $.p("s" + i);
    }
    public static Compound s(String group, int i) {
        return $.p(group + "_" + i);
    }
    public static Compound b(int i) {
        return $.p("b" + i);
    }

    static class ExeTracker {

        final List<IntObjectPair<Compound>> events = Global.newArrayList();
        final SummaryStatistics eventIntervals = new SummaryStatistics();

        public void record(Compound c, long now) {

            if (!events.isEmpty())
                eventIntervals.addValue(now - events.get(events.size() - 1).getOne());

            events.add(PrimitiveTuples.pair((int)now, c));

        }

        public void assertLength(int length, int delay) {
            System.out.println("Execution Intervals: \t min=" + eventIntervals.getMin() + " avg=" + eventIntervals.getMean() + " max=" + eventIntervals.getMax() + " stddev=" + eventIntervals.getStandardDeviation());

            System.out.println("  mean timing error: " + Texts.n2((((float) Math.abs(eventIntervals.getMean() - delay) / delay) * 100.0)) + "%");
            System.out.println();

            assertEquals(length, events.size());
        }

        public void clear() {
            events.clear();
            eventIntervals.clear();
        }

        public void assertPath(Term... states) {
            final int[] i = {0};
            assertTrue( events.stream().allMatch(p -> p.getTwo().equals(states[i[0]++])) );
        }
    }



    public CompoundConcept newExeState(NAR n, Compound term, ExeTracker e) {
        float exeThresh = 0.1f;

        return new MotorConcept(term, n, (b, d) -> {
            if (d > 0.5f && (d > b + exeThresh)) {
                long now = n.time();
                System.out.println(term + " at " + now + " b=" + b + ", d=" + d + " (d-b)=" + (d - b));

                e.record(term, now);

                //n.goal(term, Tense.Present, 0f); //neutralize

                return 1f;
            }
            return Float.NaN;
            //return 1f;
        });
    }


    @Test
    public void testToggledSequence() {

    }

    @Test
    public void testWeightedFork() {

    }

    @Test
    public void testLoop() {

    }

    @Test
    public void testToggledLoop() {

    }

}

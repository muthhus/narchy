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

/**
 * Created by me on 5/6/16.
 */
public class ControlFlowTest {

    @Test public void testSequence2()   { testSequence(n, 2, 5);     }
    @Test public void testSequence3()   { testSequence(n, 3, 20);     }
    @Test public void testSequence4()   { testSequence(n, 4, 30);    }
    @Test public void testSequence8()   { testSequence(n, 8, 30);    }
    @Test public void testSequence16()  { testSequence(n, 16, 40);     }

    @Test public void testBranch1()  {
        testBranch(n, 30);
    }

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
        n.goal(s(0), Tense.Present, 1f, n.getDefaultConfidence(Symbols.GOAL));

        n.run(runtime);

        exeTracker.assertLength(length, delay);

        return exeTracker;

    }


    public ExeTracker testBranch(Supplier<NAR> nn, int delay) {

        int beforeBranchLength = 2;
        int afterBranchLength = 2;

        int length = (beforeBranchLength + afterBranchLength );
        System.out.println("branch execution:  states=" + length + " inter-state delay=" + delay);

        int runtime = (delay * length) * 10;


        ExeTracker exeTracker = new ExeTracker();
        NAR n = nn.get();

        final int PRE = 0;
        final int THEN = 1;
        final int ELSE = 2;


        for (int i = 0; i < beforeBranchLength; i++) {
            newExeState(n, s(PRE, i), exeTracker);
        }
        for (int i = 0; i < beforeBranchLength - 1; i++) {
            n.goal($.conj(delay, s(PRE, i), s(PRE, i + 1)));
        }

        Term condition = newExeState(n, b(0), exeTracker).term();

        $.conj(delay, s(PRE, beforeBranchLength-1), condition);

        for (int i = 0; i < afterBranchLength; i++) {
            newExeState(n, s(THEN, i), exeTracker);
            newExeState(n, s(ELSE, i), exeTracker);
        }
        for (int i = 0; i < afterBranchLength - 1; i++) {
            n.goal($.conj(delay, s(THEN, i), s(THEN, i + 1)));
            n.goal($.conj(delay, s(ELSE, i), s(ELSE, i + 1)));
        }

        n.goal($.conj(delay, condition, s(THEN, 0)));
        n.goal($.conj(delay, $.neg(condition), s(ELSE, 0)));



        //start
        Compound start = s(PRE, 0);

        n.goal(start, Tense.Present, 1f);
        n.believe(condition, Tense.Present, 1f);

        n.log();
        n.run(runtime);

        exeTracker.assertLength(length, delay);

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
    public static Compound s(int group, int i) {
        return $.p("s" + group + "_" + i);
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

            assertEquals(events.get(events.size() - 1).toString(), length, events.size());
        }
    }



    public CompoundConcept newExeState(NAR n, Compound term, ExeTracker e) {
        float exeThresh = 0.2f;

        return new MotorConcept(term, n, (b, d) -> {
            if (d > b + exeThresh) {
                long now = n.time();
                System.out.println(term + " at " + now + " " + (d - b));

                e.record(term, now);

                return 0.9f;
            }
            return Float.NaN;
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

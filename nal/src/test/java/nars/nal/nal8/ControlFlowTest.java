package nars.nal.nal8;

import nars.$;
import nars.Global;
import nars.NAR;
import nars.Symbols;
import nars.nal.Tense;
import nars.nar.Default;
import nars.term.Compound;
import nars.util.Texts;
import nars.util.signal.MotorConcept;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

/**
 * Created by me on 5/6/16.
 */
public class ControlFlowTest {

    @Test
    public void testSequence3() {
        Supplier<NAR> n = () -> {
            Default x = new Default(512, 2, 1, 3);
            x.cyclesPerFrame.set(2);
            return x;
        };
        testSequence(n, 2, 5);
        testSequence(n, 3, 20);
        testSequence(n, 4, 30);
        testSequence(n, 8, 30);
        testSequence(n, 16, 40);
    }

    public static Compound s(int i) {
        return $.p("s" + i);
    }

    public static class ExeTracker {

        final List<int[]> events = Global.newArrayList();
        final SummaryStatistics eventIntervals = new SummaryStatistics();

        public void record(int i, long now) {

            if (!events.isEmpty())
                eventIntervals.addValue(now - events.get(events.size() - 1)[0]);

            events.add(new int[]{(int) now, i});

        }

        public void assertLength(int length, int delay) {
            System.out.println("Execution Intervals: \t min=" + eventIntervals.getMin() + " avg=" + eventIntervals.getMean() + " max=" + eventIntervals.getMax() + " stddev=" + eventIntervals.getStandardDeviation());

            System.out.println("  mean timing error: " + Texts.n2((((float) Math.abs(eventIntervals.getMean() - delay) / delay) * 100.0)) + "%");
            System.out.println();

            assertEquals(Arrays.toString(events.get(events.size() - 1)), length, events.size());
        }
    }


    public ExeTracker testSequence(Supplier<NAR> nn, int length, int delay) {

        System.out.println("sequence execution:  states=" + length + " inter-state delay=" + delay);

        int runtime = (delay * length) * 10;

        NAR n = nn.get();

        ExeTracker exeTracker = new ExeTracker();

        for (int i = 0; i < length; i++)
            newExeState(n, exeTracker, i);

        for (int i = 0; i < length - 1; i++)
            n.goal($.conj(delay, s(i), s(i + 1)));

        //start
        n.goal(s(0), Tense.Present, 1f, n.getDefaultConfidence(Symbols.GOAL));

        n.run(runtime);

        exeTracker.assertLength(length, delay);

        return exeTracker;

    }

    public void newExeState(NAR n, ExeTracker e, int i) {
        float exeThresh = 0.2f;

        new MotorConcept(s(i), n, (b, d) -> {
            if (d > b + exeThresh) {
                long now = n.time();
                System.out.println(i + " at " + now + " " + (d - b));

                e.record(i, now);

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

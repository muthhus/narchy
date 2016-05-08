package nars.nal.nal8;

import com.gs.collections.api.tuple.primitive.IntIntPair;
import nars.$;
import nars.Global;
import nars.NAR;
import nars.Symbols;
import nars.nal.Tense;
import nars.nar.Default;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.util.Texts;
import nars.util.signal.MotorConcept;
import org.apache.commons.math3.stat.Frequency;
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

    public List<int[]> testSequence(Supplier<NAR> nn, int length, int delay) {

        System.out.println("sequence execution:  states=" +length + " inter-state delay=" + delay);

        float exeThresh = 0.1f;

        int runtime = (delay * length) * 10;


        NAR n = nn.get();

        //n.log();

        List<int[]> events = Global.newArrayList();
        SummaryStatistics eventIntervals = new SummaryStatistics();


        for (int i = 0; i < length; i++) {
            int ii = i;
            new MotorConcept(s(i), n, (b, d) -> {
                if (d > b + exeThresh) {
                    long now = n.time();
                    System.out.println(ii + " at " + now + " " + (d-b));

                    if (!events.isEmpty())
                        eventIntervals.addValue(now - events.get(events.size()-1)[0] );

                    events.add(new int[] {(int) now, ii });

                    return 0.9f;
                }
                return Float.NaN;
            });
        }

        for (int i = 0; i < length-1; i++) {
            Term t = $.conj(delay, s(i), s(i+1));
            n.goal(t);
        }

        n.goal(s(0), Tense.Present, 1f, n.getDefaultConfidence(Symbols.GOAL));

        n.run(runtime);

        System.out.println("Execution Intervals: \t min=" + eventIntervals.getMin() + " avg=" + eventIntervals.getMean() + " max=" + eventIntervals.getMax() + " stddev=" + eventIntervals.getStandardDeviation());

        System.out.println("  mean timing error: " + Texts.n2(( ((float)Math.abs(eventIntervals.getMean() - delay)/delay) * 100.0)) + "%");
        System.out.println();

        assertEquals(Arrays.toString(events.get(events.size()-1)), length, events.size());

        return events;

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

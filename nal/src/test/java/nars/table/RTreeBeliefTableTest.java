package nars.table;

import jcog.math.MultiStatistics;
import nars.*;
import nars.concept.BaseConcept;
import nars.task.DerivedTask;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.LongToFloatFunction;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static jcog.Texts.n2;
import static nars.Op.BELIEF;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RTreeBeliefTableTest {

    @Test
    public void testBasicOperations() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        BaseConcept X = (BaseConcept) n.conceptualize($.$("a:b"));
        RTreeBeliefTable r = new RTreeBeliefTable();
        r.setCapacity(4);

        assertEquals(0, r.size());

        Term x = X.term();
        float freq = 1f;
        float conf = 0.9f;
        int creationTime = 1;
        int start = 1, end = 1;

        Task a = add(r, x, freq, conf, start, end, n);
        assertEquals(1, r.size());

        r.add(a, X, n); assertEquals(1, r.size()); //no change for inserted duplicate

        Task b = add(r, x, 0f, 0.5f, 1, 1, n); //WEAKer
        assertEquals(2, r.size());

        Task c = add(r, x, 0.1f, 0.9f, 2, 2, n);
        assertEquals(3, r.size());

        Task d = add(r, x, 0.1f, 0.9f, 3, 4, n);
        assertEquals(4, r.size());

        System.out.println("at capacity");
        r.print(System.out);

        //try capacity limit
        Task e = add(r, x, 0.3f, 0.9f, 3, 4, n);
        assertEquals(4, r.size()); //capacity limit unaffected

        System.out.println("after capacity compress inserting " + e.toString(true));
        r.print(System.out);
    }

    @NotNull
    public Task add(RTreeBeliefTable r, Term x, float freq, float conf, int start, int end, NAR n) {
        Task a = $.belief(x, freq, conf).time(start, start, end).apply(n);
        a.pri(0.5f);
        r.add(a, (BaseConcept) n.concept(x), n);
        return a;
    }


    @Test public void testAccuracyFlat() {
        testAccuracy(1, 1,20, (t) -> 0.5f); //flat
    }
    @Test public void testAccuracySineDur1() {
        testAccuracy(1, 1,20, (t) -> (float)(Math.sin(t/5f)/2f+0.5f));
    }
    @Test public void testAccuracySineDur1Ext() {
        testAccuracy(1, 1,50, (t) -> (float)(Math.sin(t/5f)/2f+0.5f));
    }
    @Test public void testAccuracySineDur() {
        testAccuracy(2, 4,50, (t) -> (float)(Math.sin(t/5f)/2f+0.5f));
    }
    @Test public void testAccuracySqrWave() {
        testAccuracy(1, 3, 20, (t) -> (Math.sin(t)/2f+0.5f) >= 0.5 ? 1f : 0f);
    }

    static void testAccuracy(int dur, int period, int end, LongToFloatFunction func) {

        NAR n = NARS.shell();

        n.time.dur(dur);

        Term term = $.p("x");

        //1. populate

        n.log();

        BaseConcept c = (BaseConcept) n.conceptualize(term);
        @NotNull BeliefTable cb = true ? c.beliefs() : c.goals();
        //int numTasks = 0;
        long time=0;
        while (time < end) {
            n.input($.task(term, BELIEF, func.valueOf(time), 0.9f).time(time).setPriThen(0.5f).apply(n));
            time += period;
            n.run(period);
            //numTasks++;
        }

        //n.run(1);


        c.beliefs().print();
        MultiStatistics<Task> m = new MultiStatistics<Task>()
            .classify("input", (t) -> t.isInput())
            .classify("derived", (t) -> t instanceof DerivedTask)
//            .classify("revised", (t) -> t instanceof AnswerTask)
            .value("pri", (t) -> t.pri())
            .value2D("truth", (t) -> new float[] { t.freq(), t.conf() })
            .value("freqErr", (t) -> Math.abs( ((t.freq()-0.5f)*2f) - func.valueOf(t.mid())) )
            .add(c.beliefs());

        m.print();


        System.out.println();

        //2. validate and calculate error
        double errSum = 0;
        for (long i = 0; i < end; i++) {
            float expected = func.valueOf(i);
            Truth actualTruth = n.beliefTruth(term, i);
            float actual = actualTruth != null ? actualTruth.freq() : 0.5f;
            float err = Math.abs(actual - expected);
            System.out.println(n2(i) + "\t" + /*n2(err) + "\t" + */ n2(expected) + "\t" + n2(actual));
            errSum += err;
        }
        double avgErr = errSum / end;
        System.err.println(avgErr + " avg point error");
        assertTrue(avgErr < 0.1f);
    }

}

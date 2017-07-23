package nars.table;

import jcog.math.MultiStatistics;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.Task;
import nars.concept.TaskConcept;
import nars.nar.Terminal;
import nars.task.DerivedTask;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.LongToFloatFunction;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static jcog.Texts.n2;
import static nars.Op.BELIEF;
import static org.junit.Assert.assertEquals;

public class RTreeBeliefTableTest {

    @Test
    public void testBasicOperations() throws Narsese.NarseseException {
        NAR n = new Terminal();
        TaskConcept X = (TaskConcept) n.conceptualize($.$("a:b"));
        RTreeBeliefTable t = new RTreeBeliefTable(4);

        assertEquals(0, t.size());

        Term x = X.term();
        Task a = $.belief(x, 1f, 0.9f).time(1).apply(n); a.pri(0.5f);
        t.add(a, X, n);
        assertEquals(1, t.size());

        t.add(a, X, n);
        assertEquals(1, t.size()); //no change for inserted duplicate

        Task b = $.belief(x, 0f, 0.9f).time(3).apply(n); b.pri(0.5f);
        t.add(b, X, n);
        assertEquals(2, t.size());

        Task c = $.belief(x, 0.1f, 0.9f).time(3).apply(n); c.pri(0.5f);
        t.add(c, X, n);
        assertEquals(3, t.size());

        Task d = $.belief(x, 0.1f, 0.9f).time(0,3, 4).apply(n); d.pri(0.5f);
        t.add(d, X, n);
        assertEquals(4, t.size()); //no change for inserted duplicate

        t.print(System.out);
    }


    @Test
    public void testAccuracy() throws Narsese.NarseseException {
        NAR n = new Terminal();


        int period = 3;
        n.time.dur(period);

        int end = 50;

        Compound term = $.p("x");
        LongToFloatFunction func = (t) -> (float)(Math.sin(t/5f)/2f+0.5f);

        //1. populate

        n.log();

        TaskConcept c = (TaskConcept) n.conceptualize(term);
        @NotNull BeliefTable cb = true ? c.beliefs() : c.goals();
        //int numTasks = 0;
        long time=0;
        while (time < end) {
            cb.add($.task(term, BELIEF, func.valueOf(time), 0.9f).time(time).setPriThen(0.5f).apply(n), c, n);
            time += period;
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



        //2. validate and calculate error
        double errSum = 0;
        for (long i = 0; i < end; i++) {
            float expected = func.valueOf(i);
            Truth actualTruth = n.beliefTruth(term, i);
            float actual = actualTruth != null ? actualTruth.freq() : 0.5f;
            float err = Math.abs(actual - expected);
            System.out.println(n2(i) + "\t" + n2(err) + "\t" + n2(expected) + "\t" + n2(actual));
            errSum += err;
        }
        System.err.println(errSum / end + " avg point error");
    }

}

package nars.task;

import com.google.common.collect.Lists;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.nar.AbstractNAR;
import nars.nar.Default;
import nars.nar.Terminal;
import nars.truth.DefaultTruth;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import static org.junit.Assert.*;

/**
 * Created by me on 11/3/15.
 */
public class TaskTest {




    @Test public void testTruthHash() {
        //for TRUTH EPSILON 0.01:

        DefaultTruth dt = new DefaultTruth(0, 0.1f);
        assertEquals(9, dt.hashCode());

        DefaultTruth du = new DefaultTruth(1, 1.0f);
        assertEquals(6553700, du.hashCode());
    }

    /** tests the ordering of tasks that differ by truth values,
     * which is determined by directly comparing their int hashcode
     * representation (which is perfect and lossless hash if truth epsilon
     * is sufficiently large) */
    @Test public void testTaskOrderByTruthViaHash() {
        Terminal n = new Terminal();
        TreeSet<Task> t = new TreeSet<>();
        int count = 0;
        for (float f = 0; f < 1.0f; f += 0.3f)
            for (float c = 0.01f; c < 1.0f; c += 0.3f) {
                t.add(
                    n.inputTask($.task("a:b", '.',f, c))
                );
                count++;
            }
        assertEquals(count, t.size());

        List<Task> l = Lists.newArrayList(t);
        //l.forEach(System.out::println);
        int last = l.size() - 1;

        assertTrue(l.get(0).toString(), l.get(0).toString().contains("(b-->a). :0: %0.0;.01%"));
        assertTrue(l.get(last).toString(), l.get(last).toString().contains("(b-->a). :0: %.90;.91%"));

        //test monotonically increasing
        Task y = null;
        for (int i = l.size()-1; i >=0; i--) {
            Task x = l.get(i);
            if (y != null) {
                assertTrue(x.freq() <= y.freq());
                float c = y.conf();
                if (x.conf() < 0.90f) //wrap around only time when it will decrease
                    assertTrue(x.conf() <= c);
            }
            y = x;
        }
    }


    @Test
    public void inputTwoUniqueTasksDef() {
        inputTwoUniqueTasks(new Default());
    }
    /*@Test public void inputTwoUniqueTasksSolid() {
        inputTwoUniqueTasks(new Solid(4, 1, 1, 1, 1, 1));
    }*/
    /*@Test public void inputTwoUniqueTasksEq() {
        inputTwoUniqueTasks(new Equalized(4, 1, 1));
    }
    @Test public void inputTwoUniqueTasksNewDef() {
        inputTwoUniqueTasks(new Default());
    }*/

    public void inputTwoUniqueTasks(@NotNull NAR n) {

        Param.DEBUG = true;

        Task x = n.inputTask("<a --> b>.");
        assertArrayEquals(new long[]{1}, x.evidence());
        n.next();

        Task y = n.inputTask("<b --> c>.");
        assertArrayEquals(new long[]{2}, y.evidence());
        n.next();

        n.reset();

        n.input("<e --> f>.  <g --> h>. "); //test when they are input on the same parse

        n.run(10);

        Task q = n.inputTask("<c --> d>.");
        assertArrayEquals(new long[]{5}, q.evidence());

    }


    @Test
    public void testDoublePremiseMultiEvidence() {

        AbstractNAR d = new Default(100,1,1,3);
        d.nal(2);
        d.input("<a --> b>.", "<b --> c>.");

        long[] ev = {1, 2};
        d.eventTaskProcess.on(t -> {
            if (t.getParentBelief()!=null)
                assertArrayEquals("all double-premise derived terms have this evidence: "
                        + t + ": " + Arrays.toString(ev) + "!=" + Arrays.toString(t.evidence()), ev, t.evidence());

            //System.out.println(t);
        });

        d.run(64);


    }
}

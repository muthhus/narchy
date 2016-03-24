package nars.nal.nal7;

import nars.Global;
import nars.NAR;
import nars.nar.Default;
import nars.task.Task;
import nars.util.time.IntervalTree;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;
import java.util.function.Consumer;

import static java.lang.System.out;
import static org.junit.Assert.assertEquals;

/**
 * Created by me on 6/5/15.
 */
public class TemporalTest {

//    @Test
//    public void testAfter() {
//
//        assertTrue("after", Tense.after(1, 4, 1));
//
//        assertFalse("concurrent (equivalent)", Tense.after(4, 4, 1));
//        assertFalse("before", Tense.after(6, 4, 1));
//        assertFalse("concurrent (by duration range)", Tense.after(3, 4, 3));
//
//    }

    @Test public void parsedCorrectOccurrenceTime() {
        NAR n = new Default(); //for cycle/frame clock, not realtime like Terminal
        Task t = n.inputTask("<a --> b>. :\\:");
        assertEquals(0, t.creation());
        assertEquals(-(n.duration()), t.occurrence());
    }

    public static class TimeMap extends IntervalTree<Long,Task> implements Consumer<Task> {

        public TimeMap(NAR n) {
            n.forEachConceptTask(true,true,false,false,this);
        }

        @Override
        public void accept(Task task) {
            if (!task.isEternal()) {
                put(task.occurrence(), task);
            }
        }

    }

    @Test public void testTemporalStability() {

        Global.DEBUG = true;
        Default n = new Default(1024, 8, 4, 3);

        n.inputAt(1, "a:b. :|:");
        n.inputAt(2, "b:c. :|:");
        n.inputAt(5, "c:d. :|:");

        n.run(10);

        {
            TimeMap m = new TimeMap(n);
            print(n, m);
            assertEquals("[[1..1], [2..2], [5..5]]", m.keySetSorted().toString());
        }

        Set<Task> bad = Global.newHashSet(1);

        for (int i = 0; i < 15; i++) {

            n.step();
            TimeMap m = new TimeMap(n);
            print(n, m);
            for (Task tt : m.values()) {
                if (bad.contains(tt)) continue; //already detected

                long o = tt.occurrence();
                if ((o!=1) && (o!=2) && (o!=5)) {
                    System.err.println("Temporal Instability: " + tt + "\n" + tt.explanation() + "\n");
                    bad.add(tt);
                }
            }
            assertEquals("[[1..1], [2..2], [5..5]]", m.keySetSorted().toString());
        }
    }

    static void print(Default n, TimeMap m) {
        out.println(n.time() + ": " + "Total tasks: " + m.size() + ", Total Times: " + m.keySet().size());
    }


}

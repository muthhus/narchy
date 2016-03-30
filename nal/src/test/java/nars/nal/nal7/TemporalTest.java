package nars.nal.nal7;

import nars.Global;
import nars.NAR;
import nars.nar.Default;
import nars.task.Task;
import nars.util.time.IntervalTree;
import org.junit.Test;

import java.util.Set;
import java.util.function.Consumer;

import static java.lang.System.out;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class TemporalTest {


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

    //TODO make more of these tests with different niputs
    @Test public void testTemporalStability() {

        int cycles = 300; //increase for more thorough testing

        Global.DEBUG = true;
        Default n = new Default(1024, 8, 4, 3);

        n.inputAt(1, "a:b. :|:");
        n.inputAt(2, "b:c. :|:");
        n.inputAt(5, "c:d. :|:");
        //n.log();

        Set<Task> irregular = Global.newHashSet(1);

        for (int i = 0; i < cycles; i++) {

            n.step();
            TimeMap m = new TimeMap(n);
            //Set<Between<Long>> times = m.keySetSorted();
            /*if (times.size() < 3)
                continue; //wait until the initial temporal model is fully constructed*/

            print(n, m);

            for (Task tt : m.values()) {

                long o = tt.occurrence();
                if ((o!=1) && (o!=2) && (o!=5)) {
                    if (irregular.add(tt)) { //already detected?
                        System.err.println("Temporal Instability: " + tt + "\n" + tt.explanation() + "\n");
                        irregular.add(tt);
                    }
                }
            }


            //assertEquals("[[1..1], [2..2], [5..5]]", times.toString());
        }

        assertTrue(irregular.isEmpty());
    }

    static void print(Default n, TimeMap m) {
        out.println(n.time() + ": " + "Total tasks: " + m.size() + "\t" + m.keySetSorted().toString());
    }



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
}

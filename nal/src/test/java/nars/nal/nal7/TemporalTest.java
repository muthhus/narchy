package nars.nal.nal7;

import nars.NAR;
import nars.nar.Default;
import nars.task.Task;
import nars.util.time.IntervalTree;
import org.junit.Assert;
import org.junit.Test;

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

        Default n = new Default(1024, 2, 3, 3);
        //n.core.activationRate.setValue(0.75f);

        n.inputAt(1, "a:b. :|:");
        n.inputAt(2, "b:c. :|:");
        n.inputAt(5, "c:d. :|:");

        n.run(10);

        TimeMap m = new TimeMap(n);
        out.println("Total tasks: " + m.size() + ", Total Times: " + m.keySet().size());
        assertEquals("[[1..1], [2..2], [5..5]]", m.keySetSorted().toString());


    }


}

package nars.nal.nal7;

import nars.NAR;
import nars.nar.Default;
import nars.task.Task;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class TemporalTest {


    @Test public void parsedCorrectOccurrenceTime() {
        NAR n = new Default(); //for cycle/frame clock, not realtime like Terminal
        Task t = n.inputTask("<a --> b>. :\\:");
        assertEquals(0, t.creation());
        assertEquals(-(1 /*n.duration()*/), t.occurrence());
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

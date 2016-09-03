package nars.task;

import nars.NAR;
import nars.time.Tense;
import nars.nar.Default;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created by me on 6/8/15.
 */
public class MutableTaskTest {

    @Test public void testTenseEternality() {
        NAR n = new Default();

        String s = "<a --> b>.";

        assertTrue(Tense.isEternal(n.task(s).occurrence()));

        assertTrue("default is eternal", n.task(s).isEternal());

        assertTrue("tense=eternal is eternal", Tense.isEternal(((MutableTask)n.task(s)).eternal().occurrence()));

        assertTrue("present is non-eternal", !Tense.isEternal(((MutableTask)n.task(s)).present(n).occurrence()));

    }

    @Test public void testTenseOccurrenceOverrides() {

        NAR n = new Default();

        String s = "<a --> b>.";

        //the final occurr() or tense() is the value applied
        assertTrue(!Tense.isEternal(((MutableTask)n.task(s)).eternal().occurr(100).occurrence()));
        assertTrue(!Tense.isEternal(((MutableTask)n.task(s)).eternal().present(n).occurrence()));
        assertTrue(Tense.isEternal(((MutableTask)n.task(s)).occurr(100).eternal().occurrence()));
    }


//    @Test public void testStampTenseOccurenceOverrides() {
//
//        NAR n = new NAR(new Default());
//
//        Task parent = n.task("<x --> y>.");
//
//
//        String t = "<a --> b>.";
//
//
//        Stamper st = new Stamper(parent, 10);
//
//        //the final occurr() or tense() is the value applied
//        assertTrue(!n.memory.task(n.term(t)).eternal().stamp(st).isEternal());
//        assertTrue(n.memory.task(n.term(t)).stamp(st).eternal().isEternal());
//        assertEquals(20, n.memory.task(n.term(t)).judgment().parent(parent).stamp(st).occurr(20).get().getOccurrenceTime());
//    }

}

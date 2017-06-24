package nars.task;

import nars.NAR;
import nars.Narsese;
import nars.nar.NARBuilder;
import nars.time.Tense;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created by me on 6/8/15.
 */
public class MutableTaskTest {

    @Test public void testTenseEternality() throws Narsese.NarseseException {
        NAR n = new NARBuilder().get();

        String s = "<a --> b>.";

        assertTrue(n.task(s).start() == Tense.ETERNAL);

        assertTrue("default is eternal", n.task(s).isEternal());

        assertTrue("tense=eternal is eternal", n.task(s).start() == Tense.ETERNAL);

        //assertTrue("present is non-eternal", !Tense.isEternal(((Task)n.task(s)).present(n).start()));

    }

//    @Test public void testTenseOccurrenceOverrides() throws Narsese.NarseseException {
//
//        NAR n = new Default();
//
//        String s = "<a --> b>.";
//
//        //the final occurr() or tense() is the value applied
//        assertTrue(!Tense.isEternal(((Task)n.task(s)).eternal().occurr(100).start()));
//        assertTrue(!Tense.isEternal(((TaskBuilder)n.task(s)).eternal().present(n).start()));
//        assertTrue(Tense.isEternal(((TaskBuilder)n.task(s)).occurr(100).eternal().start()));
//    }


//    @Test public void testStampTenseOccurenceOverrides() throws Narsese.NarseseException {
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

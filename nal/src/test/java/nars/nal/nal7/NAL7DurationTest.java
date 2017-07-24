package nars.nal.nal7;

import com.google.common.collect.Lists;
import nars.*;
import nars.concept.BaseConcept;
import nars.concept.dynamic.DynamicBeliefTable;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by me on 2/2/17.
 */
public class NAL7DurationTest {

    @Test
    public void testLinearTruthpolation() throws Narsese.NarseseException {
        NAR n = new NARS().get();
        n.time.dur(5);
        n.inputAt(10, "(x). :|:");
        n.run(10);
        //with duration = 5, the evidence surrounding a point
        // belief/goal will decay in the +/- 2.5 radius of time surrounding it.

        n.conceptualize("(x)").print();

        assertEquals(0.85f, n.beliefTruth("(x)",  7).conf(), 0.01f );
        assertEquals(0.86f, n.beliefTruth("(x)",  8).conf(), 0.01f );
        assertEquals(0.88f, n.beliefTruth("(x)",  9).conf(), 0.01f );
        assertEquals(0.90f, n.beliefTruth("(x)", 10).conf(), 0.01f );
        assertEquals(0.88f, n.beliefTruth("(x)", 11).conf(), 0.01f );
        assertEquals(0.86f, n.beliefTruth("(x)", 12).conf(), 0.01f );
        //assertNull( n.beliefTruth("(x)", 13000) );

    }

    @Test
    public void testDurationDithering() throws Narsese.NarseseException {
        NAR n = NARS.shell();
        n.time.dur(5);
        assertEquals( $.$("((x) &| (y))"), n.term("((x) &&+1 (y))"));
        assertEquals( $.$("((x) &| (y))"), n.term("((x) &&-1 (y))"));
        assertEquals( "(&|,(x),(y),(z))", n.term("(((x) &&+1 (y)) &&+1 (z))").toString());
        assertEquals( $.$("((x) &&+6 (y))"), n.term("((x) &&+6 (y))"));
        assertEquals( $.$("((x) =|> (y))"), n.term("((x) ==>+1 (y))"));
        assertEquals( $.$("((x) <|> (y))"), n.term("((x) <=>+1 (y))"));

    }

    @Test
    public void testTemporalIntersection() throws Narsese.NarseseException {

        //this.activeTasks = activeTasks;
        NAR n = NARS.tmp();

        //n.log();
        n.inputAt( 0,"a:x. :|:");
        n.inputAt(10,"a:y. :|:");
        n.run(128);

        assertDuration(n, "a:(x|y)", 5, 5);
        assertDuration(n, "a:(x&y)", 5, 5);
        assertDuration(n, "a:(y~x)", 5, 5);
        assertDuration(n, "a:(x~y)", 5, 5);
        //assertDuration(n, "(x<->y)", 5, 5);

        //n.concept("(x-->a)").print();
        //n.concept("(y-->a)").print();
    }

    @Test public void testDurationIntersection() {
        /*
        WRONG: t=25 is not common to both; 30 is however
        $.12 ((happy|i)-->L). 25 %.49;.81% {37: b;k} (((%1-->%2),(%3-->%2),task("."),notSet(%3),notSet(%1),neqRCom(%3,%1)),(((%1|%3)-->%2),((Intersection-->Belief))))
            $.25 (i-->L). 30 %.84;.90% {30: k}
            $.22 (happy-->L). 20⋈30 %.58;.90% {20: b}
        */

    }

    static void assertDuration(NAR n, String c, long start, long end) throws Narsese.NarseseException {
        BaseConcept cc = (BaseConcept) n.conceptualize(c);
        assertNotNull(c + " unconceptualized", cc);

        List<Task> tt = Lists.newArrayList(cc.beliefs());
        assertTrue(c + " not believed", cc.beliefs() instanceof DynamicBeliefTable || tt.size() > 0);

        if (tt.size() > 0) {
            Task t = tt.get(0);
            //System.out.println(sim.proof());
            //System.out.println(sim.start() + ".." + /*sim.occurrence() + ".."*/ + sim.end());
            assertEquals(start, t.start());
            assertEquals(end, t.end());
        }
    }
}

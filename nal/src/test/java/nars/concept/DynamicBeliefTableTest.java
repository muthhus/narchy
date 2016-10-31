package nars.concept;

import nars.$;
import nars.NAR;
import nars.nar.Default;
import nars.truth.Truth;
import org.junit.Test;

import static nars.$.$;
import static org.junit.Assert.*;

/**
 * Created by me on 10/27/16.
 */
public class DynamicBeliefTableTest {

    @Test
    public void testDynamicConjunction2() {
        NAR n = new Default();
        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);
        n.run(1);
        Truth now = n.concept($("(a:x && a:y)"), true).belief(n.time());
        assertEquals($.t(1f,0.81f),now);

        //n.ask("(a:x && a:y)")
    }

    @Test
    public void testDynamicConjunction3() {
        NAR n = new Default();
        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);
        n.believe("a:z", 1f, 0.9f);
        n.run(1);

        {
            Concept cc = n.concept($("(&&, a:x, a:y, a:z)"), true);
            Truth now = cc.belief(n.time());
            assertEquals($.t(1f, 0.73f), now);
            //the truth values were provided despite the belief tables being empty:
            assertTrue(cc.beliefs().isEmpty());
        }

        //test unknown:
        {
            Concept ccn = n.concept($("(&&, a:x, a:w)"), true);
            Truth nown = ccn.belief(n.time());
            assertNull(nown);
        }

        //test negation:
        {
            Concept ccn = n.concept($("(&&, a:x, (--, a:y), a:z)"), true);
            Truth nown = ccn.belief(n.time());
            assertEquals(ccn.toString(), $.t(0f, 0.73f), nown);
        }

        //test change after a component's revision:
        {
            n.believe("a:y", 0, 0.95f);
            n.run(1);
            Concept ccn = n.concept("(&&, a:x, a:y, a:z)");
            Truth now2 = ccn.belief(n.time());
            assertTrue(now2.freq() < 0.4f);
        }

    }

    @Test
    public void testDynamicConjunction2Temporal() {
        NAR n = new Default();
        n.believe($("a:x"), (long)0, 1f, 0.9f);
        n.believe($("a:y"), (long)4, 1f, 0.9f);
        n.run(1);
        CompoundConcept cc = (CompoundConcept) n.concept($("(a:x && a:y)"), true);
        cc.print();



        Truth now = cc.belief(n.time());
        assertEquals($.t(1f,0.57f),now); //evidence decayed due to time

        n.run(1);
        cc.print();

        //n.ask("(a:x && a:y)")
    }

}
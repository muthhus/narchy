package nars.concept;

import nars.NAR;
import nars.nar.Default;
import nars.term.Compound;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by me on 5/28/16.
 */
public class ProxyCompoundConceptTest {

    @Test
    public void testProxy1() {
        NAR n = new Default();

        String abString = "(a --> b)";
        String cString = "(c)";

        n.input(abString + ".");
        n.step();

        CompoundConcept ab = (CompoundConcept) n.concept(abString);

        Compound cTerm = nars.$.$(cString);
        ProxyCompoundConcept C = new ProxyCompoundConcept(cTerm, ab, n);
        assertEquals(cTerm, C);
        assertEquals(C, cTerm);

        Concept C1 = n.concept(cString);
        assertNotNull(C1);
        assertEquals(C, C1);
        assertEquals(C1, C);
        assertTrue(C1 == C);
        assertEquals(2, C1.termlinks().size());

        Concept R = n.concept(abString);
        assertEquals(ProxyCompoundConcept.class, R.getClass());
        System.out.println(R + " " + ((ProxyCompoundConcept)R).toStringActual());

        assertEquals(ProxyCompoundConcept.class, n.concept(cString).getClass());
        assertTrue(n.concept(abString) == n.concept(cString));
    }

}
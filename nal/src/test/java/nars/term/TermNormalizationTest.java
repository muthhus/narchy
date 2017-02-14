package nars.term;

import nars.Narsese;
import org.junit.Test;

import static nars.$.$;
import static org.junit.Assert.*;

/**
 * Created by me on 3/19/15.
 */
public class TermNormalizationTest {

    @Test
    public void reuseVariableTermsDuringNormalization2() throws Narsese.NarseseException {
        for (String v : new String[] { "?a", "?b", "#a", "#c" }) {
            Compound eq = $("<<" + v +" --> b> <=> <" + v + " --> c>>");
            Term a = eq.subterm((byte)0, (byte)0);
            Term b = eq.subterm((byte)1, (byte)0);
            assertNotEquals(a, eq.subterm((byte)0, (byte)1));
            assertEquals(eq + " subterms (0,0)==(1,0)", a, b);
            assertTrue(a == b);
        }
    }

}

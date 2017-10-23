package nars.term;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 3/19/15.
 */
public class TermNormalizationTest {

    @Test
    public void reuseVariableTermsDuringNormalization2() throws Narsese.NarseseException {
        for (String v : new String[] { "?a", "?b", "#a", "#c" }) {
            Compound x = $("<<" + v +" --> b> ==> <" + v + " --> c>>");
            Term a = x.sub((byte)0, (byte)0);
            Term b = x.sub((byte)1, (byte)0);
            assertNotEquals(a, x.sub((byte)0, (byte)1));
            assertEquals(a, b, x + " subterms (0,0)==(1,0)");
            assertTrue(a == b);
        }
    }

    @Test public void testConjNorm() throws Narsese.NarseseException {
        String a = "(&&,(#1-->key),(#2-->lock),open(#1,#2))";
        String b = "(&&,(#2-->key),(#1-->lock),open(#2,#1))";

        assertEquals($.$(a), $.$(b));

        NAR t = NARS.shell();
        assertEquals($.$(a), $.$(b));
    }
}

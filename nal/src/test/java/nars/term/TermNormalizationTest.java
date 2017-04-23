package nars.term;

import nars.$;
import nars.Narsese;
import nars.nar.Terminal;
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
            Term a = eq.sub((byte)0, (byte)0);
            Term b = eq.sub((byte)1, (byte)0);
            assertNotEquals(a, eq.sub((byte)0, (byte)1));
            assertEquals(eq + " subterms (0,0)==(1,0)", a, b);
            assertTrue(a == b);
        }
    }

    @Test public void testConjNorm() throws Narsese.NarseseException {
        String a = "(&&,(#1-->key),(#2-->lock),open(#1,#2))";
        String b = "(&&,(#2-->key),(#1-->lock),open(#2,#1))";

        assertEquals($.$(a), $.$(b));

        Terminal t = new Terminal();
        assertEquals(t.term(a), t.term(b));
    }
}

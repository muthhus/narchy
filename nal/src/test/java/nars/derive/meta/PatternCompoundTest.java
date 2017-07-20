package nars.derive.meta;

import nars.Narsese;
import nars.index.term.PatternTermIndex;
import nars.term.Compound;
import nars.term.Term;
import org.junit.Test;

import static nars.$.$;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;
import static org.junit.Assert.*;

public class PatternCompoundTest {

    @Test
    public void testPatternCompoundWithXTERNAL() throws Narsese.NarseseException {
        Compound p = (Compound) new PatternTermIndex().get($("((x) ==>+- (y))"), true).term();
        assertEquals(PatternCompound.PatternCompoundSimple.class, p.getClass());
        assertEquals(XTERNAL, p.dt());
    }

    @Test
    public void testEqualityWithNonPatternDT() throws Narsese.NarseseException {
        for (String s : new String[] { "(a ==> b)", "(a ==>+1 b)", "(a &&+1 b)" }) {
            Compound t = $(s);
            Compound p = (Compound) new PatternTermIndex().get(t, true).term();
            assertEquals(PatternCompound.PatternCompoundSimple.class, p.getClass());
            assertEquals(t.dt(), p.dt());
            assertEquals(t, p);
        }
    }
}
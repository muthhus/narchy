package nars.term;

import nars.Op;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import static nars.$.*;
import static nars.Op.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * test term hash and structure bits
 */
public class TermHashTest {

    @Test
    public void testStructureIsVsHas() {

        assertTrue(inh("a", "b").hasAny(Op.ATOM));
        assertTrue(inh(p("a"), $("b"))
                .hasAny(or(Op.ATOM, Op.PRODUCT)));

        assertFalse(inh(p("a"), $("b"))
                .isAnyOf(or(SIMILAR, Op.PRODUCT)));
        assertFalse(inh(p("a"), $("b"))
                .op() == Op.PRODUCT);

        assertTrue(inh("a", "b").op() == INHERIT);
        assertTrue(inh("a", "b").hasAny(INHERIT));
        assertTrue(inh("a", "b").hasAny(Op.ATOM));
        assertFalse(inh("a", "b").hasAny(SIMILAR));
    }

    @Test public void testHasAnyVSAll() {
        @Nullable Compound iii = impl(inh("a", "b"), $("c"));
        assertTrue(iii.hasAll(or(IMPLICATION, INHERIT)));
        assertFalse(iii.hasAll(or(IMPLICATION, SIMILAR)));
        assertTrue(iii.hasAny(or(IMPLICATION, INHERIT)));

    }

//    @Test
//    public void testTemporalBits() {
//        Term x = $("<(&&,%1,%2)=\\>%3>");
//        assertTrue(x
//                .isAny(Op.TemporalBits));
//
//        TestCase.assertFalse(x
//                .isAny(Op.CONJUNCTION.bit()));
//        assertTrue(x
//                .hasAny(Op.CONJUNCTION.bit()));
//
//    }

}

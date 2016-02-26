package nars.term;

import nars.Op;
import org.junit.Test;

import static nars.$.*;
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
                .hasAny(Op.or(Op.ATOM, Op.PRODUCT)));

        assertFalse(inh(p("a"), $("b"))
                .isAnyOf(Op.or(Op.SIMILAR, Op.PRODUCT)));
        assertFalse(inh(p("a"), $("b"))
                .op() == Op.PRODUCT);

        assertTrue(inh("a", "b").op() == Op.INHERIT);
        assertTrue(inh("a", "b").hasAny(Op.INHERIT));
        assertTrue(inh("a", "b").hasAny(Op.ATOM));
        assertFalse(inh("a", "b").hasAny(Op.SIMILAR));
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

package nars.term;

import nars.Narsese;
import nars.Op;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import static nars.$.$;
import static nars.$.impl;
import static nars.$.inh;
import static nars.$.p;
import static nars.Op.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * test term hash and structure bits
 */
public class TermHashTest {

    @Test
    public void testStructureIsVsHas() throws Narsese.NarseseException {

        assertTrue(inh("a", "b").hasAny(Op.ATOM));
        assertTrue(inh(p("a"), $("b"))
                .hasAny(or(Op.ATOM, Op.PROD)));

        assertFalse(inh(p("a"), $("b"))
                .isAny(or(SIM, Op.PROD)));
        assertFalse(inh(p("a"), $("b"))
                .op() == Op.PROD);

        assertTrue(inh("a", "b").op() == INH);
        assertTrue(inh("a", "b").hasAny(INH));
        assertTrue(inh("a", "b").hasAny(Op.ATOM));
        assertFalse(inh("a", "b").hasAny(SIM));
    }

    @Test public void testHasAnyVSAll() throws Narsese.NarseseException {
        @Nullable Term iii = impl(inh("a", "b"), $("c"));
        assertTrue(iii.hasAll(or(IMPL, INH)));
        assertFalse(iii.hasAll(or(IMPL, SIM)));
        assertTrue(iii.hasAny(or(IMPL, INH)));

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

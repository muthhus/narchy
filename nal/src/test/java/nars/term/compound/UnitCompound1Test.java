package nars.term.compound;

import nars.$;
import nars.index.term.tree.TermKey;
import nars.term.Compound;
import nars.term.atom.Atomic;
import nars.term.container.TermVector;
import org.junit.Test;

import java.util.Arrays;

import static nars.Op.PROD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 11/16/16.
 */
public class UnitCompound1Test {

    @Test
    public void testUnitCompound1() {
        Atomic x = Atomic.the("x");
        UnitCompound1 u = new UnitCompound1(PROD, x);
        Compound g = new GenericCompound(PROD, TermVector.the(x));
        assertEquals(g.hashCode(), u.hashCode());
        assertEquals(u, g);
        assertEquals(g, u);
        assertEquals(0, u.compareTo(g));
        assertEquals(0, g.compareTo(u));
        assertEquals(g.toString(), u.toString());
        assertTrue(Arrays.equals(TermKey.term(g).array(), TermKey.term(u).array()));
    }

    @Test
    public void testUnitCompound2() {
        Atomic x = Atomic.the("x");
        Compound c = $.p(x);
        System.out.println(c);
        System.out.println(c.get(0));

        Compound d = $.inh(x, Atomic.the("y"));
        System.out.println(d);
    }
    @Test
    public void testUnitCompound3() {
        Atomic x = Atomic.the("x");
        Atomic y = Atomic.the("y");
        Compound c = $.func(x, y);
        System.out.println(c);
        System.out.println(c.get(0));
        System.out.println(c.get(1));
    }

}
package nars.term.container;

import nars.$;
import nars.Narsese;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.junit.Test;

import static nars.$.$;
import static org.junit.Assert.*;

/**
 * Created by me on 3/1/16.
 */
public class TermContainerTest {

    @Test
    public void testCommonSubterms() throws Narsese.NarseseException {
        assertTrue(TermContainer.commonSubtermOrContainment($("x"), $("x")));
        assertFalse(TermContainer.commonSubtermOrContainment($("x"), $("y")));
        assertTrue(TermContainer.commonSubtermOrContainment($("(x,y,z)"), $("y")));
        assertFalse(TermContainer.commonSubtermOrContainment($("(x,y,z)"), $("w")));
        assertFalse(TermContainer.commonSubterms($("(a,b,c)"), $("(x,y,z)"), false));
        assertTrue(TermContainer.commonSubterms($("(x,y)"), $("(x,y,z)"), false));
    }

    @Test
    public void testCommonSubtermsRecursion() throws Narsese.NarseseException {
        assertTrue(TermContainer.commonSubterms($("(x,y)"), $("{a,x}"), false));
        assertFalse(TermContainer.commonSubterms($("(x,y)"), $("{a,b}"), false));

        assertFalse(TermContainer.commonSubterms($("(#x,y)"), $("{a,#x}"), true));
        assertTrue(TermContainer.commonSubterms($("(#x,a)"), $("{a,$y}"), true));
    }

    @Test
    public void testUnionReusesInstance() throws Narsese.NarseseException {
        Compound container = $("{a,b}");
        Compound contained = $("{a}");
        assertTrue(
            $.terms.union(container.op(), container, contained) == container
        );
        assertTrue(
            $.terms.union(contained.op(), contained, container) == container  //reverse
        );
        assertTrue(
            $.terms.union(container.op(), container, container) == container  //equal
        );
    }

    @Test
    public void testDifferReusesInstance() throws Narsese.NarseseException {
        Compound x = $("{x}");
        Compound y = $("{y}");
        assertTrue(
                $.terms.difference(x.op(), x, y) == x
        );
    }
    @Test
    public void testIntersectReusesInstance() throws Narsese.NarseseException {
        Compound x = $("{x,y}");
        Compound y = $("{x,y}");
        assertTrue(
                $.terms.intersect(x.op(), x, y) == x
        );
    }

    @Test
    public void testSomething() throws Narsese.NarseseException {
        Compound x = $("{e,f}");
        Compound y = $("{e,d}");

        System.out.println($.terms.intersect(x.op(), x, y));
        System.out.println($.terms.difference(x.op(), x, y));
        System.out.println($.terms.union(x.op(), x, y));

    }

    @Test
    public void testEqualityOfVector1() {
        Term a = Atomic.the("a");
        TermContainer x = TermVector.the(a);
        TermContainer y = TermVector.the(a);
        assertEquals(x, y);

        TermContainer z = new ArrayTermVector(a);
        assertEquals(z.hashCode(), x.hashCode());
        assertEquals(x, z);
        assertEquals(z, x);


    }

}
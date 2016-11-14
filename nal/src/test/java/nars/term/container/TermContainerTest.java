package nars.term.container;

import nars.$;
import nars.term.Compound;
import org.junit.Test;

import static nars.$.$;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 3/1/16.
 */
public class TermContainerTest {

    @Test
    public void testCommonSubterms() {
        assertTrue(TermContainer.commonSubtermOrContainment($("x"), $("x")));
        assertFalse(TermContainer.commonSubtermOrContainment($("x"), $("y")));
        assertTrue(TermContainer.commonSubtermOrContainment($("(x,y,z)"), $("y")));
        assertFalse(TermContainer.commonSubtermOrContainment($("(x,y,z)"), $("w")));
        assertFalse(TermContainer.commonSubterms($("(a,b,c)"), $("(x,y,z)")));
        assertTrue(TermContainer.commonSubterms($("(x,y)"), $("(x,y,z)")));
    }

    @Test
    public void testCommonSubtermsRecursion() {
        assertTrue(TermContainer.commonSubterms($("(x,y)"), $("{a,x}")));
        assertFalse(TermContainer.commonSubterms($("(x,y)"), $("{a,b}")));

        assertFalse(TermContainer.commonSubterms($("(#x,y)"), $("{a,#x}"), true));
        assertTrue(TermContainer.commonSubterms($("(#x,a)"), $("{a,$y}"), true));
    }

    @Test
    public void testUnionReusesInstance() {
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
    public void testDifferReusesInstance() {
        Compound x = $("{x}");
        Compound y = $("{y}");
        assertTrue(
                $.terms.difference(x.op(), x, y) == x
        );
    }
    @Test
    public void testIntersectReusesInstance() {
        Compound x = $("{x,y}");
        Compound y = $("{x,y}");
        assertTrue(
                $.terms.intersect(x.op(), x, y) == x
        );
    }

    @Test
    public void testSomething() {
        Compound x = $("{e,f}");
        Compound y = $("{e,d}");

        System.out.println($.terms.intersect(x.op(), x, y));
        System.out.println($.terms.difference(x.op(), x, y));
        System.out.println($.terms.union(x.op(), x, y));

    }
}
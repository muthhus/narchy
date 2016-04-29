package nars.term.container;

import nars.term.Compound;
import nars.term.Terms;
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
        assertTrue(TermContainer.commonSubterms($("(x,y)"), $("{a, [x, b]}")));
        assertFalse(TermContainer.commonSubterms($("(x,y)"), $("{a, [c, b]}")));
    }

    @Test
    public void testUnionReusesInstance() {
        Compound container = $("{a,b}");
        Compound contained = $("{a}");
        assertTrue(
            TermSet.union(Terms.terms, container, contained) == container
        );
        assertTrue(
            TermSet.union(Terms.terms, contained, container) == container  //reverse
        );
        assertTrue(
            TermSet.union(Terms.terms, container, container) == container  //equal
        );
    }

    @Test
    public void testDifferReusesInstance() {
        Compound x = $("{x}");
        Compound y = $("{y}");
        assertTrue(
                TermContainer.difference(Terms.terms, x, y) == x
        );
    }
    @Test
    public void testIntersectReusesInstance() {
        Compound x = $("{x,y}");
        Compound y = $("{x,y}");
        assertTrue(
                TermContainer.intersect(Terms.terms, x, y) == x
        );
    }
}
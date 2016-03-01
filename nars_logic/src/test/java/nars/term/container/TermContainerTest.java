package nars.term.container;

import nars.$;
import org.junit.Test;

import static nars.$.$;
import static org.junit.Assert.*;

/**
 * Created by me on 3/1/16.
 */
public class TermContainerTest {

    @Test
    public void testCommonSubterms() {
        assertTrue(TermContainer.commonSubterms($("x"), $("x")));
        assertFalse(TermContainer.commonSubterms($("x"), $("y")));
        assertTrue(TermContainer.commonSubterms($("(x,y,z)"), $("y")));
        assertFalse(TermContainer.commonSubterms($("(x,y,z)"), $("w")));
        assertFalse(TermContainer.commonSubterms($("(a,b,c)"), $("(x,y,z)")));
        assertTrue(TermContainer.commonSubterms($("(x,y)"), $("(x,y,z)")));
    }
}
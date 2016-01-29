package nars.op.software.prolog;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by me on 1/29/16.
 */
public class PrologMainTest {

    @Test
    public void testAsk() {
        PrologMain p = new PrologMain();

        assertEquals("the(eq(V_0,V_0))", p.ask("eq(X,X)"));
        assertEquals("the(eq(1,1))", p.ask("eq(X,1)"));
        assertEquals("the(eq(1,1))", p.ask("eq(1,1)"));
    }
}
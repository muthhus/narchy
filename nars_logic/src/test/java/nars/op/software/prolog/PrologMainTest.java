package nars.op.software.prolog;

import nars.op.software.prolog.terms.PTerm;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by me on 1/29/16.
 */
public class PrologMainTest {

    @Test
    public void testAsk() {
        PrologMain p = new PrologMain();
        assertEquals("the(eq(V_0,V_0))", p.ask("eq(X,X)").pprint());
        assertEquals("the(eq(1,1))", p.ask("eq(X,1)").pprint());
        assertEquals("the(eq(1,1))", p.ask("eq(1,1)").pprint());
    }

    @Test
    public void test1() {
        PrologMain p = new PrologMain();
        p.ask("['/tmp/x'].");
        PTerm r = p.ask("mid.");
        System.out.println(r);

        //assertEquals("the(['/tmp/x'])", p.goal("['/tmp/x'].").pprint());
        //assertEquals(null, p.ask("small.").pprint());

    }
}
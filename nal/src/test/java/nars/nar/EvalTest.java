package nars.nar;

import nars.$;
import nars.NAR;
import nars.task.Task;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 3/9/16.
 */
public class EvalTest {

    static final Terminal t = new Terminal(8);

    @Test
    public void testCommandDefault() {
        Task a = t.task("(a b c)");
        assertNotNull(a);
        assertTrue(a.isCommand());
        assertEquals($.$("(a b c)"), a.term());
    }

    @Test public void testEval1() {
        assertEquals("(1)",
                t.eval(
                        //"(add 1 2)"
                        "(list 1)"
                ).toString());
    }

    @Test public void testStaticMethodInvoke() {
        assertEquals(System.getProperty("java.vm.version"),
                t.eval("(System/getProperty \"java.vm.version\")").toString() );
    }
    @Test public void testClojuredCompound() {
        assertEquals("[\"==>\" ([\"-->\" (a b)] (println x))]", t.eval("(quote <<a-->b> ==> (println x)>)").toString());
    }

    @Test public void testClojure2() {
            //t.eval("(\"org.junit.Assert/assertTrue\" false)");
        NAR n = new Default();
        n.log();
        n.input("a:b!");
        //n.input("<(println (System/getProperty \"java.vm.version\"))==>a:b>.");
        n.input("<(println 1)==>a:b>.");
        //n.input("<echo(y)==>a:b>.");
        //n.input("echo(z)!");
        n.run(16);

    }
}

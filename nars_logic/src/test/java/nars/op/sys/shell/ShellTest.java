package nars.op.sys.shell;

import nars.$;
import nars.Global;
import nars.Memory;
import nars.NAR;
import nars.nar.Default;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created by me on 2/24/16.
 */
public class ShellTest {

    static {
        Global.DEBUG = true;
    }
    final NAR d = new Default(1024, 8, 2, 3);
    final shell s = new shell(d);

    public ShellTest() throws Exception {
    }

    @Test
    public void testLobjectized()  {

        System.out.println(d.exe);

        assertTrue(d.exe.containsKey($.operator("sh")));

        d.log();
        d.input("sh(pwd,I,(),#x)!");
        d.run(5);
        //expect: ("file:///home/me/opennars/nars_logic"-->(/,^sh,pwd,I,(),_)). :|: %1.0;.90%
    }

    @Test
    public void testWrappedDirectoryConcept()  {

        System.out.println(d.exe);

        assertTrue(d.exe.containsKey($.operator("sh")));

        d.log();

        d.input("(go($w) <=> sh($w,I,(),#z)). %1.0;0.95%");
        d.input("go(ls)! :|:");

        d.run(145);
        //expect: ("file:///home/me/opennars/nars_logic"-->(/,^sh,pwd,I,(),_)). :|: %1.0;.90%
    }
}
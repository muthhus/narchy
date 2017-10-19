package nars.op.java;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OObjectsTest {

    static class SimpleClass {
        private int v;

        public void set(int x) {
            this.v = x;
        }

        public int get() {
            return v;
        }
    }

    final NAR n = NARS.tmp();

    final OObjects objs = new OObjects(n);

    final SimpleClass x = objs.the("x", SimpleClass.class);

    @Test public void testSelfInvocation() throws Narsese.NarseseException {
        n.log();

        StringBuilder sb = new StringBuilder();
        n.onTask(sb::append);

        n.input("SimpleClass(set,x,(1))! :|:");
        n.run(1);
        n.run(1);

        n.input("SimpleClass(get,x,(),#y)! :|:");
        n.run(1);
        n.run(1);

        assertEquals("",sb.toString());

    }

    @Test public void testExternalInvocation() {
        n.log();
        n.run(1);
        {
            x.get();
        }
        n.run(1);
        {
            x.set(1);
        }
        n.run(1);
    }
}
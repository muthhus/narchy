package nars.op.java;

import nars.NAR;
import nars.NARS;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

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

    @Test
    public void testOObjects1() {
        NAR n = NARS.tmp();

        n.log();

        OObjects objs = new OObjects(n);
        SimpleClass x = objs.the("x", SimpleClass.class);
        System.out.println(x + " " + x.getClass());

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
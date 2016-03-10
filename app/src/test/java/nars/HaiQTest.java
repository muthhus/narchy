package nars;

import nars.NAR;
import nars.op.java.Lobjects;
import nars.nar.Default;
import nars.op.NarQ;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by me on 2/2/16.
 */
public class HaiQTest {

    @Ignore
    @Test
    public void testSimple() throws Exception {

        NAR n = new Default(200, 4, 3, 2);
        //n.log();

        new Lobjects(n).the("q", NarQ.class, n);
        n.input("hai(set, q, ({x:0,x:1}, {reward:xy}, {y:0, y:1}), #z);");

        n.input("x:0. :\\: %0.25%");
        n.input("(x:0 ==>+5 (--,x:1)). :|: %0.99%");
        n.run(1);
        n.input("x:0. :/: %0.5%");
        n.run(1);
        n.input("x:1. :|: %0.5%");
        n.run(1);
        n.input("reward:xy. :/: %0.75%");
        n.run(10);
        //n.input("x:1. :|: %0%");
        n.run(50);

        //n.concept("(1-->x)").print();
    }
}
package nars.op;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.term.Term;
import org.junit.Test;

import static nars.$.$;
import static org.junit.Assert.assertEquals;

public class ImplierTest {

    @Test
    public void testImplier1() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        Term x = $("x");
        Term y = $("y");
        Implier imp = new Implier(n, y);

        n.log();
        n.input("(x ==> y). :|:");

        System.out.println(imp.graph);

        for (int i = 0; i < 2; i++) {

            n.run(1);
            System.out.println(imp.graph);
            assertEquals(2, imp.graph.nodeCount());
            assertEquals(1, imp.graph.edgeCount());
        }

        n.input("(z ==> x). :|:");
        n.run(1);
        System.out.println(imp.graph);
        n.run(1);
        System.out.println(imp.graph);
        System.out.println(imp.goals);

    }
}
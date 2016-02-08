package nars.bag.impl;

import nars.NAR;
import nars.nar.Default;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DigestBagTest {

    @Test public void testConfidenceCutoff() {

        NAR n = new Default();

        DigestBag d = new DigestBag.OutputBuffer(n, 4).buffer;

        n.input("a:b."); n.run(1);

        assertEquals(1, d.list.size());

        n.input("a:b. %1.0;0.75%"); n.run(1);
        n.input("a:b. %1.0;0.5%"); n.run(1);
        n.input("a:b. %1.0;0.25%"); n.run(1);

        assertEquals(4, d.list.size());

        n.input("a:b. %1.0;0.1%"); n.run(1);

        assertEquals(4, d.list.size());
        //d.list.print(System.out);

        /* ignored the 0.1 conf result */
        //revision adds results above, knocking out lower values like 0.25
        assertTrue(0.25f < d.list.getLast().get().conf());


    }

    @Test public void testPriorityCutoff() {

        NAR n = new Default();

        DigestBag d = new DigestBag.OutputBuffer(n, 2).buffer;

        n.input("$0.3$ a:b.");
        n.input("$0.2$ a:c.");
        n.input("$0.1$ a:d.");
        n.run(1);

        assertEquals(2, d.list.size());

        /* ignored the low budget result */
        assertTrue(d.list.getLast().get().toString().contains("(c-->a)"));

    }

}
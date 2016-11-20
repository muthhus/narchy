package nars.term;

import nars.$;
import nars.term.container.TermContainer;
import nars.term.container.TermSet;
import nars.term.container.TermVector;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by me on 11/12/15.
 */
public class TermVectorTest {

    @Test
    public void testSubtermsEquality() {

        Compound a = $.inh("a", "b");
        //return Atom.the(Utf8.toUtf8(name));

        //        int olen = name.length();
//        switch (olen) {
//            case 0:
//                throw new RuntimeException("empty atom name: " + name);
//
////            //re-use short term names
////            case 1:
////            case 2:
////                return theCached(name);
//
//            default:
//                if (olen > Short.MAX_VALUE/2)
//                    throw new RuntimeException("atom name too long");

        //  }
        //return Atom.the(Utf8.toUtf8(name));

        //        int olen = name.length();
//        switch (olen) {
//            case 0:
//                throw new RuntimeException("empty atom name: " + name);
//
////            //re-use short term names
////            case 1:
////            case 2:
////                return theCached(name);
//
//            default:
//                if (olen > Short.MAX_VALUE/2)
//                    throw new RuntimeException("atom name too long");

        //  }
        Compound b = (Compound) $.impl($.the("a"), $.the("b"));

        assertEquals(a.subterms(), b.subterms());
        assertEquals(a.subterms().hashCode(), b.subterms().hashCode());

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());

        assertEquals(0, TermContainer.compare(a.subterms(), b.subterms()));
        assertEquals(0, TermContainer.compare(b.subterms(), a.subterms()));

        assertNotEquals(0, a.compareTo(b));
        assertNotEquals(0, b.compareTo(a));

        /*assertTrue("after equality test, subterms vector determined shareable",
                a.subterms() == b.subterms());*/


    }

    @Test public void testSortedTermContainer() {
        TermContainer a = TermVector.the($.$("a"), $.$("b"));
        assertTrue(a.isSorted());
        TermContainer b = TermVector.the($.$("b"), $.$("a"));
        assertFalse(b.isSorted());
        TermContainer s = TermSet.the(b.terms());
        assertTrue(s.isSorted());
        assertEquals(a, s);
        assertNotEquals(b, s);
    }

}

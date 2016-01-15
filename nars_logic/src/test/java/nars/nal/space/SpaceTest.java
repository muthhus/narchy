package nars.nal.space;

import nars.$;
import nars.NAR;
import nars.concept.Concept;
import nars.nar.Default;
import nars.term.TermVector;
import nars.term.atom.Atom;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by me on 1/7/16.
 */
public class SpaceTest {
    final static Atom x = $.the("x");
    final static Atom y = $.the("y");
    final static Atom z = $.the("z");
    final static TermVector xy = new TermVector(x, y);
    final static TermVector xyz = new TermVector(x, y, z);

    @Test public void test1() {

        Space xy00 = new Space(xy, 0, 0);
        assertEquals("(+,x*0.0,y*0.0)", xy00.toString());
        assertEquals(xy00.hash2, new Space(xy, 0, 0).hash2);
        assertEquals(xy00.vector, new Space(xy, 0, 0).vector);
        assertEquals(xy00.subterms(), new Space(xy, 0, 0).subterms());
        assertEquals(xy00, xy00);
        assertEquals(0, xy00.compareTo(xy00));
        assertEquals(0, xy00.compareTo(new Space(xy, 0, 0)));
        assertEquals(xy00, new Space(xy, 0, 0));

        assertEquals(xy.reverse(), new TermVector(y, x)); //TODO move this to TermVctor test

        assertNotEquals(xy00, new Space(xy.reverse(), 0, 0));

        Space xy11 = new Space(xy, 1, 1);
        assertEquals("(+,x*1.0,y*1.0)", xy11.toString());
        assertNotEquals(xy00, xy11);

        assertNotEquals(0, xy11.compareTo(xy00));
        assertEquals(-xy00.compareTo(xy11), xy11.compareTo(xy00));

    }

    @Test public void testBlank() {
        assertEquals("(x+y)", new Space(xy).toString());
        assertEquals("(+,x,y,z)", new Space(xyz).toString());
    }


    @Test public void testSpaceConcept() {
        Space xyClass = new Space(xy);
        Space xy00 = new Space(xy, 0, 0);
        Space xy11 = new Space(xy, 1, 1);

        NAR n = new Default();
        n.believe(xy00);
        n.believe(xy11);

        Concept c = n.concept(xyClass);
        assertNotNull(c);
        assertEquals(SpaceConcept.class, c.getClass());
        Concept c2 = n.concept(xy11);
        assertTrue(c == c2);

        n.frame();

        c.print();
    }
    @Test public void testSpaceConcept3d() {
        Space xyClass = new Space(xyz);
        Space xy00 = new Space(xyz, 1, 0, 0);
        Space xy11 = new Space(xyz, 0, 1, 0);
        Space xy22 = new Space(xyz, 0, 0, 1);

        NAR n = new Default();
        n.believe(xy00);
        n.believe(xy11);
        n.believe(xy22);

        Concept c = n.concept(xyClass);

        n.frame();

        c.print();
    }

    @Test public void testInternalSpaceConcept() {
        float sqr2 = (float) Math.sqrt(2);
        Atom a = $.the("a");

        NAR n = new Default();
        Space xyClass = new Space(xy);
        n.believe($.sim(a, new Space(xy, sqr2, 0)));
        n.believe($.sim(a, new Space(xy, sqr2, sqr2)));
        n.believe($.sim(a, new Space(xy, 0, sqr2)));


        Concept c = n.concept($.sim(a, xyClass));

        n.frame();

        c.print();
    }
}
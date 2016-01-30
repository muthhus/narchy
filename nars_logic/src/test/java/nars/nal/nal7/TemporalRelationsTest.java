package nars.nal.nal7;

import nars.nar.Default;
import org.junit.Test;

import static nars.$.$;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by me on 1/12/16.
 */
public class TemporalRelationsTest {

    @Test
    public void parseTemporalRelation() {
        //TODO move to NarseseTest
        assertEquals("(x ==>+5 y)", $("(x ==>+5 y)").toString());
        assertEquals("(x &&+5 y)", $("(x &&+5 y)").toString());

        assertEquals("(x ==>-5 y)", $("(x ==>-5 y)").toString());

        assertEquals("((before-->x) ==>+5 (after-->x))", $("(x:before ==>+5 x:after)").toString());
    }
    @Test public void temporalEqualityAndCompare() {
        assertNotEquals( $("(x ==>+5 y)"), $("(x ==>+0 y)") );
        assertNotEquals( $("(x ==>+5 y)").hashCode(), $("(x ==>+0 y)").hashCode() );
        assertNotEquals( $("(x ==> y)"), $("(x ==>+0 y)") );
        assertNotEquals( $("(x ==> y)").hashCode(), $("(x ==>+0 y)").hashCode() );

        assertEquals( $("(x ==>+0 y)"), $("(x ==>-0 y)") );

        assertEquals(0,   $("(x ==>+0 y)").compareTo( $("(x ==>+0 y)") ) );
        assertEquals(-1,  $("(x ==>+0 y)").compareTo( $("(x ==>+1 y)") ) );
        assertEquals(+1,  $("(x ==>+1 y)").compareTo( $("(x ==>+0 y)") ) );
    }


    @Test public void testReversibilityOfCommutive() {
        assertEquals("(a <=>+5 b)", $("(a <=>+5 b)").toString());
        assertEquals("(a <=>-5 b)", $("(b <=>+5 a)").toString());
        assertEquals("(a <=>-5 b)", $("(a <=>-5 b)").toString());

        assertEquals("(a &&+5 b)", $("(a &&+5 b)").toString());
        assertEquals("(a &&-5 b)", $("(b &&+5 a)").toString());


    }

    @Test public void testConceptualization() {
        Default d = new Default();

        d.input("(x ==>+0 y)."); //eternal
        d.input("(x ==>+1 y)."); //eternal

        //d.index().print(System.out);
        //d.concept("(x==>y)").print();

        d.step();

        assertEquals(3, d.index().size() );

        assertEquals(2, d.concept("(x==>y)").beliefs().size() );

        d.input("(x ==>+1 y). :|:"); //present
        d.step();

        //d.concept("(x==>y)").print();

        assertEquals(4, d.concept("(x==>y)").beliefs().size() );

        assertEquals(3, d.index().size() ); //remains 3

        d.index().print(System.out);
        d.concept("(x==>y)").print();
    }

}

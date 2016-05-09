package nars.nal.nal7;

import nars.$;
import nars.NAR;
import nars.nar.Default;
import nars.term.Compound;
import nars.term.Term;
import org.junit.Test;

import java.util.TreeSet;

import static java.lang.System.out;
import static nars.$.$;
import static org.junit.Assert.*;

/**
 * Created by me on 1/12/16.
 */
public class TemporalRelationsTest {

    static final Term A = $("a");
    static final Term B = $("b");

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
        assertNotEquals( $("(x ==>+5 y)"), $("(y ==>-5 x)") );



        assertEquals(0,   $("(x ==>+0 y)").compareTo( $("(x ==>+0 y)") ) );
        assertEquals(-1,  $("(x ==>+0 y)").compareTo( $("(x ==>+1 y)") ) );
        assertEquals(+1,  $("(x ==>+1 y)").compareTo( $("(x ==>+0 y)") ) );
    }


    @Test public void testReversibilityOfCommutive() {
        for (String c : new String[] { "&&", "<=>" }) {
            assertEquals("(a "+c+"+5 b)", $("(a "+c+"+5 b)").toString());
            assertEquals("(a "+c+"-5 b)", $("(b "+c+"+5 a)").toString());
            assertEquals("(a "+c+"+5 b)", $("(b "+c+"-5 a)").toString());
            assertEquals("(a "+c+"-5 b)", $("(a "+c+"-5 b)").toString());

            assertEquals($("(b "+c+"-5 a)"), $("(a "+c+"+5 b)"));
            assertEquals($("(b "+c+"+5 a)"), $("(a "+c+"-5 b)"));
            assertEquals($("(a "+c+"-5 b)"), $("(b "+c+"+5 a)"));
            assertEquals($("(a "+c+"+5 b)"), $("(b "+c+"-5 a)"));
        }
    }

    @Test public void testCommutiveWithCompoundSubterm() {
        Term a = $("(((--,(b0)) &&+0 (pre_1)) &&+10 (else_0))");
        Term b = $("((else_0) &&-10 ((--,(b0)) &&+0 (pre_1)))");
        Term c = $.conj(10, $("((--,(b0)) &&+0 (pre_1))"), $("(else_0)"));
        Term d = $.conj(-10, $("(else_0)"), $("((--,(b0)) &&+0 (pre_1))"));

        System.out.println(a);
        System.out.println(b);
        System.out.println(c);
        System.out.println(d);

        assertEquals(a, b);
        assertEquals(b, c);
        assertEquals(c, d);
        assertEquals(a, c);
        assertEquals(a, d);
    }

    @Test public void testConceptualization() {
        Default d = new Default();

        d.input("(x ==>+0 y)."); //eternal
        d.input("(x ==>+1 y)."); //eternal

        //d.index().print(System.out);
        //d.concept("(x==>y)").print();

        d.step();

        int indexSize = d.index().size();

        d.index().print(System.out);

        assertEquals(2, d.concept("(x==>y)").beliefs().size() );

        d.input("(x ==>+1 y). :|:"); //present
        d.step();

        //d.concept("(x==>y)").print();

        assertEquals(4, d.concept("(x==>y)").beliefs().size() );

        d.index().print(System.out);
        assertEquals(indexSize, d.index().size() ); //remains same amount

        d.index().print(out);
        d.concept("(x==>y)").print();
    }

    @Test public void testSubtermTimeRecursive() {
        Compound c = $("(hold:t2 &&+1 (at:t1 &&+3 ([opened]:t1 &&+5 open(t1))))");
        assertEquals(0, c.subtermTime($("hold:t2")));
        assertEquals(1, c.subtermTime($("at:t1")));
        assertEquals(4, c.subtermTime($("[opened]:t1")));
        assertEquals(9, c.subtermTime($("open(t1)")));
    }


    @Test public void testSubtermTimeRecursiveWithNegativeCommutive() {
        Compound b = $("(a &&+5 b)");
        assertEquals(0, b.subtermTime(A));
        assertEquals(5, b.subtermTime(B));

        Compound c = $("(a &&-5 b)");
        assertEquals(5, c.subtermTime(A));
        assertEquals(0, c.subtermTime(B));

        Compound d = $("(b &&-5 a)");
        assertEquals(0, d.subtermTime(A));
        assertEquals(5, d.subtermTime(B));

        Compound e = $("(a <=>+1 b)");
        assertEquals(0, e.subtermTime(A));
        assertEquals(1, e.subtermTime(B));

        Compound f = $("(a <=>-1 b)");
        assertEquals(1, f.subtermTime(A));
        assertEquals(0, f.subtermTime(B));

        Compound g = $("(b <=>+1 a)");
        assertEquals(1, g.subtermTime(A));
        assertEquals(0, g.subtermTime(B));

    }

    @Test public void testSubtermTestOffset() {
        String x = "(({t001}-->[opened]) &&-5 (open({t001}) &&-5 ((({t001})-->at) &&-5 (({t002})-->hold))))";
        String y =                           "(open({t001}) &&-5 ((({t001})-->at) &&-5 (({t002})-->hold)))";
        assertEquals(0, $(x).subtermTime($(y)));

    }
    @Test public void testSubtermNonCommutivePosNeg() {
        Term ct = $("((d-->c) ==>-3 (c-->b))");
        assertEquals(-3, ct.subtermTime($("(c-->b)")));
        assertEquals(0, ct.subtermTime($("(d-->c)")));
    }

    @Test public void testNonCommutivityImplConcept() {
        NAR n = new Default();
        n.input("(x ==>+5 y).");
        n.input("(y ==>-5 x).");
        n.step();

        StringBuilder cc = new StringBuilder();
        TreeSet d = new TreeSet();
        n.forEachConcept(d::add);

        //2 unique impl concepts created
        assertEquals("[x, y, (x==>y), (y==>x)]", d.toString());
    }

    @Test public void testCommutivity() {

        assertTrue( $("(b && a)").isCommutative() );
        assertTrue( $("(b &&+1 a)").isCommutative() );


        Term abc = $("((a &&+0 b) &&+0 c)");
        assertEquals( "( &&+0 ,a,b,c)", abc.toString() );
        assertTrue( abc.isCommutative() );

    }

//    @Test public void testRelationTaskNormalization() {
//        String a = "pick({t002})";
//        String b = "reachable:(SELF,{t002})";
//
//        String x = "(" + a + " &&+5 " + b + ")";
//        String y = "(" + b + " &&+5 " + a + ")";
//
//        NAR n = new Default();
//        Task xt = n.inputTask(x + ". :|:");
//        Task yt = n.inputTask(y + ". :|:");
//        out.println(xt);
//        out.println(yt);
//        assertEquals(5, xt.term().dt());
//        assertEquals(0, xt.occurrence());
//
//        //should have been shifted to place the earliest component at
//        // the occurrence time expected by the semantics of the input
//        assertEquals(-5, yt.term().dt());
//        assertEquals(5, yt.occurrence());
//
//
//    }
}

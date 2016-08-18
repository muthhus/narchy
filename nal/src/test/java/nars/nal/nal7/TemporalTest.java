package nars.nal.nal7;

import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.bag.Bag;
import nars.bag.impl.ArrayBag;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.io.NarseseTest;
import nars.nal.Tense;
import nars.nar.Default;
import nars.nar.Terminal;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.util.TreeSet;

import static java.lang.System.out;
import static junit.framework.TestCase.assertNotNull;
import static nars.$.$;
import static nars.term.Terms.equalsAnonymous;
import static org.junit.Assert.*;


public class TemporalTest {

    @NotNull NAR n = new Terminal(128); //for cycle/frame clock, not realtime like Terminal


    @Test public void parsedCorrectOccurrenceTime() {
        Task t = n.inputTask("<a --> b>. :\\:");
        assertEquals(0, t.creation());
        assertEquals(-(1 /*n.duration()*/), t.occurrence());
    }

    @Test public void testCoNegatedSubtermConcept() {
        assertEquals("((--,(x))&&(x))", n.concept(
                n.term("((x) &&+10 (--,(x)))"), true).toString());
    }

    @Test public void testCoNegatedSubtermTask() {

        //allowed
        assertNotNull(n.task("((x) &&+1 (--,(x)))."));

        //not allowed
        assertInvalidTask("((x) && (--,(x))).");
        assertInvalidTask("((x) &&+0 (--,(x))).");
    }

    public void assertInvalidTask(@NotNull String ss) {
        try {
            n.input(ss);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test public void testAtemporalization() {
        assertEquals("((x)==>(y))", n.concept(
                n.term("((x) ==>+10 (y))"), true).toString());
    }

    @Test public void testAtemporalizationSharesNonTemporalSubterms() {

        Task a = n.inputTask("((x) ==>+10 (y)).");
        Task c = n.inputTask("((x) ==>+9 (y)).");
        Task b = n.inputTask("((x) <-> (y)).");

        assertTrue( n.concept(a.term()) == n.concept(c.term()));

        assertTrue( ((CompoundConcept)n.concept(a.term())).term(0) == ((CompoundConcept)n.concept(c.term())).term(0));
        assertTrue( ((CompoundConcept)n.concept(b.term())).term(0) == ((CompoundConcept)n.concept(c.term())).term(0));

    }

    @Test public void testHasTemporal() {
        assertTrue( $("(?x &&+1 y)").hasTemporal() );
    }

    @Test public void testParseOperationInFunctionalForm2() {
        assertEquals("(do(that) &&+0 ((a)&&(b)))", n.term("(do(that) &&+0 ((a)&&(b)))").toString());

        Termed<Term> nt = n.term("(((that)-->^do) &&+0 ((a)&&(b)))");
        assertEquals("(do(that) &&+0 ((a)&&(b)))", nt.toString());

        //assertNotNull(n.conceptualize(nt, UnitBudget.One));
        assertEquals("(do(that)&&((a)&&(b)))", n.concept(nt, true).toString());

        //assertEquals("(&&,do(that),(a),(b))", n.conceptualize(nt, UnitBudget.One).toString()); ????????

    }

    @Test public void testAnonymization2() {
        Termed<Term> nn = n.term("(do(that) &&+1 ((a) ==>+2 (b)))");
        assertEquals("(do(that) &&+1 ((a) ==>+2 (b)))", nn.toString());


        assertEquals("(do(that)&&((a)==>(b)))", n.concept(nn, true).toString());

        //assertEquals("(&&,do(that),(a),(b))", n.conceptualize(nt, UnitBudget.One).toString()); ??

    }

    @Test public void testCommutiveTemporalityConjEquiv() {
        testParse("((#1-->$2) <=>-20 ({(row,3)}-->$2))", "((#1-->$2) <=>-20 ({(row,3)}-->$2))");
        testParse("(({(row,3)}-->$2) <=>+20 (#1-->$2))", "((#1-->$2) <=>-20 ({(row,3)}-->$2))");

        testParse("((#1-->$2) &&-20 ({(row,3)}-->$2))", "((#1-->$2) &&-20 ({(row,3)}-->$2))");
        testParse("(({(row,3)}-->$2) &&+20 (#1-->$2))", "((#1-->$2) &&-20 ({(row,3)}-->$2))");
    }
    @Test public void testCommutiveTemporalityConj2() {
        testParse("(goto(a) &&+5 ((SELF,b)-->at))", "(goto(a) &&+5 ((SELF,b)-->at))");
    }


    @Test public void testCommutiveTemporality1() {
        testParse("(goto(a) &&-5 ((SELF,b)-->at))", "(goto(a) &&-5 ((SELF,b)-->at))");
        testParse("(goto(a) &&+0 ((SELF,b)-->at))", "(goto(a) &&+0 ((SELF,b)-->at))");
        testParse("(goto(a)&&((SELF,b)-->at))", "(goto(a)&&((SELF,b)-->at))");
    }
    @Test public void testCommutiveTemporality2() {
        testParse("(goto(a) &&-5 ((SELF,b)-->at))");
        testParse("(goto(a) &&+5 ((SELF,b)-->at))");
        testParse("(goto(a) &&+0 ((SELF,b)-->at))");
        testParse("(goto(a)&&((SELF,b)-->at))");
    }

    @Test public void testCommutiveTemporalityDepVar0() {
        Term t0 = n.term("((SELF,#1)-->at)").term();
        Term t1 = n.term("goto(#1)").term();
        assertEquals(
                TermContainer.the(Op.CONJ, t0, t1),
                TermContainer.the(Op.CONJ, t1, t0)
        );
    }

    @Test public void testCommutiveTemporalityDepVar1() {
        testParse("(goto(#1) &&+5 ((SELF,#1)-->at))");
    }
    @Test public void testCommutiveTemporalityDepVar2() {
        testParse("(goto(#1) &&+5 ((SELF,#1)-->at))", "(goto(#1) &&+5 ((SELF,#1)-->at))");
        testParse("(goto(#1) &&-5 ((SELF,#1)-->at))", "(goto(#1) &&-5 ((SELF,#1)-->at))");
    }

    void testParse(String s) {
        testParse(s, null);
    }

    void testParse(String input, String expected) {
        Termed<Term> t = n.term(input);
        if (expected == null)
            expected = input;
        assertEquals(expected, t.toString());
    }

    @Test public void testCommutiveTemporalityConcepts() {
        Default n = new Default();

        n.log();

        n.input("(goto(#1) &&+5 ((SELF,#1)-->at)).");
        //n.step();

        n.input("(goto(#1) &&-5 ((SELF,#1)-->at)).");
        //n.step();

        n.input("(goto(#1) &&+0 ((SELF,#1)-->at)).");
        //n.step();
        n.input("(((SELF,#1)-->at) &&-3 goto(#1)).");
        //n.step();
        //n.input("(((SELF,#1)-->at) &&+3 goto(#1)).");
        //n.step();
        //n.input("(((SELF,#1)-->at) &&+0 goto(#1)).");


        n.next();

        Concept a = n.concept("(((SELF,#1)-->at) && goto(#1)).");
        Concept a0 = n.concept("(goto(#1) && ((SELF,#1)-->at)).");
        assertTrue(a == a0);


        a.beliefs().print();

        assertEquals(7, a.beliefs().size());
    }

    @Nullable
    static final Term A = $("a");
    @Nullable
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
        Term c = $.conj($("((--,(b0)) &&+0 (pre_1))"), 10, $("(else_0)"));
        Term d = $.conj($("(else_0)"), -10, $("((--,(b0)) &&+0 (pre_1))"));

//        System.out.println(a);
//        System.out.println(b);
//        System.out.println(c);
//        System.out.println(d);

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

        d.next();

        int indexSize = d.index.size();

        d.index.print(System.out);


        assertEquals(3 , d.concept("(x==>y)").beliefs().size() );

        d.input("(x ==>+1 y). :|:"); //present
        d.next();

        //d.concept("(x==>y)").print();

        assertEquals(4, d.concept("(x==>y)").beliefs().size() );

        d.index.print(System.out);
        assertEquals(indexSize, d.index.size() ); //remains same amount

        d.index.print(out);
        d.concept("(x==>y)").print();
    }

    @Test public void testConceptualization2() {
        //test that an image is not considered temporal:
        Default d = new Default();
        d.believe("((\\,((#1-->[happy])&&(#1-->[sad])),((0-->v),(0-->h)),_)-->[pill])");
        d.run(1);
        d.core.concepts.print();
        assertEquals(10, d.core.concepts.size());
    }

    @Test public void testConceptualizationIntermpolationEternal() {

        Default d = new Default();
        d.believe("((\\,(a ==>+2 b),_)-->[pill])");
        d.believe("((\\,(a ==>+6 b),_)-->[pill])"); //same concept
        //d.run(1);

        Bag<Concept> cb = d.core.concepts;
        cb.print();
        assertEquals(7, cb.size());
        Concept cc = ((ArrayBag<Concept>) cb).get(0).get();
        assertEquals("((\\,(a==>b),_)-->[pill])", cc.toString());
        cc.print();
        //INTERMPOLATION APPLIED DURING REVISION:
        assertEquals("((\\,(a ==>+4 b),_)-->[pill])", cc.beliefs().eternalTop().term().toString());
    }

    @Test public void testConceptualizationIntermpolationTemporal() {

        Default d = new Default();
        d.believe("((\\,(a ==>+2 b),_)-->[pill])", Tense.Present, 1f, 0.9f);
        d.run(4);
        d.believe("((\\,(a ==>+6 b),_)-->[pill])", Tense.Present, 1f, 0.9f);
        d.run(1);

        Bag<Concept> cb = d.core.concepts;
        cb.print();
        assertTrue(7 <= cb.size());
        Concept cc = ((ArrayBag<Concept>) cb).get(0).get();
        assertEquals("((\\,(a==>b),_)-->[pill])", cc.toString());
        cc.print();
        //INTERMPOLATION APPLIED DURING REVISION:
        assertEquals("((\\,(a ==>+4 b),_)-->[pill])", cc.beliefs().topTemporal(2,d.time(), null).term().toString());
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
        Term ct = $("((d-->c) ==>-3 (a-->b))");
        assertEquals(-3, ct.subtermTime($("(a-->b)")));
        assertEquals(0, ct.subtermTime($("(d-->c)")));
    }

    @Test public void testNonCommutivityImplConcept() {
        NAR n = new Default();

        n.input("(x ==>+5 y).");
        n.input("(y ==>-5 x).");
        n.next().next();

        StringBuilder cc = new StringBuilder();
        TreeSet d = new TreeSet((x,y)-> x.toString().compareTo(y.toString()));
        n.forEachActiveConcept(d::add);

        //2 unique impl concepts created
        assertEquals("[(x<=>y), (x==>y), (y==>x), x, y]", d.toString());
    }

    @Test public void testCommutivity() {

        assertTrue( $("(b && a)").isCommutative() );
        assertTrue( $("(b &&+1 a)").isCommutative() );


        Term abc = $("((a &&+0 b) &&+0 c)");
        assertEquals( "( &&+0 ,a,b,c)", abc.toString() );
        assertTrue( abc.isCommutative() );

    }

    @Test public void testInvalidConjunction() {
        NarseseTest.assertInvalid( "( &&-59 ,(#1-->I),(#1-->{i141}),(#2-->{i141}))");

        Compound x = $("(&&,(#1-->I),(#1-->{i141}),(#2-->{i141}))");
        Assert.assertNotNull(x);

//        Assert.assertNotNull(x.dt(0));
//        Assert.assertNotNull(x.dt(0).dt(DTERNAL));
//        assertEquals(x, x.dt(0).dt(DTERNAL));
//
//        try {
//            x.dt(-59);
//            assertTrue(x.toString(), false);
//        } catch (InvalidTerm e) {
//            assertTrue(true);
//        }
    }
    @Test public void testEqualsAnonymous() {
        assertTrue(equalsAnonymous(
                $("(x && y)"), $("(x &&+1 y)")
        ));
        assertTrue(equalsAnonymous(
                $("(x && y)"), $("(y &&+1 x)")
        ));
        assertFalse(equalsAnonymous(
                $("(x && y)"), $("(z &&+1 x)")
        ));

        assertTrue(equalsAnonymous(
                $("(x ==> y)"), $("(x ==>+1 y)")
        ));
        assertFalse(equalsAnonymous(
                $("(x ==> y)"), $("(y ==>+1 x)")
        ));
    }
    @Test public void testEqualsAnonymous3() {
        assertTrue(equalsAnonymous(
                $("(x && (y ==> z))"), $("(x &&+1 (y ==> z))")
        ));
        assertTrue(equalsAnonymous(
                $("(x && (y ==> z))"), $("(x &&+1 (y ==>+1 z))")
        ));
        assertFalse(equalsAnonymous(
                $("(x && (y ==> z))"), $("(x &&+1 (z ==>+1 w))")
        ));
    }
    @Test public void testEqualsAnonymous4() {
        //temporal terms within non-temporal terms
        assertTrue(equalsAnonymous(
                $("(a <-> (y ==> z))"), $("(a <-> (y ==>+1 z))")
        ));
        assertFalse(equalsAnonymous(
                $("(a <-> (y ==> z))"), $("(a <-> (w ==>+1 z))")
        ));

        assertTrue(equalsAnonymous(
                $("((a ==> b),(b ==> c))"), $("((a ==> b),(b ==>+1 c))")
        ));
        assertTrue(equalsAnonymous(
                $("((a ==>+1 b),(b ==> c))"), $("((a ==> b),(b ==>+1 c))")
        ));
    }
    @Test public void testEqualsAnonymous5() {
        //special handling for images
        assertTrue(equalsAnonymous(
                $("(/, (a ==> b), c, _)"), $("(/, (a ==>+1 b), c, _)")
        ));
        assertFalse(equalsAnonymous(
                $("(/, a, b, _)"), $("(/, a, _, b)")
        ));
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

//    @Test
//    public void testAfter() {
//
//        assertTrue("after", Tense.after(1, 4, 1));
//
//        assertFalse("concurrent (equivalent)", Tense.after(4, 4, 1));
//        assertFalse("before", Tense.after(6, 4, 1));
//        assertFalse("concurrent (by duration range)", Tense.after(3, 4, 3));
//
//    }
}

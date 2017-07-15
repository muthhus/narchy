package nars.term;

import nars.*;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.io.NarseseTest;
import nars.nar.NARS;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.container.TermContainer;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.util.TreeSet;

import static junit.framework.TestCase.assertNotNull;
import static nars.$.$;
import static nars.term.container.TermContainer.theTermArray;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;
import static org.junit.Assert.*;


public class TemporalTest {

    @NotNull
    static final NAR n = new NARS().get();


    @Test
    public void parsedCorrectOccurrenceTime() throws Narsese.NarseseException {
        long now = n.time();
        Task t = n.inputAndGet("b:a. :\\:");
        assertEquals(now, t.creation());
        assertEquals(now - 1, t.start());
    }

    @Test
    public void testCoNegatedSubtermConcept() throws Narsese.NarseseException {
        assertEquals("((x) &&+- (x))", n.conceptualize(n.term("((x) &&+10 (x))")).toString());

        assertEquals("((--,(x)) &&+- (x))", n.conceptualize(n.term("((x) &&+10 (--,(x)))")).toString());
        assertEquals("((--,(x)) &&+- (x))", n.conceptualize(n.term("((x) &&-10 (--,(x)))")).toString());

        assertEquals("((x) <=>+- (x))", n.conceptualize(n.term("((x) <=>+10 (--,(x)))")).toString());
        assertEquals("((x) <=>+- (x))", n.conceptualize(n.term("((x) <=>-10 (--,(x)))")).toString());

        assertEquals("((x) ==>+- (x))", n.conceptualize(n.term("((x) ==>+10 (x))")).toString());
        assertEquals("((--,(x)) ==>+- (x))", n.conceptualize(n.term("((--,(x)) ==>+10 (x))")).toString());
        assertEquals("((x) ==>+- (x))", n.conceptualize(n.term("((x) ==>+10 (--,(x)))")).toString());
        assertEquals("((x) ==>+- (x))", n.conceptualize(n.term("((x) ==>-10 (--,(x)))")).toString());

    }

    @Test
    public void testCoNegatedSubtermTask() throws Narsese.NarseseException {

        //allowed
        assertNotNull(n.task("((x) &&+1 (--,(x)))."));

        //not allowed
        assertInvalidTask("((x) && (--,(x))).");
        assertInvalidTask("((x) &&+0 (--,(x))).");
    }


    public void assertInvalidTask(@NotNull String ss) {
        try {
            Narsese.the().task(ss, n);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void testAtemporalization() throws Narsese.NarseseException {
        assertEquals("((x)==>(y))", n.conceptualize(n.term("((x) ==>+10 (y))")).toString());
    }

    @Test public void testAtemporalization2() throws Narsese.NarseseException {
        assertEquals("(y &&+- (--,y))", $.terms.atemporalize($("(y &&+3 (--,y))")).toString());
    }
    @Test public void testAtemporalization3() throws Narsese.NarseseException {
        assertEquals("",
                $.terms.atemporalize($("(--,(($1&&x) ==>+1 ((--,y) &&+2 $1)))")).toString());
    }

    @Test public void testAtemporalization4() throws Narsese.NarseseException {
        //maintain temporal information that would otherwise be factored out if non-temporal
        assertEquals("(($1 && x) ==>+- ($1 && y))",
                $.terms.atemporalize($("(($1 && x) ==>+1 ($1 &&+1 y))")).toString());
    }

    @Test public void testAtemporalization5() throws Narsese.NarseseException {
        assertEquals("((x==>y) &&+- y)",
                $.terms.atemporalize($("((x==>y) &&+1 y)")).toString());
    }

    @Test public void testAtemporalization6() throws Narsese.NarseseException {
        Compound x = $("((--,(($1&&x) ==>+1 ((--,y) &&+2 $1))) &&+3 (--,y))");
        Term y = $.terms.atemporalize(x);
        assertEquals("((--,(($1&&x) ==> ((--,y) && $1))) && (--,y))",y);
    }

    @Test
    public void testAtemporalizationSharesNonTemporalSubterms() throws Narsese.NarseseException {

        Task a = n.inputAndGet("((x) ==>+10 (y)).");
        Task c = n.inputAndGet("((x) ==>+9 (y)).");
        Task b = n.inputAndGet("((x) <-> (y)).");
        n.cycle();

        @NotNull Compound aa = a.term();
        assertNotNull(aa);

        @Nullable Concept na = a.concept(n);
        assertNotNull(na);

        @Nullable Concept nc = c.concept(n);
        assertNotNull(nc);

        assertTrue(na == nc);

        assertTrue(((CompoundConcept) na).term(0) == ((CompoundConcept) nc).term(0));

//        System.out.println(b.concept(n));
//        System.out.println(c.concept(n));

        assertTrue(b.concept(n).term(0).equals(c.concept(n).term(0)));

    }

    @Test
    public void testHasTemporal() throws Narsese.NarseseException {
        assertTrue($("(?x &&+1 y)").isTemporal());
    }

    @Test
    public void testParseOperationInFunctionalForm2() throws Narsese.NarseseException {
        assertEquals(
                //"(do(that) &&+0 ((a)&&(b)))",
                "( &&+0 ,do(that),(a),(b))",
                n.term("(do(that) &&+0 ((a)&&(b)))").toString());

        Termed<Term> nt = n.term("(((that)-->do) &&+0 ((a)&&(b)))");
        assertEquals(
                //"(do(that) &&+0 ((a)&&(b)))",
                "( &&+0 ,do(that),(a),(b))",
                nt.toString());

        //assertNotNull(n.conceptualize(nt, UnitBudget.One));
        assertEquals("(&&,do(that),(a),(b))", n.conceptualize(nt).toString());

        //assertEquals("(&&,do(that),(a),(b))", n.conceptualize(nt, UnitBudget.One).toString()); ????????

    }

    @Test
    public void testAnonymization2() throws Narsese.NarseseException {
        Termed<Term> nn = n.term("((do(that) &&+1 (a)) ==>+2 (b))");
        assertEquals("((do(that) &&+1 (a)) ==>+2 (b))", nn.toString());


        assertEquals("((do(that)&&(a))==>(b))", n.conceptualize(nn).toString());

        //assertEquals("(&&,do(that),(a),(b))", n.conceptualize(nt, UnitBudget.One).toString()); ??

    }

    @Test
    public void testCommutiveTemporalityConjEquiv() {
        testParse("((#1-->$2) <=>-20 ({(row,3)}-->$2))", "(({(row,3)}-->$2) <=>+20 (#1-->$2))");
        testParse("(({(row,3)}-->$2) <=>+20 (#1-->$2))", "(({(row,3)}-->$2) <=>+20 (#1-->$2))");

        testParse("((#1-->$2) &&-20 ({(row,3)}-->$2))", "(({(row,3)}-->$2) &&+20 (#1-->$2))");
    }

    @Test
    public void testCommutiveTemporalityConjEquiv2() {
        testParse("(({(row,3)}-->$2) &&+20 (#1-->$2))", "(({(row,3)}-->$2) &&+20 (#1-->$2))");
    }

    @Test
    public void testCommutiveTemporalityConj2() {
        testParse("(goto(a) &&+5 ((SELF,b)-->at))", "(goto(a) &&+5 at(SELF,b))");
    }


    @Test
    public void testCommutiveTemporality1() {
        testParse("(at(SELF,b) &&+5 goto(a))", "(at(SELF,b) &&+5 goto(a))");
        testParse("(goto(a) &&+0 ((SELF,b)-->at))", "(goto(a) &&+0 at(SELF,b))");
        testParse("(goto(a)&&((SELF,b)-->at))", "(goto(a)&&at(SELF,b))");
    }

    @Test
    public void testCommutiveTemporality2() {
        testParse("(at(SELF,b) &&+5 goto(a))");
        testParse("(goto(a) &&+5 at(SELF,b))");
        testParse("(goto(a) &&+0 at(SELF,b))");
        testParse("(goto(a)&&at(SELF,b))");
    }

    @NotNull
    static TermContainer the(@NotNull Op op, int dt, @NotNull Term... tt) {
        return Op.subterms(theTermArray(op, dt, tt));
    }

    @Test
    public void testCommutiveTemporalityDepVar0() throws Narsese.NarseseException {
        Term t0 = n.term("((SELF,#1)-->at)").term();
        Term t1 = n.term("goto(#1)").term();
        assertEquals(
                the(Op.CONJ, DTERNAL, t0, t1),
                the(Op.CONJ, DTERNAL, t1, t0)
        );
    }

    @Test
    public void testCommutiveTemporalityDepVar1() {
        testParse("(goto(#1) &&+5 at(SELF,#1))");
    }

    @Test
    public void testCommutiveTemporalityDepVar2() {
        testParse("(goto(#1) &&+5 at(SELF,#1))", "(goto(#1) &&+5 at(SELF,#1))");
        testParse("(goto(#1) &&-5 at(SELF,#1))", "(at(SELF,#1) &&+5 goto(#1))");
    }

    @Test
    public void testCommutiveEquivAgain1() throws Narsese.NarseseException {
        assertEquals($("((--,(0,0)) <=>+48 (happy))"), $("((happy) <=>-48 (--,(0,0)))"));
    }

    @Test
    public void testCommutiveEquivAgain2() throws Narsese.NarseseException {
        assertEquals($("((--,(0,0)) <=>+48 (happy))"), $("((--,(happy)) <=>-48 (0,0))"));
    }

    @Test
    public void testCommutiveEquivAgain3() throws Narsese.NarseseException {
        assertEquals($("((--,(0,0)) <=>+48 (--,(happy)))"), $("((--,(happy)) <=>-48 (--,(0,0)))"));
    }

    void testParse(String s) {
        testParse(s, null);
    }

    void testParse(String input, String expected) {
        Termed<Term> t = null;
        try {
            t = n.term(input);
        } catch (Narsese.NarseseException e) {
            assertTrue(false);
        }
        if (expected == null)
            expected = input;
        assertEquals(expected, t.toString());
    }

    @Test
    public void testCommutiveTemporalityConcepts() throws Narsese.NarseseException {
        NAR n = new NARS().get();

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


        n.run(2);

        Concept a = n.concept("(((SELF,#1)-->at) && goto(#1)).");
        Concept a0 = n.concept("(goto(#1) && ((SELF,#1)-->at)).");
        assertNotNull(a);
        assertTrue(a == a0);


        a.beliefs().print();

        assertTrue(a.beliefs().size() >= 4);
    }

    @Test
    public void testCommutiveTemporalityConcepts2() throws Narsese.NarseseException {
        NAR n = new NARS().get();

        for (String op : new String[]{"&&", "<=>"}) {
            Concept a = n.conceptualize($("(x " + op + "   y)"));
            Concept b = n.conceptualize($("(x " + op + "+1 y)"));

            assertTrue(a == b);

            Concept c = n.conceptualize($("(x " + op + "+2 y)"));

            assertTrue(b == c);

            Concept d = n.conceptualize($("(x " + op + "-1 y)"));

            assertTrue(c == d);

            Term e0 = $("(x " + op + "+- y)");
            assertEquals("(x " + op + "+- y)", e0.toString());
            Concept e = n.conceptualize(e0);

            assertTrue(d == e);

            Term f0 = $("(y " + op + "+- x)");
            assertEquals("(y " + op + "+- x)", f0.toString());
            Concept f = n.conceptualize(f0);


            assertTrue(e + "==" + f, e == f);

            //repeat
            Concept g = n.conceptualize($("(x " + op + "+- x)"));
            assertEquals("(x " + op + "+- x)", g.toString());

            //co-negation
            Concept h = n.conceptualize($("(x " + op + "+- (--,x))"));
            assertEquals("(x " + op + "+- (--,x))", h.toString());

        }

    }

    @Nullable
    static final Term A = $.the("a");
    @Nullable
    static final Term B = $.the("b");

    @Test
    public void parseTemporalRelation() throws Narsese.NarseseException {
        //TODO move to NarseseTest
        assertEquals("(x ==>+5 y)", $("(x ==>+5 y)").toString());
        assertEquals("(x &&+5 y)", $("(x &&+5 y)").toString());

        assertEquals("(x ==>-5 y)", $("(x ==>-5 y)").toString());

        assertEquals("((before-->x) ==>+5 (after-->x))", $("(x:before ==>+5 x:after)").toString());
    }

    @Test
    public void temporalEqualityAndCompare() throws Narsese.NarseseException {
        assertNotEquals($("(x ==>+5 y)"), $("(x ==>+0 y)"));
        assertNotEquals($("(x ==>+5 y)").hashCode(), $("(x ==>+0 y)").hashCode());
        assertNotEquals($("(x ==> y)"), $("(x ==>+0 y)"));
        assertNotEquals($("(x ==> y)").hashCode(), $("(x ==>+0 y)").hashCode());

        assertEquals($("(x ==>+0 y)"), $("(x ==>-0 y)"));
        assertNotEquals($("(x ==>+5 y)"), $("(y ==>-5 x)"));


        assertEquals(0, $("(x ==>+0 y)").compareTo($("(x ==>+0 y)")));
        assertEquals(-1, $("(x ==>+0 y)").compareTo($("(x ==>+1 y)")));
        assertEquals(+1, $("(x ==>+1 y)").compareTo($("(x ==>+0 y)")));
    }


    @Test
    public void testReversibilityOfCommutive() throws Narsese.NarseseException {
        for (String c : new String[]{"&&", "<=>"}) {
            assertEquals("(a " + c + "+5 b)", $("(a " + c + "+5 b)").toString());
            assertEquals("(b " + c + "+5 a)", $("(b " + c + "+5 a)").toString());
            assertEquals("(a " + c + "+5 b)", $("(b " + c + "-5 a)").toString());
            assertEquals("(b " + c + "+5 a)", $("(a " + c + "-5 b)").toString());

            assertEquals($("(b " + c + "-5 a)"), $("(a " + c + "+5 b)"));
            assertEquals($("(b " + c + "+5 a)"), $("(a " + c + "-5 b)"));
            assertEquals($("(a " + c + "-5 b)"), $("(b " + c + "+5 a)"));
            assertEquals($("(a " + c + "+5 b)"), $("(b " + c + "-5 a)"));
        }
    }

    @Test
    public void testCommutiveWithCompoundSubterm() throws Narsese.NarseseException {
        Term a = $("(((--,(b0)) &&+0 (pre_1)) &&+10 (else_0))");
        Term b = $("((else_0) &&-10 ((--,(b0)) &&+0 (pre_1)))");
        assertEquals(a, b);

        Term c = $.seq($("((--,(b0)) &&+0 (pre_1))"), 10, $("(else_0)"));
        Term d = $.seq($("(else_0)"), -10, $("((--,(b0)) &&+0 (pre_1))"));

//        System.out.println(a);
//        System.out.println(b);
//        System.out.println(c);
//        System.out.println(d);

        assertEquals(b, c);
        assertEquals(c, d);
        assertEquals(a, c);
        assertEquals(a, d);
    }

    @Test
    public void testConceptualization() throws Narsese.NarseseException {
        //NAR n = new NARBuilder().get();

        n.input("(x ==>+0 y)."); //eternal
        n.input("(x ==>+1 y)."); //eternal

        //d.index().print(System.out);
        //d.concept("(x==>y)").print();

        n.run(2);



        Concept xImplY = n.conceptualize($("(x==>y)"));
        assertEquals(3, xImplY.beliefs().size());

        int indexSize = n.terms.size();
        n.terms.print(System.out);

        n.input("(x ==>+1 y). :|:"); //present
        n.cycle();

        //d.concept("(x==>y)").print();

        assertEquals(4, xImplY.beliefs().size());

        n.terms.print(System.out);
        assertEquals(indexSize, n.terms.size()); //remains same amount

        n.concept("(x==>y)").print();
    }


    @Test
    public void testConceptualizationIntermpolationEternal() throws Narsese.NarseseException {

        NAR d = new NARS().get();
        d.believe("((a ==>+2 b)-->[pill])");
        d.believe("((a ==>+6 b)-->[pill])"); //same concept
        d.run(1);


        //assertTrue(5 <= size(d.focus().concepts()));
        //Concept cc = ((ArrayBag<Concept>) cb).get(0).get();


        Term term = $("((a==>b)-->[pill])");

        Concept cc = d.concept(term);
        assertNotNull(cc);
        String q = "((a==>b)-->[pill])";
        assertTrue(cc.toString().equals(q));
        //assertEquals(q, cc.toString());


        //INTERMPOLATION APPLIED DURING REVISION:
        assertEquals("((a ==>+4 b)-->[pill])", cc.beliefs().match(ETERNAL, null, null, true, null).term().toString());
    }

    @Test
    public void testConceptualizationIntermpolationTemporal() throws Narsese.NarseseException {

        n.believe("((a ==>+2 b)-->[pill])", Tense.Present, 1f, 0.9f);
        n.believe("((a ==>+6 b)-->[pill])", Tense.Present, 1f, 0.9f);
        n.run(1);

        //@NotNull Bag<Concept, PLink<Concept>> cb = n.focus.active;
        //assertTrue(5 <= cb.size());

        String abpill = "((a==>b)-->[pill])";
        Concept cc = n.concept(abpill); //iterator().next().get();//((ArrayBag<Concept>) cb).get(0).get();

        assertNotNull(cc);

        String correctMerge = "((a ==>+4 b)-->[pill])";


        cc.beliefs().print();

        //test belief match interpolated a result
        assertEquals(correctMerge, cc.beliefs().match((long) 0, null, null, true, n).term().toString());


        //test merge after capacity shrink:

        cc.beliefs().setCapacity(1, 1); //set to capacity=1 to force compression

        cc.print();

        //n.forEachTask(System.out::println);

        //INTERMPOLATION APPLIED AFTER REVECTION:
        assertEquals(correctMerge, cc.beliefs().match((long) 0, null, null, true, n).term().toString());
    }

    @Test
    public void testSubtermTimeRecursive() throws Narsese.NarseseException {
        Compound c = $("(hold:t2 &&+1 (at:t1 &&+3 ([opened]:t1 &&+5 open(t1))))");
        assertEquals(0, c.subtermTime($("hold:t2")));
        assertEquals(1, c.subtermTime($("at:t1")));
        assertEquals(4, c.subtermTime($("[opened]:t1")));
        assertEquals(9, c.subtermTime($("open(t1)")));
    }


    @Test
    public void testSubtermTimeRecursiveWithNegativeCommutive() throws Narsese.NarseseException {
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

    @Test
    public void testSubtermConjInConj() throws Narsese.NarseseException {
        Compound g = $("(((x) &&+1 (y)) &&+1 (z))");
        assertEquals(0, g.subtermTime($("(x)")));
        assertEquals(1, g.subtermTime($("(y)")));
        assertEquals(2, g.subtermTime($("(z)")));

        Compound h = $("((z) &&+1 ((x) &&+1 (y)))");
        assertEquals(0, h.subtermTime($("(z)")));
        assertEquals(1, h.subtermTime($("(x)")));
        assertEquals(2, h.subtermTime($("(y)")));

        Compound i = $("((y) &&+1 ((z) &&+1 (x)))");
        assertEquals(0, i.subtermTime($("(y)")));
        assertEquals(1, i.subtermTime($("(z)")));
        assertEquals(2, i.subtermTime($("(x)")));

        Compound j = $("((x) &&+1 ((z) &&+1 (y)))");
        assertEquals(0, j.subtermTime($("(x)")));
        assertEquals(1, j.subtermTime($("(z)")));
        assertEquals(2, j.subtermTime($("(y)")));
    }

    @Test
    public void testDTRange() throws Narsese.NarseseException {
        assertEquals(1, $("((z) &&+1 (y))").dtRange());
        assertEquals(2, $("((x) &&+1 ((z) &&+1 (y)))").dtRange());
        assertEquals(4, $("((x) &&+1 ((z) &&+1 ((y) &&+2 (w))))").dtRange());
        assertEquals(4, $("(((z) &&+1 ((y) &&+2 (w))) &&+1 (x))").dtRange());
    }

    @Test
    public void testSubtermTestOffset() throws Narsese.NarseseException {
        String x = "(({t001}-->[opened]) &&-5 (open({t001}) &&-5 ((({t001})-->at) &&-5 (({t002})-->hold))))";
        String y = "(open({t001}) &&-5 ((({t001})-->at) &&-5 (({t002})-->hold)))";
        assertEquals(0, $(x).subtermTime($(y)));

    }

    @Test
    public void testSubtermNonCommutivePosNeg() throws Narsese.NarseseException {
        Term ct = $("((d-->c) ==>-3 (a-->b))");
        assertEquals(0, ct.subtermTime($("(a-->b)")));
        assertEquals(3, ct.subtermTime($("(d-->c)")));
    }

    @Test
    public void testNonCommutivityImplConcept() throws Narsese.NarseseException {
        Param.DEBUG = true;
        NAR n = new NARS().get();
        n.input("((x) ==>+5 (y)).", "((y) ==>-5 (x)).");
        n.run(155);

        TreeSet d = new TreeSet((x, y) -> x.toString().compareTo(y.toString()));
        n.forEachConceptActive(x -> d.add(x.get()));

        //2 unique impl concepts created
        assertEquals(
                //"[(#1==>x), (#1==>y), ((--,(y==>#1))&&(--,(#1==>y))), ((x==>#1)&&(#1==>x)), (x<=>y), (x==>#1), (x==>y), (y==>#1), (y==>x), x, y]"
                //"[((x)<=>(y)), ((x)==>(y)), ((y)<=>(x)), ((y)==>(x)), (x), (y), x, y]"
                "[((x)<=>(y)), ((x)==>(y)), ((y)==>(x)), (x), (y), x, y]"
                , d.toString());
    }

    @Test
    public void testCommutivity() throws Narsese.NarseseException {

        assertTrue($("(b && a)").isCommutative());
        assertTrue($("(b &&+0 a)").isCommutative());
        assertFalse($("(b &&+1 a)").isCommutative());
        assertFalse($("(b &&+- a)").isCommutative());


        Term abc = $("((a &&+0 b) &&+0 c)");
        assertEquals("( &&+0 ,a,b,c)", abc.toString());
        assertTrue(abc.isCommutative());

    }

    @Test
    public void testInvalidConjunction() throws Narsese.NarseseException {
        NarseseTest.assertInvalidTerms("( &&-59 ,(#1-->I),(#1-->{i141}),(#2-->{i141}))");

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

    @Test
    public void testEqualsAnonymous() throws Narsese.NarseseException {
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        //assertEquals("", $.terms.atemporalize())
        @NotNull Term a = $("(x && y)");

        Term b = $.$("(x &&+1 y)");
        assertEquals("(x&&y)", $.terms.atemporalize(b).toString());

        Term c = $.$("(x &&+1 x)");
        assertEquals("(x &&+- x)", $.terms.atemporalize(c).toString());

        assertTrue(Terms.equalAtemporally(a, b));
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        assertTrue(Terms.equalAtemporally(a, $.$("(y &&+1 x)")));
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        assertFalse(Terms.equalAtemporally(a, $.$("(z &&+1 x)")));

        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        assertTrue(Terms.equalAtemporally($("(x ==> y)"), $.$("(x ==>+1 y)")));
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        Term f = $("(x ==> y)");
        Term g = $("(y ==>+1 x)");
        assertEquals("(x==>y)", $.terms.atemporalize(f).toString());
        assertEquals("(y==>x)", $.terms.atemporalize(g).toString());
        assertFalse(Terms.equalAtemporally(f, g));
    }

    @Test
    public void testEqualsAnonymous3() throws Narsese.NarseseException {
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        assertTrue(Terms.equalAtemporally($("(x && (y ==> z))"), $.$("(x &&+1 (y ==> z))")));
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        assertTrue(Terms.equalAtemporally($("(x && (y ==> z))"), $.$("(x &&+1 (y ==>+1 z))")));
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        assertFalse(Terms.equalAtemporally($("(x && (y ==> z))"), $.$("(x &&+1 (z ==>+1 w))")));
    }

    @Test
    public void testAtemporalization1() throws Narsese.NarseseException {
        Term x = $("(((--,(tetris-->(_n,#2))) &&+1 $1) <=>+1 ($1 &&+0 (--,(tetris-->(_n,#2)))))");
        Term y = $.terms.atemporalize(x);
        assertEquals("(($1&&(--,(tetris-->(_n,#2)))) <=>+- ($1&&(--,(tetris-->(_n,#2)))))", y.toString());
    }


    @Test
    public void testEqualsAnonymous4() throws Narsese.NarseseException {
        //temporal terms within non-temporal terms
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        assertTrue(Terms.equalAtemporally($("(a <-> (y ==> z))"), $.$("(a <-> (y ==>+1 z))")));
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        assertFalse(Terms.equalAtemporally($("(a <-> (y ==> z))"), $.$("(a <-> (w ==>+1 z))")));

        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        assertTrue(Terms.equalAtemporally($("((a ==> b),(b ==> c))"), $.$("((a ==> b),(b ==>+1 c))")));
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        assertTrue(Terms.equalAtemporally($("((a ==>+1 b),(b ==> c))"), $.$("((a ==> b),(b ==>+1 c))")));
    }

    @Test
    public void testEqualAtemporally5() throws Narsese.NarseseException {
        //special handling for images
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        assertTrue(Terms.equalAtemporally($("(/, (a ==> b), c, _)"), $.$("(/, (a ==>+1 b), c, _)")));
        //        if (as == bs) {
//            return true;
//        } else if (as instanceof Compound && bs instanceof Compound) {
//            return equalsAnonymous((Compound) as, (Compound) bs);
//        } else {
//            return as.equals(bs);
//        }
        assertFalse(Terms.equalAtemporally($("(/, a, b, _)"), $.$("(/, a, _, b)")));
    }

    @Test
    public void testRetermporalization1() throws Narsese.NarseseException {

        String st = "((--,(happy)) && (--,((--,(o))&&(happy))))";
        Compound t = $.$(st);
        assertEquals("((--,(happy))&&(--,((--,(o))&&(happy))))", t.toString());
        Term xe = $.terms.retemporalize(t, $.terms.retemporalizationDTERNAL);
        assertEquals("((--,(happy))&&(--,((--,(o))&&(happy))))", xe.toString());
        Term xz = $.terms.retemporalize(t, $.terms.retemporalizationZero);
        assertEquals("((--,(happy)) &&+0 (--,((--,(o)) &&+0 (happy))))", xz.toString());
    }

    @Test
    public void testRetermporalization2() throws Narsese.NarseseException {
        String su = "((--,(happy)) &&+- (--,((--,(o))&&+-(happy))))";
        Compound u = $.$(su);
        assertEquals("((--,(happy)) &&+- (--,((--,(o)) &&+- (happy))))", u.toString());
        Term ye = $.terms.retemporalize(u, $.terms.retemporalizationDTERNAL);
        assertEquals("((--,(happy))&&(--,((--,(o))&&(happy))))", ye.toString());
        Term yz = $.terms.retemporalize(u, $.terms.retemporalizationZero);
        assertEquals("((--,(happy)) &&+0 (--,((--,(o)) &&+0 (happy))))", yz.toString());


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

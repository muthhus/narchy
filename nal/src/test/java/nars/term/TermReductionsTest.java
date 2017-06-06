package nars.term;

import nars.$;
import nars.Narsese;
import nars.Op;
import nars.Task;
import nars.nar.Terminal;
import nars.task.util.InvalidTaskException;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Ignore;
import org.junit.Test;

import static nars.$.*;
import static nars.$.t;
import static nars.Op.*;
import static nars.index.TermBuilder.imageUnwrap;
import static nars.io.NarseseTest.assertInvalidTasks;
import static nars.io.NarseseTest.assertInvalidTerms;
import static nars.term.TermTest.*;
import static nars.term.TermTest.t;
import static org.junit.Assert.*;

/**
 * Created by me on 12/10/15.
 */
public class TermReductionsTest {

    @Nullable
    final Term p = Atomic.the("P"), q = Atomic.the("Q"), r = Atomic.the("R"), s = Atomic.the("S");


    @Test
    public void testIntersectExtReduction1() throws Narsese.NarseseException {
        // (&,R,(&,P,Q)) = (&,P,Q,R)
        assertEquals("(&,P,Q,R)", secte(r, secte(p, q)).toString());
        assertEquals("(&,P,Q,R)", $("(&,R,(&,P,Q))").toString());
    }

    @Test
    public void testIntersectExtReduction2() throws Narsese.NarseseException {
        // (&,(&,P,Q),(&,R,S)) = (&,P,Q,R,S)
        assertEquals("(&,P,Q,R,S)", secte(secte(p, q), secte(r, s)).toString());
        assertEquals("(&,P,Q,R,S)", $("(&,(&,P,Q),(&,R,S))").toString());
    }

    @Test
    public void testIntersectExtReduction3() throws Narsese.NarseseException {
        // (&,(&,P,Q),(&,R,S)) = (&,P,Q,R,S)
        assertEquals("(&,P,Q,R,S,T,U)", $("(&,(&,P,Q),(&,R,S), (&,T,U))").toString());
    }

    @Test
    public void testIntersectExtReduction2_1() throws Narsese.NarseseException {
        // (&,R,(&,P,Q)) = (&,P,Q,R)
        assertEquals("(&,P,Q,R)", $("(&,R,(&,P,Q))").toString());
    }

    @Test
    public void testIntersectExtReduction4() throws Narsese.NarseseException {
        //UNION if (term1.op(Op.SET_INT) && term2.op(Op.SET_INT)) {
        assertEquals("{P,Q,R,S}", secte(sete(p, q), sete(r, s)).toString());
        assertEquals("{P,Q,R,S}", $("(&,{P,Q},{R,S})").toString());
    }

    @Test
    public void testIntersectExtReduction5() throws Narsese.NarseseException {
        assertEquals(Null /* emptyset */, secte(seti(p, q), seti(r, s)));
    }

    @Test
    public void testIntersectIntReduction1() throws Narsese.NarseseException {
        // (|,R,(|,P,Q)) = (|,P,Q,R)
        assertEquals("(|,P,Q,R)", secti(r, secti(p, q)).toString());
        assertEquals("(|,P,Q,R)", $("(|,R,(|,P,Q))").toString());
    }

    @Test
    public void testIntersectIntReduction2() throws Narsese.NarseseException {
        // (|,(|,P,Q),(|,R,S)) = (|,P,Q,R,S)
        assertEquals("(|,P,Q,R,S)", secti(secti(p, q), secti(r, s)).toString());
        assertEquals("(|,P,Q,R,S)", $("(|,(|,P,Q),(|,R,S))").toString());
    }

    @Test
    public void testIntersectIntReduction3() throws Narsese.NarseseException {
        // (|,R,(|,P,Q)) = (|,P,Q,R)
        assertEquals("(|,P,Q,R)", $("(|,R,(|,P,Q))").toString());
    }

    @Test
    public void testIntersectIntReduction4() throws Narsese.NarseseException {
        //UNION if (term1.op(Op.SET_INT) || term2.op(Op.SET_INT)) {
        assertEquals("[P,Q,R,S]", secti(seti(p, q), seti(r, s)).toString());
        assertEquals("[P,Q,R,S]", $("(|,[P,Q],[R,S])").toString());

    }

    @Test
    public void testIntersectIntReductionToZero() throws Narsese.NarseseException {
        assertInvalidTerms("(|,{P,Q},{R,S})");
    }

    @Test
    public void testIntersectIntReduction_to_one() throws Narsese.NarseseException {
        assertEquals("(robin-->bird)", $("<robin-->(|,bird)>").toString());
        assertEquals("(robin-->bird)", $("<(|,robin)-->(|,bird)>").toString());
    }

    @Test
    public void testInheritNegative() throws Narsese.NarseseException {
        assertEquals("(--,(x-->y))", inh(neg($("x")), $("y")).toString()  );
        assertEquals("(x-->y)", inh(neg($("x")), neg($("y"))).toString()  );
    }

    @Test
    public void testInvalidEquivalences() throws Narsese.NarseseException {
        assertEquals("(P<=>Q)", equi(p, q).toString());

        assertInvalid(() -> equi(impl(p, q), r));
        assertInvalid(() -> equi(equi(p, q), r));
        assertInvalidTerms("<<a <=> b> <=> c>");
    }


    @Test
    public void testEquivalenceNegatedSubtermsDoubleNeg() throws Narsese.NarseseException {
        assertEquals(("((P)<=>(Q))"), $("((--,(P))<=>(--,(Q)))").toString());
    }
    @Test
    public void testSimilarityNegatedSubtermsDoubleNeg() throws Narsese.NarseseException {
        assertEquals(("((--,(P))<->(--,(Q)))"), $("((--,(P))<->(--,(Q)))").toString()); //SAME should not change
        /*
        <patham9> <-> is a relation in meaning not in truth
        <patham9> so negation can't enforce any equivalence here
        */
    }

    @Test
    public void testSimilarityNegatedSubterms() throws Narsese.NarseseException {
//        assertEquals(("((P)<->(--,(Q)))"), $("((P)<->(--,(Q)))").toString());
//        assertEquals(("((P)<->(--,(Q)))"), $("((--,(P))<->(Q))").toString());
        assertEquals("((P)<->(--,(Q)))", $("((P)<->(--,(Q)))").toString()); //NO change
        assertEquals("((--,(P))<->(Q))", $("((--,(P))<->(Q))").toString()); //NO change
    }
    @Test public void testEquivalenceNegatedSubterms() throws Narsese.NarseseException {
        assertEquals(("(--,((P)<=>(Q)))"), $("((P)<=>(--,(Q)))").toString());
        assertEquals(("(--,((P)<=>(Q)))"), $("((--,(P))<=>(Q))").toString());
    }
    @Test public void testImplicationNegatedPredicate() throws Narsese.NarseseException {
        assertEquals("(--,((P)==>(Q)))", $("((P)==>(--,(Q)))").toString());
        assertEquals(("((--,(P))==>(Q))"), $("((--,(P))==>(Q))").toString()); //SAME should not change
    }

    @Test
    public void testReducedAndInvalidImplications1() throws Narsese.NarseseException {
        assertInvalidTerms("<<P<=>Q> ==> R>");
    }

    @Test
    public void testReducedAndInvalidImplications5() throws Narsese.NarseseException {
        assertInvalidTerms("<<P==>Q> ==> R>");
    }

    @Test
    public void testReducedAndInvalidImplications6() throws Narsese.NarseseException {
        assertInvalidTerms("<R ==> <P<=>Q>>");
    }

    @Test
    public void testReducedAndInvalidImplications2() throws Narsese.NarseseException {
        assertEquals("((P&&R)==>Q)", $("<R==><P==>Q>>").toString());
    }

    @Test
    public void testReducedAndInvalidImplications3() throws Narsese.NarseseException {
        assertInvalidTerms("<R==><P==>R>>");
    }

    @Test
    public void testReducedAndInvalidImplications4() throws Narsese.NarseseException {
        assertEquals("(R==>P)", $("(R==>(R==>P))").toString());
    }

//    @Test public void testReducedAndInvalidImplicationsTemporal() throws Narsese.NarseseException {
//        assertNull($("<<P<=>Q> =/> R>"));
//        assertNull($("<R =/> <P<=>Q>>"));
//
//        assertNull($("<<P==>Q> =/> R>"));
//        assertNull($("<<P==>Q> =|> R>"));
//        assertNull($("<<P==>Q> =|> R>"));
//    }
//
//    @Test public void testReducedAndInvalidImplicationsTemporal2() throws Narsese.NarseseException {
//        assertEquals("<(&|,P,R)=|>Q>", $("<R=|><P==>Q>>").toString());
//    }
//    @Test public void testReducedAndInvalidImplicationsTemporal3() throws Narsese.NarseseException {
//        assertEquals("<(&/,R,P)=/>Q>", $("<R=/><P==>Q>>").toString());
//    }
//    @Test public void testReducedAndInvalidImplicationsTemporal4() throws Narsese.NarseseException {
//        assertEquals("<(&/,P,R)=\\>Q>", $("<R=\\><P==>Q>>").toString());
//    }

    //TODO:
        /*
            (&,(&,P,Q),R) = (&,P,Q,R)
            (&,(&,P,Q),(&,R,S)) = (&,P,Q,R,S)

            // set union
            if (term1.op(Op.SET_INT) && term2.op(Op.SET_INT)) {

            // set intersection
            if (term1.op(Op.SET_EXT) && term2.op(Op.SET_EXT)) {

         */

    @Test
    public void testDisjunctEqual() throws Narsese.NarseseException {
        @NotNull Compound pp = p(this.p);
        assertEquals(pp, disj(pp, pp));
    }

    @Ignore @Test public void testRepeatConjunctionTaskSimplification() throws Narsese.NarseseException {
        //the repeats in the conjunction term can be replaced with a single event with equivalent start/stop time
        assertEquals(
                "$.50 (x). 0⋈10 %1.0;.90%",
                t.task("((x) &&+10 (x)). :|:").toString());
    }

    @Test
    public void testConjunctionEqual() throws Narsese.NarseseException {
        assertEquals(p, conj(p, p));
    }

    @Test
    public void testConjunctionNormal() throws Narsese.NarseseException {
        Term x = $("(&&, <#1 --> lock>, <#1 --> (/, open, #2, _)>, <#2 --> key>)");
        assertEquals(3, x.size());
        assertEquals(CONJ, x.op());
    }

    @Test
    public void testIntExtEqual() throws Narsese.NarseseException {
        assertEquals(p, secte(p, p));
        assertEquals(p, secti(p, p));
    }

    @Test
    public void testDiffIntEqual() throws Narsese.NarseseException {

        assertEquals(Null, diffi(p, p));
    }

    @Test
    public void testDiffExtEqual() throws Narsese.NarseseException {

        assertEquals(Null, diffe(p, p));
    }


    @Test
    public void testDifferenceSorted() throws Narsese.NarseseException {
//        assertArrayEquals(
//            new Term[] { r, s },
//            Terms.toArray(TermContainer.differenceSorted(sete(r, p, q, s), sete(p, q)))
//        );
        //check consistency with differenceSorted
        assertArrayEquals(
                new Term[]{r, s},
                ((Compound) terms.difference(Op.SETe, sete(r, p, q, s), sete(p, q))).toArray()
        );
    }

    @Test
    public void testDifferenceSortedEmpty() throws Narsese.NarseseException {
//        assertArrayEquals(
//                new Term[] { },
//                Terms.toArray(TermContainer.differenceSorted(sete(p, q), sete(p, q)))
//        );
        //check consistency with differenceSorted
        assertEquals(
                Null,
                terms.difference(Op.SETe, sete(p, q), sete(p, q))
        );
    }


    @Test
    public void testDifference() throws Narsese.NarseseException {
        /*tester.believe("<planetX --> {Mars,Pluto,Venus}>",0.9f,0.9f); //.en("PlanetX is Mars, Pluto, or Venus.");
        tester.believe("<planetX --> {Pluto,Saturn}>", 0.1f, 0.9f); //.en("PlanetX is probably neither Pluto nor Saturn.");
        tester.mustBelieve(cycles, "<planetX --> {Mars,Venus}>", 0.81f ,0.81f); //.en("PlanetX is either Mars or Venus.");*/


        assertEquals(
                $("{Mars,Venus}"),
                terms.difference(Op.SETe, $("{Mars,Pluto,Venus}"), $.<Compound>$("{Pluto,Saturn}"))
        );
        assertEquals(
                $("{Saturn}"),
                terms.difference(Op.SETe, $("{Pluto,Saturn}"), $.<Compound>$("{Mars,Pluto,Venus}"))
        );


//        //test identity does not create new instance, single term
//        Compound b = $("{Mars}");
//        assertTrue(
//                b ==
//                TermContainer.difference(
//                        b,
//                        $("{Pluto}")
//                )
//        );
//
//        //test identity does not create new instance, multiterm
//        Compound a = $("{Mars,Venus}");
//        assertTrue(
//                a ==
//                        TermContainer.difference(
//                                a,
//                                $("{Pluto,PlanetX}")
//                        )
//        );
    }


    @Test
    public void testDifferenceImmediate() throws Narsese.NarseseException {

        Term d = diffi(
                seti($("a"), $("b"), $("c")),
                seti($("d"), $("b")));
        assertEquals(Op.SETi, d.op());
        assertEquals(d.toString(), 2, d.size());
        assertEquals("[a,c]", d.toString());
    }

    @Test
    public void testDifferenceImmediate2() throws Narsese.NarseseException {


        Compound a = sete($("a"), $("b"), $("c"));
        Compound b = sete($("d"), $("b"));
        Term d = diffe(a, b);
        assertEquals(Op.SETe, d.op());
        assertEquals(d.toString(), 2, d.size());
        assertEquals("{a,c}", d.toString());

    }

    @Test
    public void testDisjunctionReduction() throws Narsese.NarseseException {

        assertEquals("(||,(a-->x),(b-->x),(c-->x),(d-->x))",
                $("(||,(||,x:a,x:b),(||,x:c,x:d))").toString());
        assertEquals("(||,(b-->x),(c-->x),(d-->x))",
                $("(||,x:b,(||,x:c,x:d))").toString());
    }

    @Test
    public void testConjunctionReduction() throws Narsese.NarseseException {
        assertEquals("(&&,a,b,c,d)",
                $("(&&,(&&,a,b),(&&,c,d))").toString());
        assertEquals("(&&,b,c,d)",
                $("(&&,b,(&&,c,d))").toString());
    }

    @Test
    public void testTemporalConjunctionReduction1() throws Narsese.NarseseException {
        assertEquals("(a &&+0 b)", $("(a &&+0 b)").toString());
        assertEquals(
                $("((--,(ball_left)) &&-270 (ball_right))"),
                $("((ball_right) &&+270 (--,(ball_left)))"));

    }

    @Test
    public void testTemporalConjunctionReduction2() throws Narsese.NarseseException {
        assertEquals("(a &&+0 (b &&+1 c))", $("(a &&+0 (b &&+1 c))").toString());
    }

    @Test
    public void testTemporalConjunctionReduction3() throws Narsese.NarseseException {
        assertEquals("(a &&+0 b)", $("( (a &&+0 b) && (a &&+0 b) )").toString());
    }

    @Test
    public void testTemporalConjunctionReduction5() throws Narsese.NarseseException {
        assertEquals("((a &&+0 b)&&(a &&+1 b))",
                $("( (a &&+0 b) && (a &&+1 b) )").toString());
    }

    @Test
    public void testTemporalConjunctionReduction4() throws Narsese.NarseseException {
        assertEquals("(a &&+0 b)", $("( a &&+0 (b && b) )").toString());
    }


    @Test
    public void testTemporalNTermConjunctionParallel() throws Narsese.NarseseException {
        //+0 is the only case in which temporal && can have arity>2
        //TODO fix spacing:
        assertEquals("( &&+0 ,a,b,c)", $("( a &&+0 (b &&+0 c) )").toString());
    }

    @Ignore
    @Test
    public void testTemporalNTermEquivalenceParallel() throws Narsese.NarseseException {
        //+0 is the only case in which temporal && can have arity>2
        assertEquals("(<=>+0, a, b, c)", $("( a <=>+0 (b <=>+0 c) )").toString());
    }


    @Test
    public void testMultireduction() throws Narsese.NarseseException {
        //TODO probably works
    }

    @Test
    public void testConjunctionMultipleAndEmbedded() throws Narsese.NarseseException {

        assertEquals("(&&,a,b,c,d)",
                $("(&&,(&&,a,b),(&&,c,d))").toString());
        assertEquals("(&&,a,b,c,d,e,f)",
                $("(&&,(&&,a,b),(&&,c,d), (&&, e, f))").toString());
        assertEquals("(&&,a,b,c,d,e,f,g,h)",
                $("(&&,(&&,a,b, (&&, g, h)),(&&,c,d), (&&, e, f))").toString());
    }

    @Test
    public void testConjunctionEquality() throws Narsese.NarseseException {

        assertEquals(
                $("(&&,r,s)"),
                $("(&&,s,r)"));
//        assertNotEquals(
//            $("(&/,r,s)"),
//            $("(&/,s,r)"));
//        assertEquals(
//            $("(&|,r,s)"),
//            $("(&|,s,r)"));

    }

    @Test
    public void testImplicationInequality() throws Narsese.NarseseException {

        assertNotEquals(
                $("<r ==> s>"),
                $("<s ==> r>"));
//        assertNotEquals(
//                $("<r =/> s>"),
//                $("<s =/> r>"));
//        assertNotEquals(
//                $("<r =\\> s>"),
//                $("<s =\\> r>"));
//        assertNotEquals(
//                $("<r =|> s>"),
//                $("<s =|> r>"));

    }

    @Test
    public void testDisjunctionMultipleAndEmbedded() throws Narsese.NarseseException {

        assertEquals("(||,(a),(b),(c),(d))",
                $("(||,(||,(a),(b)),(||,(c),(d)))").toString());
        assertEquals("(||,(a),(b),(c),(d),(e),(f))",
                $("(||,(||,(a),(b)),(||,(c),(d)), (||,(e),(f)))").toString());
        assertEquals("(||,(a),(b),(c),(d),(e),(f),(g),(h))",
                $("(||,(||,(a),(b), (||,(g),(h))),(||,(c),(d)), (||,(e),(f)))").toString());

    }

    @Test
    public void testImplicationConjCommonSubterms() throws Narsese.NarseseException {
        assertEquals("((b&&c)==>d)",
                $("((&&, a, b, c) ==> (&&, a, d))").toString());
        assertEquals("(d==>(b&&c))",
                $("((&&, a, d) ==> (&&, a, b, c))").toString());
        assertInvalidTerms("((&&, a, b, c) ==> (&&, a, b))");
        assertEquals("c", $("((&&, a, b) ==> (&&, a, b, c))").toString());
        assertInvalidTerms("((&&, a, b, c) ==> a)");
        assertInvalidTerms("(a ==> (&&, a, b, c))");
    }

    @Test public void testConegatedConjunctionTerms0() throws Narsese.NarseseException {
        assertEquals(False, $("(#1 && (--,#1))"));
        assertEquals(False, $("(&&, #1, (--,#1), (x))"));

        assertTrue( $("(#1 &&+1 (--,#1))") instanceof Compound);
        assertTrue( $("((x) &&+1 --(x))") instanceof Compound);


    }


    @Test public void testConegatedConjunctionTerms01() throws Narsese.NarseseException {
        assertEquals(False, parallel(varDep(1), neg(varDep(1))));
    }

    @Test public void testInvalidStatementIndepVarTask() throws Narsese.NarseseException {
        Terminal t = new Terminal();
        try {
            t.inputAndGet("at($1,$2,$3)");
            assertTrue(false);
        } catch (InvalidTaskException e) {
            assertTrue(true);
        }
    }

    @Test public void testConegatedConjunctionTerms1() throws Narsese.NarseseException {
        assertEquals($("((--,((y)&&(z)))&&(x))"), $("((x) && --((y) && (z)))"));
    }
     @Test public void testConegatedConjunctionTerms0not() throws Narsese.NarseseException {
        //dont unwrap due to different 'dt'
        assertEquals("((--,((y) &&+0 (z)))&&(x))", $("((x)&&--((y) &&+0 (z)))").toString());

        assertEquals("((--,((y)&&(z))) &&+0 (x))", $("((x) &&+0 --((y) && (z)))").toString());
    }
    @Test public void testConegatedConjunctionTerms1not() throws Narsese.NarseseException {
        //dont unwrap due to different 'dt'
        assertEquals("((--,((y) &&+1 (z)))&&(x))", $("((x)&&--((y) &&+1 (z)))").toString());

        assertEquals("((x) &&+1 (--,((y)&&(z))))", $("((x) &&+1 --((y) && (z)))").toString());
    }

    @Test public void testConegatedConjunctionTerms2() throws Narsese.NarseseException {
        //(x && not(x&&y)) |- x && not(y)
        assertEquals(
                "(#1&&(--,(robin-->swimmer)))",
                $("(#1 && --(#1&&(robin-->swimmer)))").toString()
        );
    }

    @Test
    public void testDemorgan1() throws Narsese.NarseseException {
        //https://en.wikipedia.org/wiki/De_Morgan%27s_laws


        // \neg(P\and Q)\iff(\neg P)\or(\neg Q)
        assertEquals("(--,((p)&&(q)))",
                $("(--(p) || --(q))").toString());
    }

    @Ignore
    @Test
    public void testDemorgan2() throws Narsese.NarseseException {

        // \neg(P\or Q)\iff(\neg P)\and(\neg Q),
        assertEquals("(--,((p)||(q)))",
                $("(--(p) && --(q))").toString());
    }

    @Test
    public void testCoNegatedJunction() throws Narsese.NarseseException {
        //the conegation cancels out conflicting terms

        assertEquals(False, $("(&&,x,a:b,(--,a:b))"));

        assertEquals(False, $("(&&, (a), (--,(a)), (b))")); //a cancels, reduce to 'b'
        assertEquals(False, $("(&&, (a), (--,(a)), (b), (c))"));


        assertEquals(False, $("(&&,x,y,a:b,(--,a:b))"));
    }


    @Test
    public void testCoNegatedDisjunction() throws Narsese.NarseseException {

        assertEquals(True,
                $("(||,x,a:b,(--,a:b))"));

        assertEquals(True,
                $("(||,x,y,a:b,(--,a:b))"));

        assertEquals(False, parallel(varDep(0), neg(varDep(0)), Atomic.the("x")));
    }

    @Test
    public void testFilterCommutedWithCoNegatedSubterms() throws Narsese.NarseseException {
        //any commutive terms with both a subterm and its negative are invalid


        assertValidTermValidConceptInvalidTaskContent(("((--,(a1)) && (a1))"));
        assertValidTermValidConceptInvalidTaskContent("((--,(a1)) &&+0 (a1))");
        assertValid($("((--,(a1)) &&+1 (a1))"));
        assertValid($("((a1) &&+1 (a1))"));

        assertEquals($("(a1)"), $("((a1) &&+0 (a1))"));
        assertEquals($("(a1)"), $("((a1) && (a1))"));
        assertNotEquals($("(a1)"), $("((a1) &&+1 (a1))"));

        assertInvalidTerms("((--,(a1)) || (a1))");

    }

    @Test public void testRepeatInverseEquivalent() throws Narsese.NarseseException {
        assertEquals($("((a1) &&-1 (a1))"), $("((a1) &&+1 (a1))"));
        assertEquals($("((a1) <=>-1 (a1))"), $("((a1) <=>+1 (a1))"));
    }

    @Test
    public void testFilterCoNegatedStatements() throws Narsese.NarseseException {
        assertEquals(False, $("((--,(a1)) <-> (a1))"));
        assertEquals(False, $("((--,(a1)) --> (a1))"));
    }


    @Test
    public void testCoNegatedImpl() throws Narsese.NarseseException {
        assertValidTermValidConceptInvalidTaskContent(("((--,(a)) ==> (a))"));
        assertValidTermValidConceptInvalidTaskContent(("((--,(a)) ==>+0 (a))"));
    }

    @Test
    public void testCoNegatedEqui() throws Narsese.NarseseException {
        assertValidTermValidConceptInvalidTaskContent(("((--,(a)) <=>+- (a))"));

        assertEquals(False, $("((--,(a)) <=> (a))"));

        assertEquals(False, $("((--,(a)) <=>+0 (a))"));

        String e = "(--,((a) <=>+1 (a)))"; assertEquals(e, $(e).toString());

        //due to the unpaired $3
        assertInvalidTasks("(((--,isIn($1,xyz))&&(--,(($1,xyz)-->$2)))<=>((--,(($1,xyz)-->$2))&&(--,isIn($3,xyz)))).");
    }

    @Test
    public void testEquivCommonSubterms() throws Narsese.NarseseException {
        //factor out the common sub-term
        assertEquals(
                "(--,(isIn($1,xyz)<=>((y-->x))))", //involves an additional negation factoring out to top level
                $("(((--,isIn($1,xyz))&&(--,(($1,xyz)-->$2)))<=>((--,(($1,xyz)-->$2))&&(x:y)))").toString());


    }

    @Test public void testCoNegatedImplOK() throws Narsese.NarseseException {
        assertValid($("((--,(a)) ==>+1 (a))"));
        assertValid($("((--,a) ==>+1 a)"));
    }
    @Test public void testCoNegatedEquiOK() throws Narsese.NarseseException {
        assertValid($("((--,(a)) <=>+1 (a))"));
        assertValid($("((--,a) <=>+1 a)"));
    }

    @Test public void testRepeatEvent() throws Narsese.NarseseException {
        Terminal n = new Terminal();

        for (String x : new String[] { "((a) ==>+1 (a))",
            /*"((a) &&+1 (a))",*/ //<-- conjunction case is special, see repeating conjunction simplification test
            "((a) <=>+1 (a))" }) {
            Term t = $(x);
            assertValid(t);

            Task y = task((Compound)t, Op.BELIEF, t(1f,0.9f)).apply(n);

            assertEquals(x, y.term().toString());

        }



    }


    @Test
    public void testCoNegatedDifference() throws Narsese.NarseseException {
        //..
    }

    @Test
    public void testCoNegatedIntersection() throws Narsese.NarseseException {
        //..
    }

    @Test public void testSimEquivOfAbsoluteTrueNull1() throws Narsese.NarseseException {
        assertEquals(True, sim(True, True));
    }
    @Test public void testSimEquivOfAbsoluteTrueNull2() throws Narsese.NarseseException {
        assertEquals(Null, sim(Null, Null));
    }
    @Test public void testSimEquivOfAbsoluteTrueNull3() throws Narsese.NarseseException {
        assertEquals(Null, sim(True, Null));
    }
    @Test public void testSimEquivOfAbsoluteTrueNull4() throws Narsese.NarseseException {
        assertEquals(Null, sim(neg(True),Null));
        assertEquals(False, sim(True,False));
        assertEquals(True, sim(neg(True),False));
    }

    /**
     * conjunction and disjunction subterms which can occurr as a result
     * of variable substitution, etc which don't necessarily affect
     * the resulting truth of the compound although if the statements
     * were alone they would not form valid tasks themselves
     */
    @Test
    public void testSingularStatementsInConjunction() throws Narsese.NarseseException {
        assertEquals($("(&&,c:d,e:f)"), $("(&&,(a<->a),c:d,e:f)"));
        assertEquals($("(&&,c:d,e:f)"), $("(&&,(a<=>a),c:d,e:f)"));
        assertEquals($("(&&,c:d,e:f)"), $("(&&,(a-->a),c:d,e:f)"));
        assertEquals($("(&&,c:d,e:f)"), $("(&&,(a==>a),c:d,e:f)"));
        assertEquals(False, $("(&&,(--,(a==>a)),c:d,e:f)"));

    }

    @Test
    public void testSingularStatementsInDisjunction() throws Narsese.NarseseException {

        assertInvalidTerms("(||,(a<->a),c:d,e:f)"); //null, singular true
    }

    @Test
    public void testSingularStatementsInDisjunction2() throws Narsese.NarseseException {
        assertEquals($("x:y"), $("(&&,(||,(a<->a),c:d,e:f),x:y)")); //double fall-thru
        assertEquals(False, $("(&&,(--,(||,(a<->a),c:d,e:f)),x:y)")); //double fall-thru

//        assertEquals($("(||,c:d,e:f)"), $("(||,(a<=>a),c:d,e:f)"));
//        assertEquals($("(||,c:d,e:f)"), $("(||,(a-->a),c:d,e:f)"));
//        assertEquals($("(||,c:d,e:f)"), $("(||,(a==>a),c:d,e:f)"));
//        assertEquals($("(||,c:d,e:f)"), $("(||,(--,(a==>a)),c:d,e:f)")); //VALID

    }

    @Test
    public void testOneArgIntersection() throws Narsese.NarseseException {
        Term x = $.p($.the("x"));
        assertEquals(x, $("(|,(x))"));
        assertEquals(x, $("(|,(x),(x))"));
        assertEquals(x, $("(&,(x))"));
        assertEquals(x, $("(&,(x),(x))"));
    }

    @Test
    public void testCoNegatedIntersectionAndDiffs() throws Narsese.NarseseException {
        assertInvalidTerms("(|,(x),(--,(x))");
        assertInvalidTerms("(&,(x),(--,(x))");
        assertInvalidTerms("(-,(x),(--,(x))");
        assertInvalidTerms("(~,(x),(--,(x))");
        assertInvalidTerms("(-,(x),(x))");
    }

    @Test
    public void testGroupNonDTemporalParallelComponents() throws Narsese.NarseseException {
        //$.76;.45;.70$ ( &&+0 ,(ball_left),(ball_right),((--,(ball_left)) &&-270 (ball_right))). :3537: %.64;.15%
        //$.39;.44;.70$ (((--,(ball_left)) &&-233 (ball_right)) &&-1 ((ball_left) &&+0 (ball_right))). :3243: %.53;.23%
        assertEquals(
                "( &&+0 ,(ball_left),(ball_right),((ball_right) &&+270 (--,(ball_left))))",

                //HACK: this narsese parser isnt implemented yet:
                //$("( &&+0 ,(ball_left),(ball_right),((--,(ball_left)) &&-270 (ball_right)))")

                parallel($("(ball_left)"), $("(ball_right)"), $("((--,(ball_left)) &&-270 (ball_right)))"))
                        .toString());

    }

    @Test
    public void testReducibleImpl() throws Narsese.NarseseException {
        assertInvalidTerms("((--,(x)) ==>+0 ((--,(y)) &&+0 (--,(x))))");
        assertInvalidTerms("((--,(x)) ==> ((--,(y)) && (--,(x))))");
    }

    @Test public void testImplInImplDTernal() throws Narsese.NarseseException {
        assertEquals(
                "(((--,(in))&&(happy))==>(out))",
                $("((--,(in)) ==> ((happy)  ==> (out)))").toString());
    }
    @Test public void testImplInImplDTemporal() throws Narsese.NarseseException {
        assertEquals(
                "(((--,(in)) &&+1 (happy)) ==>+2 (out))",
                $("((--,(in)) ==>+1 ((happy) ==>+2 (out)))").toString());
    }


    @Test
    public void testConjunctiveCoNegationAcrossImpl() throws Narsese.NarseseException {
        //((--,(&&,(--,(pad_top)),(pad_bottom),(pad_top))) ==>+133 (--,(pad_bottom)))! :4355: %.73;.24%

        /*
        (
            (&&,(--,(23)),(--,(31)),(23),(31))
                <=>
            (&&,(--,(23)),(--,(31)),(23),(31),((--,(31)) &&+98 (23)))) (class nars.term.compound.GenericCompound): Failed atemporalization, becoming: ¿".
        ((&&,(--,(2,3)),(--,(3,1)),(2,3),(3,1))<=>(&&,(--,(2,3)),(--,(3,1)),(2,3),(3,1),((--,(3,1)) &&+98 (2,3)))) (class nars.term.compound.GenericCompound): Failed atemporalization, becoming: ¿".
        ((&&,(--,(0,2)),(--,(2,0)),((((--,(0,2)) &&+428 (--,(2,0))) ==>+1005 (--,(2,0))) &&+0 ((--,(2,0)) <=>-1005 ((--,(0,2)) &&+428 (--,(2,0))))))<=>(&&,(--,(0,2)),((--,(0,2)) &&-395 (--,(2,0))),((((--,(0,2)) &&+428 (--,(2,0))) ==>+1005 (--,(2,0))) &&+0 ((--,(2,0)) <=>-1005 ((--,(0,2)) &&+428 (--,(2,0))))))) (class nars.term.compound.GenericCompound): Failed atemporalization, becoming: ¿".
        temporal conjunction requires exactly 2 arguments {&&, dt=-125, args=[(1,4), ((&&,(--,(1,4)),(--,(2,4)),(2,4)) ==>+125 (--,(1,4))), ((&&,(--,(1,4)),(--,(2,4)),(1,4),(2,4)) ==>+125 (--,(1,4)))]}
            temporalizing from (&&,(1,4),((&&,(--,(1,4)),(--,(2,4)),(2,4)) ==>+125 (--,(1,4))),((&&,(--,(1,4)),(--,(2,4)),(1,4),(2,4)) ==>+125 (--,(1,4))))
            deriving rule <(P ==> M), (S ==> M), neq(S,P), time(dtBminT) |- (S ==> P), (Belief:Induction, Derive:AllowBackward)>".
        */


    }

    @Test public void testImageUnwrap0() throws Narsese.NarseseException {
        assertEquals("(a,b)",
           p(imageUnwrap($("(\\,n,_,b)"), $("a"))).toString());
    }
    @Test public void testImageUnwrap1() throws Narsese.NarseseException {
        assertEquals("(a,b)",
            p(imageUnwrap($("(\\,n,a,_)"), $("b"))).toString());
    }
    @Test public void testImageUnwrap2() throws Narsese.NarseseException {
        assertEquals("(a,b)",
            p(imageUnwrap($("(/,n,_,b)"), $("a"))).toString());
    }
    @Test public void testImageUnwrap3() throws Narsese.NarseseException {
        assertEquals(     "(a,b)",
           p(imageUnwrap($("(/,n,a,_)"), $("b"))).toString());
    }

    @Test public void testImageInSubtermsProductNormalFormIntensional() throws Narsese.NarseseException {

        //<neutralization --> (acid,base)>" //en("Neutralization is a relation between an acid and a base. ");
        //  <(\,neutralization,acid,_) --> base> //en("Something that can be neutralized by an acid is a base.");
        //  <(\,neutralization,_,base) --> acid> //en("Something that can neutralize a base is an acid.");
        assertEquals(
                "((x)==>(n-->(a,b)))",
                $("((x)==>((\\,n,a,_)-->b))").toString());
        assertEquals(
                "((x)==>(n-->(a,b)))",
                $("((x)==>((\\,n,_,b)-->a))").toString());
    }

    @Test public void testImageInSubtermsProductNormalFormExtensional() throws Narsese.NarseseException {
        //<(acid,base) --> reaction> //en("An acid and a base can have a reaction.");
        //  <base --> (/,reaction,acid,_)> //en("A base is something that has a reaction with an acid.");
        //  <acid --> (/,reaction,_,base)> //en("Acid can react with base.");
        assertEquals(
                  "{r(a,b)}",
                $("{(b-->(/,r,a,_))}").toString());

        assertEquals(
                  "{likes(cat,{sky})}",
                $("{({sky}-->(/,likes,cat,_))}").toString());

        assertEquals(
                "(--,r(a,b))",
                $("(--,(a-->(/,r,_,b)))").toString());

    }
    @Test public void testNegatedImageInSubtermsProductNormalForm() throws Narsese.NarseseException {
        assertEquals(
                  "{(--,r(a,b)),(z-->(x,y))}",
                $("{ (--,(b-->(/,r,a,_))), ((\\,z,x,_)-->y) }").toString());

    }

    @Test public void testConjDisjNeg() throws Narsese.NarseseException {
        assertEquals(
                "((--,(out))&&(happy))",
                $("((--,(out))&&(||,(happy),(out)))").toString());
    }

    @Test public void taskWithFlattenedConunctions() {
        //$0.0;NaN$ ((busyVol)&&((busyPri)&&(busyVol))). %.19;.10%  //<-- should not be allowed to be constructed
        //  instead should have been: (busyVol&&busyPri)

    }

    @Test public void reduceComplex() throws Narsese.NarseseException {
        String s = "(((rotate-->tetris) &&+2 (x-->tetris)) <=>+8236 ((--,(rotate-->tetris)) &&+3 (--,((--,(rotate-->tetris)) &&+0 ((rotate-->tetris)&&((x-->tetris)&&((rotate-->tetris)&&(x-->tetris))))))))";

        Term t = $(s);

        assertEquals(
                //"(((rotate-->tetris) &&+2 (x-->tetris)) <=>+8236 ((--,(rotate-->tetris)) &&+3 (--,((--,(rotate-->tetris)) &&+0 ((rotate-->tetris)&&(x-->tetris))))))",
                "(--,(((rotate-->tetris) &&+2 (x-->tetris)) <=>+8236 (rotate-->tetris)))", //TODO check this reduction
                t.toString());
    }

    @Test public void testPromoteEternalToParallel() throws Narsese.NarseseException {
        String s = "(a &&+0 (b && c))";
        assertEquals(
                "( &&+0 ,a,b,c)",
                $(s).toString()
        );
    }
    @Test public void testPromoteEternalToParallelDont() throws Narsese.NarseseException {
        String s = "(a && (b &&+0 c))";
        assertEquals(
                "(a&&(b &&+0 c))",
                $(s).toString()
        );
    }

    @Test public void testCoNegatedConjunctionParallelEternal() throws Narsese.NarseseException {
        //mix of parallel and eternal
        assertEquals(False,
            $("((--,(happy-->noid)) &&+0 ((--,((x-->ball)&&(x-->paddle)))&&(happy-->noid)))")
        );
    }

    @Test public void testCoNegatedImplication() throws Narsese.NarseseException {
        assertEquals(False,
            $("((--,$1) ==>+0 (((--,$1) &&+0 (--,#2)) &&+16 #2))")
        );
    }


    @Test public void testConjImplReduction1() throws Narsese.NarseseException {
        assertEquals(
                "((inside(bob,office) &&+0 inside(john,playground))==>inside(bob,kitchen))",
                $("(inside(bob,office) &&+0 (inside(john,playground)==>inside(bob,kitchen)))").toString()
        );
    }

    @Test public void testConjImplReduction2() throws Narsese.NarseseException {
        //with some dt's
        assertEquals(
                "((inside(bob,office) &&+1 inside(john,playground)) ==>+1 inside(bob,kitchen))",
                $("(inside(bob,office) &&+1 (inside(john,playground) ==>+1 inside(bob,kitchen)))").toString()
        );
    }
      @Test public void testConjImplReduction3() throws Narsese.NarseseException {
        //with some dt's
        assertEquals(
                "((inside(john,playground) &&+1 inside(bob,office)) ==>-1 inside(bob,kitchen))",
                $("(inside(bob,office) &&-1 (inside(john,playground) ==>-1 inside(bob,kitchen)))").toString()
        );
    }

}

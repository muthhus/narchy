package nars.term;

import nars.$;
import nars.Op;
import nars.term.compound.UnitCompound1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Ignore;
import org.junit.Test;

import static nars.$.*;
import static nars.Op.CONJ;
import static nars.Op.NEG;
import static nars.Op.PROD;
import static nars.io.NarseseTest.assertInvalid;
import static nars.nal.TermBuilder.imageUnwrap;
import static nars.term.Term.False;
import static nars.term.Term.True;
import static nars.term.TermTest.assertValid;
import static nars.term.TermTest.assertValidTermValidConceptInvalidTaskContent;
import static org.junit.Assert.*;

/**
 * Created by me on 12/10/15.
 */
public class TermReductionsTest {

    @Nullable
    final Term p = $("P"), q = $("Q"), r = $("R"), s = $("S");


    @Test
    public void testIntersectExtReduction1() {
        // (&,R,(&,P,Q)) = (&,P,Q,R)
        assertEquals("(&,P,Q,R)", secte(r, secte(p, q)).toString());
        assertEquals("(&,P,Q,R)", $("(&,R,(&,P,Q))").toString());
    }

    @Test
    public void testIntersectExtReduction2() {
        // (&,(&,P,Q),(&,R,S)) = (&,P,Q,R,S)
        assertEquals("(&,P,Q,R,S)", secte(secte(p, q), secte(r, s)).toString());
        assertEquals("(&,P,Q,R,S)", $("(&,(&,P,Q),(&,R,S))").toString());
    }

    @Test
    public void testIntersectExtReduction3() {
        // (&,(&,P,Q),(&,R,S)) = (&,P,Q,R,S)
        assertEquals("(&,P,Q,R,S,T,U)", $("(&,(&,P,Q),(&,R,S), (&,T,U))").toString());
    }

    @Test
    public void testIntersectExtReduction2_1() {
        // (&,R,(&,P,Q)) = (&,P,Q,R)
        assertEquals("(&,P,Q,R)", $("(&,R,(&,P,Q))").toString());
    }

    @Test
    public void testIntersectExtReduction4() {
        //UNION if (term1.op(Op.SET_INT) && term2.op(Op.SET_INT)) {
        assertEquals("{P,Q,R,S}", secte(sete(p, q), sete(r, s)).toString());
        assertEquals("{P,Q,R,S}", $("(&,{P,Q},{R,S})").toString());
    }

    @Test
    public void testIntersectExtReduction5() {
        assertEquals(False /* emptyset */, secte(seti(p, q), seti(r, s)));
    }

    @Test
    public void testIntersectIntReduction1() {
        // (|,R,(|,P,Q)) = (|,P,Q,R)
        assertEquals("(|,P,Q,R)", secti(r, secti(p, q)).toString());
        assertEquals("(|,P,Q,R)", $("(|,R,(|,P,Q))").toString());
    }

    @Test
    public void testIntersectIntReduction2() {
        // (|,(|,P,Q),(|,R,S)) = (|,P,Q,R,S)
        assertEquals("(|,P,Q,R,S)", secti(secti(p, q), secti(r, s)).toString());
        assertEquals("(|,P,Q,R,S)", $("(|,(|,P,Q),(|,R,S))").toString());
    }

    @Test
    public void testIntersectIntReduction3() {
        // (|,R,(|,P,Q)) = (|,P,Q,R)
        assertEquals("(|,P,Q,R)", $("(|,R,(|,P,Q))").toString());
    }

    @Test
    public void testIntersectIntReduction4() {
        //UNION if (term1.op(Op.SET_INT) || term2.op(Op.SET_INT)) {
        assertEquals("[P,Q,R,S]", secti(seti(p, q), seti(r, s)).toString());
        assertEquals("[P,Q,R,S]", $("(|,[P,Q],[R,S])").toString());

    }

    @Test
    public void testIntersectIntReductionToZero() {
        assertInvalid("(|,{P,Q},{R,S})");
    }

    @Test
    public void testIntersectIntReduction_to_one() {
        assertEquals("(robin-->bird)", $("<robin-->(|,bird)>").toString());
        assertEquals("(robin-->bird)", $("<(|,robin)-->(|,bird)>").toString());
    }


    @Test
    public void testInvalidEquivalences() {
        assertEquals("(P<=>Q)", equi(p, q).toString());

        TermTest.assertInvalid(() -> equi(impl(p, q), r));
        TermTest.assertInvalid(() -> equi(equi(p, q), r));
        assertInvalid("<<a <=> b> <=> c>");
    }


    @Test
    public void testEquivalenceNegatedSubtermsDoubleNeg() {
        assertEquals(("((P)<=>(Q))"), $("((--,(P))<=>(--,(Q)))").toString());
    }
    @Test
    public void testSimilarityNegatedSubtermsDoubleNeg() {
        assertEquals(("((--,(P))<->(--,(Q)))"), $("((--,(P))<->(--,(Q)))").toString()); //SAME should not change
        /*
        <patham9> <-> is a relation in meaning not in truth
        <patham9> so negation can't enforce any equivalence here
        */
    }

    @Test
    public void testSimilarityNegatedSubterms() {
//        assertEquals(("((P)<->(--,(Q)))"), $("((P)<->(--,(Q)))").toString());
//        assertEquals(("((P)<->(--,(Q)))"), $("((--,(P))<->(Q))").toString());
        assertEquals("((P)<->(--,(Q)))", $("((P)<->(--,(Q)))").toString()); //NO change
        assertEquals("((--,(P))<->(Q))", $("((--,(P))<->(Q))").toString()); //NO change
    }
    @Test public void testEquivalenceNegatedSubterms() {
        assertEquals(("(--,((P)<=>(Q)))"), $("((P)<=>(--,(Q)))").toString());
        assertEquals(("(--,((P)<=>(Q)))"), $("((--,(P))<=>(Q))").toString());
    }
    @Test public void testImplicationNegatedPredicate() {
        assertEquals("(--,((P)==>(Q)))", $("((P)==>(--,(Q)))").toString());
        assertEquals(("((--,(P))==>(Q))"), $("((--,(P))==>(Q))").toString()); //SAME should not change
    }

    @Test
    public void testReducedAndInvalidImplications1() {
        assertInvalid("<<P<=>Q> ==> R>");
    }

    @Test
    public void testReducedAndInvalidImplications5() {
        assertInvalid("<<P==>Q> ==> R>");
    }

    @Test
    public void testReducedAndInvalidImplications6() {
        assertInvalid("<R ==> <P<=>Q>>");
    }

    @Test
    public void testReducedAndInvalidImplications2() {
        assertEquals("((P&&R)==>Q)", $("<R==><P==>Q>>").toString());
    }

    @Test
    public void testReducedAndInvalidImplications3() {
        assertInvalid("<R==><P==>R>>");
    }

    @Test
    public void testReducedAndInvalidImplications4() {
        assertEquals("(R==>P)", $("(R==>(R==>P))").toString());
    }

//    @Test public void testReducedAndInvalidImplicationsTemporal() {
//        assertNull($("<<P<=>Q> =/> R>"));
//        assertNull($("<R =/> <P<=>Q>>"));
//
//        assertNull($("<<P==>Q> =/> R>"));
//        assertNull($("<<P==>Q> =|> R>"));
//        assertNull($("<<P==>Q> =|> R>"));
//    }
//
//    @Test public void testReducedAndInvalidImplicationsTemporal2() {
//        assertEquals("<(&|,P,R)=|>Q>", $("<R=|><P==>Q>>").toString());
//    }
//    @Test public void testReducedAndInvalidImplicationsTemporal3() {
//        assertEquals("<(&/,R,P)=/>Q>", $("<R=/><P==>Q>>").toString());
//    }
//    @Test public void testReducedAndInvalidImplicationsTemporal4() {
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
    public void testDisjunctEqual() {
        @NotNull Compound pp = $.p(this.p);
        assertEquals(pp, disj(pp, pp));
    }

    @Test
    public void testConjunctionEqual() {
        assertEquals(p, $.conj(p, p));
    }

    @Test
    public void testConjunctionNormal() {
        Term x = $.$("(&&, <#1 --> lock>, <#1 --> (/, open, #2, _)>, <#2 --> key>)");
        assertEquals(3, x.size());
        assertEquals(CONJ, x.op());
    }

    @Test
    public void testIntExtEqual() {
        assertEquals(p, $.secte(p, p));
        assertEquals(p, secti(p, p));
    }

    @Test
    public void testDiffIntEqual() {

        assertEquals(False, diffi(p, p));
    }

    @Test
    public void testDiffExtEqual() {

        assertEquals(False, diffe(p, p));
    }

    @Test
    public void testDifferenceSorted() {
//        assertArrayEquals(
//            new Term[] { r, s },
//            Terms.toArray(TermContainer.differenceSorted(sete(r, p, q, s), sete(p, q)))
//        );
        //check consistency with differenceSorted
        assertArrayEquals(
                new Term[]{r, s},
                ((Compound) terms.difference(Op.SETe, sete(r, p, q, s), sete(p, q))).terms()
        );
    }

    @Test
    public void testDifferenceSortedEmpty() {
//        assertArrayEquals(
//                new Term[] { },
//                Terms.toArray(TermContainer.differenceSorted(sete(p, q), sete(p, q)))
//        );
        //check consistency with differenceSorted
        assertEquals(
                False,
                terms.difference(Op.SETe, sete(p, q), sete(p, q))
        );
    }


    @Test
    public void testDifference() {
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
    public void testDifferenceImmediate() {

        Term d = diffi(
                seti($("a"), $("b"), $("c")),
                seti($("d"), $("b")));
        assertEquals(Op.SETi, d.op());
        assertEquals(d.toString(), 2, d.size());
        assertEquals("[a,c]", d.toString());
    }

    @Test
    public void testDifferenceImmediate2() {


        Compound a = $.sete($("a"), $("b"), $("c"));
        Compound b = $.sete($("d"), $("b"));
        Term d = diffe(a, b);
        assertEquals(Op.SETe, d.op());
        assertEquals(d.toString(), 2, d.size());
        assertEquals("{a,c}", d.toString());

    }

    @Test
    public void testDisjunctionReduction() {

        assertEquals("(||,(a-->x),(b-->x),(c-->x),(d-->x))",
                $("(||,(||,x:a,x:b),(||,x:c,x:d))").toString());
        assertEquals("(||,(b-->x),(c-->x),(d-->x))",
                $("(||,x:b,(||,x:c,x:d))").toString());
    }

    @Test
    public void testConjunctionReduction() {
        assertEquals("(&&,a,b,c,d)",
                $("(&&,(&&,a,b),(&&,c,d))").toString());
        assertEquals("(&&,b,c,d)",
                $("(&&,b,(&&,c,d))").toString());
    }

    @Test
    public void testTemporalConjunctionReduction1() {
        assertEquals("(a &&+0 b)", $("(a &&+0 b)").toString());
    }

    @Test
    public void testTemporalConjunctionReduction2() {
        assertEquals("(a &&+0 (b &&+1 c))", $("(a &&+0 (b &&+1 c))").toString());
    }

    @Test
    public void testTemporalConjunctionReduction3() {
        assertEquals("(a &&+0 b)", $("( (a &&+0 b) && (a &&+0 b) )").toString());
    }

    @Test
    public void testTemporalConjunctionReduction5() {
        assertEquals("((a &&+0 b)&&(a &&+1 b))",
                $("( (a &&+0 b) && (a &&+1 b) )").toString());
    }

    @Test
    public void testTemporalConjunctionReduction4() {
        assertEquals("(a &&+0 b)", $("( a &&+0 (b && b) )").toString());
    }

    @Test
    public void testTemporalNTermConjunctionParallel() {
        //+0 is the only case in which temporal && can have arity>2
        //TODO fix spacing:
        assertEquals("( &&+0 ,a,b,c)", $("( a &&+0 (b &&+0 c) )").toString());
    }

    @Ignore
    @Test
    public void testTemporalNTermEquivalenceParallel() {
        //+0 is the only case in which temporal && can have arity>2
        assertEquals("(<=>+0, a, b, c)", $("( a <=>+0 (b <=>+0 c) )").toString());
    }


    @Test
    public void testMultireduction() {
        //TODO probably works
    }

    @Test
    public void testConjunctionMultipleAndEmbedded() {

        assertEquals("(&&,a,b,c,d)",
                $("(&&,(&&,a,b),(&&,c,d))").toString());
        assertEquals("(&&,a,b,c,d,e,f)",
                $("(&&,(&&,a,b),(&&,c,d), (&&, e, f))").toString());
        assertEquals("(&&,a,b,c,d,e,f,g,h)",
                $("(&&,(&&,a,b, (&&, g, h)),(&&,c,d), (&&, e, f))").toString());
    }

    @Test
    public void testConjunctionEquality() {

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
    public void testImplicationInequality() {

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
    public void testDisjunctionMultipleAndEmbedded() {

        assertEquals("(||,(a),(b),(c),(d))",
                $("(||,(||,(a),(b)),(||,(c),(d)))").toString());
        assertEquals("(||,(a),(b),(c),(d),(e),(f))",
                $("(||,(||,(a),(b)),(||,(c),(d)), (||,(e),(f)))").toString());
        assertEquals("(||,(a),(b),(c),(d),(e),(f),(g),(h))",
                $("(||,(||,(a),(b), (||,(g),(h))),(||,(c),(d)), (||,(e),(f)))").toString());

    }

    @Test
    public void testImplicationConjCommonSubterms() {
        assertEquals("((b&&c)==>d)",
                $("((&&, a, b, c) ==> (&&, a, d))").toString());
        assertEquals("(d==>(b&&c))",
                $("((&&, a, d) ==> (&&, a, b, c))").toString());
        assertInvalid("((&&, a, b, c) ==> (&&, a, b))");
        assertInvalid("((&&, a, b) ==> (&&, a, b, c))");
        assertInvalid("((&&, a, b, c) ==> a)");
        assertInvalid("(a ==> (&&, a, b, c))");
    }

    @Test public void testConegatedConjunctionTerms1() {
        assertEquals($("(&&,(x),--(y),--(z))"), $("((x) && --((y) && (z)))"));
    }
    @Test public void testConegatedConjunctionTerms1not() {
        //dont unwrap due to different 'dt'
        assertEquals("((--,((y) &&+0 (z)))&&(x))", $("((x)&&--((y) &&+0 (z)))").toString());

        assertEquals("((--,((y)&&(z))) &&+0 (x))", $("((x) &&+0 --((y) && (z)))").toString());
    }

    @Test public void testConegatedConjunctionTerms2() {
        assertInvalid("(#1 && --(#1&&(robin-->swimmer)))");
    }

    @Test
    public void testDemorgan1() {
        //https://en.wikipedia.org/wiki/De_Morgan%27s_laws


        // \neg(P\and Q)\iff(\neg P)\or(\neg Q)
        assertEquals("(--,((p)&&(q)))",
                $("(--(p) || --(q))").toString());
    }

    @Ignore
    @Test
    public void testDemorgan2() {

        // \neg(P\or Q)\iff(\neg P)\and(\neg Q),
        assertEquals("(--,((p)||(q)))",
                $("(--(p) && --(q))").toString());
    }

    @Test
    public void testCoNegatedJunction() {
        //the conegation cancels itself out
        assertInvalid(
                "(&&,x,a:b,(--,a:b))");

        assertInvalid(
                "(&&,x,y,a:b,(--,a:b))");
    }


    @Test
    public void testCoNegatedJunction2() {
        assertEquals(False,
                $("(&&,x, a:b,(--,a:b))"));

        assertEquals(True,
                $("(||,x,a:b,(--,a:b))"));

        assertEquals(True,
                $("(||,x,y,a:b,(--,a:b))"));
    }

    @Test
    public void testFilterCommutedWithCoNegatedSubterms() {
        //any commutive terms with both a subterm and its negative are invalid


        assertValidTermValidConceptInvalidTaskContent(() -> $("((--,(a1)) && (a1))"));
        assertValidTermValidConceptInvalidTaskContent(() -> $("((--,(a1)) &&+0 (a1))"));
        assertValid($("((--,(a1)) &&+1 (a1))"));

        assertInvalid("((--,(a1)) || (a1))");
    }

    @Test
    public void testFilterCoNegatedStatements() {
        assertEquals(Term.False, $("((--,(a1)) <-> (a1))"));
        assertEquals(Term.False, $("((--,(a1)) --> (a1))"));
    }


    @Test
    public void testCoNegatedImplication() {

        assertValidTermValidConceptInvalidTaskContent(() -> $("((--,(a)) ==> (a))"));
        assertValidTermValidConceptInvalidTaskContent(() -> $("((--,(a)) ==>+0 (a))"));
        assertValid($("((--,(a)) ==>+1 (a))"));

    }


    @Test
    public void testCoNegatedDifference() {
        //..
    }

    @Test
    public void testCoNegatedIntersection() {
        //..
    }

    @Test public void testSimEquivOfAbsoluteTrueFalse() {
        assertEquals(True, $.sim(True,True));
        assertEquals(True, $.sim(False,False));
        assertEquals(False, $.sim(True,False));
        assertEquals(True, $.sim($.neg(True),False));
    }

    /**
     * conjunction and disjunction subterms which can occurr as a result
     * of variable substitution, etc which don't necessarily affect
     * the resulting truth of the compound although if the statements
     * were alone they would not form valid tasks themselves
     */
    @Test
    public void testSingularStatementsInConjunction() {
        assertEquals($("(&&,c:d,e:f)"), $("(&&,(a<->a),c:d,e:f)"));
        assertEquals($("(&&,c:d,e:f)"), $("(&&,(a<=>a),c:d,e:f)"));
        assertEquals($("(&&,c:d,e:f)"), $("(&&,(a-->a),c:d,e:f)"));
        assertEquals($("(&&,c:d,e:f)"), $("(&&,(a==>a),c:d,e:f)"));
        assertEquals(Term.False, $("(&&,(--,(a==>a)),c:d,e:f)"));

    }

    @Test
    public void testSingularStatementsInDisjunction() {

        assertInvalid("(||,(a<->a),c:d,e:f)"); //null, singular true
    }

    @Test
    public void testSingularStatementsInDisjunction2() {
        assertEquals($("x:y"), $("(&&,(||,(a<->a),c:d,e:f),x:y)")); //double fall-thru
        assertEquals(False, $("(&&,(--,(||,(a<->a),c:d,e:f)),x:y)")); //double fall-thru

//        assertEquals($("(||,c:d,e:f)"), $("(||,(a<=>a),c:d,e:f)"));
//        assertEquals($("(||,c:d,e:f)"), $("(||,(a-->a),c:d,e:f)"));
//        assertEquals($("(||,c:d,e:f)"), $("(||,(a==>a),c:d,e:f)"));
//        assertEquals($("(||,c:d,e:f)"), $("(||,(--,(a==>a)),c:d,e:f)")); //VALID

    }

    @Test
    public void testOneArgIntersection() {
        assertEquals("(x)", $("(|,(x))").toString());
        assertEquals("(x)", $("(|,(x),(x))").toString());
        assertEquals("(x)", $("(&,(x))").toString());
        assertEquals("(x)", $("(&,(x),(x))").toString());
    }

    @Test
    public void testCoNegatedIntersectionAndDiffs() {
        assertInvalid("(|,(x),(--,(x))");
        assertInvalid("(&,(x),(--,(x))");
        assertInvalid("(-,(x),(--,(x))");
        assertInvalid("(~,(x),(--,(x))");
        assertInvalid("(-,(x),(x))");
    }

    @Test
    public void testGroupNonDTemporalParallelComponents() {
        //$.76;.45;.70$ ( &&+0 ,(ball_left),(ball_right),((--,(ball_left)) &&-270 (ball_right))). :3537: %.64;.15%
        //$.39;.44;.70$ (((--,(ball_left)) &&-233 (ball_right)) &&-1 ((ball_left) &&+0 (ball_right))). :3243: %.53;.23%
        assertEquals("(((ball_left) &&+0 (ball_right)) &&+0 ((ball_right) &&+270 (--,(ball_left))))",

                //HACK: this narsese parser isnt implemented yet:
                //$("( &&+0 ,(ball_left),(ball_right),((--,(ball_left)) &&-270 (ball_right)))")

                $.parallel($("(ball_left)"), $("(ball_right)"), $("((--,(ball_left)) &&-270 (ball_right)))"))
                        .toString());

    }

    @Test
    public void testReducibleImpl() {
        assertInvalid("((--,(x)) ==>+0 ((--,(y)) &&+0 (--,(x))))");
        assertInvalid("((--,(x)) ==> ((--,(y)) && (--,(x))))");
    }

    @Test
    public void testConjunctiveCoNegationAcrossImpl() {
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

    @Test public void testImageUnwrap0() {
        assertEquals("(a,b)",
           $.p(imageUnwrap($("(\\,n,_,b)"), $("a"))).toString());
    }
    @Test public void testImageUnwrap1() {
        assertEquals("(a,b)",
            $.p(imageUnwrap($("(\\,n,a,_)"), $("b"))).toString());
    }
    @Test public void testImageUnwrap2() {
        assertEquals("(a,b)",
            $.p(imageUnwrap($("(/,n,_,b)"), $("a"))).toString());
    }
    @Test public void testImageUnwrap3() {
        assertEquals(     "(a,b)",
           $.p(imageUnwrap($("(/,n,a,_)"), $("b"))).toString());
    }

    @Test public void testImageInSubtermsProductNormalFormIntensional() {

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

    @Test public void testImageInSubtermsProductNormalFormExtensional() {
        //<(acid,base) --> reaction> //en("An acid and a base can have a reaction.");
        //  <base --> (/,reaction,acid,_)> //en("A base is something that has a reaction with an acid.");
        //  <acid --> (/,reaction,_,base)> //en("Acid can react with base.");
        assertEquals(
                  "{r(a,b)}",
                $("{(b-->(/,r,a,_))}").toString());
        assertEquals(
                  "(--,r(a,b))",
                $("(--,(a-->(/,r,_,b)))").toString());

    }
    @Test public void testNegatedImageInSubtermsProductNormalForm() {
        assertEquals(
                  "{(--,r(a,b)),(z-->(x,y))}",
                $("{ (--,(b-->(/,r,a,_))), ((\\,z,x,_)-->y) }").toString());

    }

}

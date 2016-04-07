package nars.term;

import nars.$;
import nars.Op;
import nars.io.NarseseTest;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.Nullable;
import org.junit.Ignore;
import org.junit.Test;

import static nars.$.*;
import static nars.Op.CONJUNCTION;
import static nars.io.NarseseTest.assertParseException;
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
        assertEquals("(&,P,Q,R)", esect(r, esect(p, q)).toString());
        assertEquals("(&,P,Q,R)", $("(&,R,(&,P,Q))").toString());
    }
    @Test public void testIntersectExtReduction2() {
        // (&,(&,P,Q),(&,R,S)) = (&,P,Q,R,S)
        assertEquals("(&,P,Q,R,S)", esect(esect(p, q), esect(r, s)).toString());
        assertEquals("(&,P,Q,R,S)", $("(&,(&,P,Q),(&,R,S))").toString());
    }
    @Test public void testIntersectExtReduction3() {
        // (&,(&,P,Q),(&,R,S)) = (&,P,Q,R,S)
        assertEquals("(&,P,Q,R,S,T,U)", $("(&,(&,P,Q),(&,R,S), (&,T,U))").toString());
    }
    @Test public void testIntersectExtReduction2_1() {
        // (&,R,(&,P,Q)) = (&,P,Q,R)
        assertEquals("(&,P,Q,R)", $("(&,R,(&,P,Q))").toString());
    }
    @Test public void testIntersectExtReduction4() {
        //UNION if (term1.op(Op.SET_INT) && term2.op(Op.SET_INT)) {
        assertEquals("{P,Q,R,S}", esect(sete(p, q), sete(r, s)).toString());
        assertEquals("{P,Q,R,S}", $("(&,{P,Q},{R,S})").toString());
        assertEquals(null /* emptyset */, esect(seti(p, q), seti(r, s)));

    }

    @Test public void testIntersectIntReduction1() {
        // (|,R,(|,P,Q)) = (|,P,Q,R)
        assertEquals("(|,P,Q,R)", isect(r, isect(p, q)).toString());
        assertEquals("(|,P,Q,R)", $("(|,R,(|,P,Q))").toString());
    }
    @Test public void testIntersectIntReduction2() {
        // (|,(|,P,Q),(|,R,S)) = (|,P,Q,R,S)
        assertEquals("(|,P,Q,R,S)", isect(isect(p, q), isect(r, s)).toString());
        assertEquals("(|,P,Q,R,S)", $("(|,(|,P,Q),(|,R,S))").toString());
    }
    @Test public void testIntersectIntReduction3() {
        // (|,R,(|,P,Q)) = (|,P,Q,R)
        assertEquals("(|,P,Q,R)", $("(|,R,(|,P,Q))").toString());
    }
    @Test public void testIntersectIntReduction4() {
        //UNION if (term1.op(Op.SET_INT) || term2.op(Op.SET_INT)) {
        assertEquals("[P,Q,R,S]", isect(seti(p, q), seti(r, s)).toString());
        assertEquals("[P,Q,R,S]", $("(|,[P,Q],[R,S])").toString());

    }
    @Test public void testIntersectIntReductionToZero() {
        assertParseException("(|,{P,Q},{R,S})");
    }

    @Test public void testIntersectIntReduction_to_one() {
        assertEquals("(robin-->bird)", $("<robin-->(|,bird)>").toString());
        assertEquals("(robin-->bird)", $("<(|,robin)-->(|,bird)>").toString());
    }


    @Test public void testInvalidEquivalences() {
        assertEquals("(P<=>Q)", equiv(p, q).toString() );

        assertNull(equiv( impl(p, q), r) );
        assertNull(equiv( equiv(p, q), r) );
        assertParseException("<<a <=> b> <=> c>");
    }

    @Test public void testReducedAndInvalidImplications1() {
        assertParseException("<<P<=>Q> ==> R>");
    }
    @Test public void testReducedAndInvalidImplications5() {
        assertParseException("<<P==>Q> ==> R>");
    }
    @Test public void testReducedAndInvalidImplications6() {
        assertParseException("<R ==> <P<=>Q>>");
    }
    @Test public void testReducedAndInvalidImplications2() {
        assertEquals("((P&&R)==>Q)", $("<R==><P==>Q>>").toString());
    }
    @Test public void testReducedAndInvalidImplications3() {
        assertParseException("<R==><P==>R>>");
    }
    @Test public void testReducedAndInvalidImplications4() {
        assertEquals("(R==>P)", $("<R==><R==>P>>").toString());
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

    @Test public void testDisjunctEqual() {
        assertEquals(p, disj(p, p));
    }
    @Test public void testConjunctionEqual() {
        assertEquals(p, $.conj(p, p));
    }
    @Test public void testConjunctionNormal() {
        Term x = $.$("(&&, <#1 --> lock>, <#1 --> (/, open, #2, _)>, <#2 --> key>)");
        assertEquals(3, x.size());
        assertEquals(CONJUNCTION, x.op());
    }

    @Test public void testIntExtEqual() {
        assertEquals(p, $.esect(p, p));
        assertEquals(p, isect(p, p));
    }

    @Test public void testDiffIntEqual() {

        assertEquals(null, diffInt(p, p));
    }
    @Test public void testDiffExtEqual() {

        assertEquals(null, diffExt(p, p));
    }
    @Test public void testDifferenceSorted() {
//        assertArrayEquals(
//            new Term[] { r, s },
//            Terms.toArray(TermContainer.differenceSorted(sete(r, p, q, s), sete(p, q)))
//        );
        //check consistency with differenceSorted
        assertArrayEquals(
            new Term[] { r, s },
            ((Compound)TermContainer.difference(Op.SET_EXT, sete(r, p, q, s), sete(p, q))).terms()
        );
    }
    @Test public void testDifferenceSortedEmpty() {
//        assertArrayEquals(
//                new Term[] { },
//                Terms.toArray(TermContainer.differenceSorted(sete(p, q), sete(p, q)))
//        );
        //check consistency with differenceSorted
        assertEquals(
            null,
            TermContainer.difference(Op.SET_EXT, sete(p, q), sete(p, q))
        );
    }


    @Test public void testDifference() {
        /*tester.believe("<planetX --> {Mars,Pluto,Venus}>",0.9f,0.9f); //.en("PlanetX is Mars, Pluto, or Venus.");
        tester.believe("<planetX --> {Pluto,Saturn}>", 0.1f, 0.9f); //.en("PlanetX is probably neither Pluto nor Saturn.");
        tester.mustBelieve(cycles, "<planetX --> {Mars,Venus}>", 0.81f ,0.81f); //.en("PlanetX is either Mars or Venus.");*/


        assertEquals(
                $("{Mars,Venus}"),
                TermContainer.difference(
                        Op.SET_EXT,
                        $("{Mars,Pluto,Venus}"),
                        $("{Pluto,Saturn}")
                )
        );
        assertEquals(
                $("{Saturn}"),
                TermContainer.difference(
                        Op.SET_EXT,
                        $("{Pluto,Saturn}"),
                        $("{Mars,Pluto,Venus}")
                )
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

        Term d = diffInt(
                seti($("a"), $("b"), $("c")),
                seti($("d"), $("b")));
        assertEquals(Op.SET_INT, d.op());
        assertEquals(d.toString(), 2, d.size());
        assertEquals("[a,c]", d.toString());
    }

    @Test
    public void testDifferenceImmediate2() {


        Compound a = $.sete($("a"), $("b"), $("c"));
        Compound b = $.sete($("d"), $("b"));
        Term d = diffExt(a, b);
        assertEquals(Op.SET_EXT, d.op());
        assertEquals(d.toString(), 2, d.size());
        assertEquals("{a,c}", d.toString());

    }

    @Test
    public void testDisjunctionReduction() {
        assertEquals("(||,a,b,c,d)",
                $("(||,(||,a,b),(||,c,d))").toString());
        assertEquals("(||,b,c,d)",
                $("(||,b,(||,c,d))").toString());
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

    @Ignore @Test
    public void testTemporalNTermEquivalenceParallel() {
        //+0 is the only case in which temporal && can have arity>2
        assertEquals("(<=>+0, a, b, c)", $("( a <=>+0 (b <=>+0 c) )").toString());
    }


    @Test
    public void testMultireduction() {
        //TODO probably works
    }

    @Test public void testConjunctionMultipleAndEmbedded() {

        assertEquals("(&&,a,b,c,d)",
                $("(&&,(&&,a,b),(&&,c,d))").toString());
        assertEquals("(&&,a,b,c,d,e,f)",
                $("(&&,(&&,a,b),(&&,c,d), (&&, e, f))").toString());
        assertEquals("(&&,a,b,c,d,e,f,g,h)",
                $("(&&,(&&,a,b, (&&, g, h)),(&&,c,d), (&&, e, f))").toString());
    }

    @Test public void testConjunctionEquality() {

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

    @Test public void testImplicationInequality() {

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

    @Test public void testDisjunctionMultipleAndEmbedded() {

        assertEquals("(||,a,b,c,d)",
                $("(||,(||,a,b),(||,c,d))").toString());
        assertEquals("(||,a,b,c,d,e,f)",
                $("(||,(||,a,b),(||,c,d), (||, e, f))").toString());
        assertEquals("(||,a,b,c,d,e,f,g,h)",
                $("(||,(||,a,b, (||, g, h)),(||,c,d), (||, e, f))").toString());

    }

    @Test public void testImplicationConjCommonSubterms() {
        assertEquals("((b&&c)==>d)",
                $("((&&, a, b, c) ==> (&&, a, d))").toString());
        assertEquals("(d==>(b&&c))",
                $("((&&, a, d) ==> (&&, a, b, c))").toString());
        assertParseException("((&&, a, b, c) ==> (&&, a, b))");
        assertParseException("((&&, a, b) ==> (&&, a, b, c))");
        assertParseException("((&&, a, b, c) ==> a)");
        assertParseException("(a ==> (&&, a, b, c))");
    }
}

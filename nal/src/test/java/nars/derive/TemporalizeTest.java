package nars.derive;

import jcog.list.FasterList;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.derive.time.*;
import nars.index.term.TermContext;
import nars.term.Term;
import org.eclipse.collections.api.tuple.primitive.ObjectLongPair;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

import static nars.$.$;
import static nars.$.the;
import static nars.time.Tense.ETERNAL;
import static org.junit.Assert.*;

public class TemporalizeTest {

    final NAR n = NARS.shell();

    @Test
    public void testAbsoluteRanking() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();


        //eternal should be ranked lower than non-eternal
        Term x = the("x");
        AbsoluteEvent ete = t.absolute(x, ETERNAL, ETERNAL);
        AbsoluteEvent tmp = t.absolute(x, 0, 0);
        assertEquals(+1, ete.compareTo(tmp));
        assertEquals(-1, tmp.compareTo(ete));
        assertEquals(0, ete.compareTo(ete));
        assertEquals(0, tmp.compareTo(tmp));
        FasterList<AbsoluteEvent> l = new FasterList<>();
        l.add(ete);
        l.add(tmp);
        l.sortThis();
        assertEquals("[x@0, x@ETE]", l.toString());
    }

    @Test
    public void testRelativeRanking() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();

        Term x = the("x");
        AbsoluteEvent xa = t.absolute(x, ETERNAL, ETERNAL);
        t.constraints.put(x, new TreeSet(List.of(xa)));

        Term y = the("y");
        AbsoluteEvent ya = t.absolute(y, 0, 0);
        t.constraints.put(y, new TreeSet(List.of(ya)));

        Term z = the("z");
        RelativeEvent zx = t.newRelative(z, x, 0);
        RelativeEvent zy = t.newRelative(z, y, 0);
        assertEquals(0, zx.compareTo(zx));
        assertEquals(0, zy.compareTo(zy));

        assertEquals(+1, zx.compareTo(zy));
        assertEquals(-zy.compareTo(zx), zx.compareTo(zy));


        FasterList<RelativeEvent> l = new FasterList<>();
        l.add(zx);
        l.add(zy);
        l.sortThis();
        assertEquals("[z@0->y, z@0->x]", l.toString()); //y first since it is non-eternal
    }

    @Test
    public void testAbsoluteRelativeRanking() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();

        //eternal should be ranked lower than non-eternal
        Term x = the("x");
        Term y = the("y");
        Event yTmp0 = t.absolute(y, 0, 0);
        Event xEte = t.absolute(x, ETERNAL, ETERNAL);
        Event xRelY = t.newRelative(x, y, 0);
        assertEquals(+1, xEte.compareTo(xRelY));
//        assertEquals(-1, tmp.compareTo(ete));
//        assertEquals(0, ete.compareTo(ete));
//        assertEquals(0, tmp.compareTo(tmp));
        FasterList<Event> l = new FasterList<>();
        l.add(xEte);
        l.add(xRelY);
        l.sortThis();
        assertEquals("[x@0->y, x@ETE]", l.toString()); //ETE must be dead last
    }


    @Test
    public void testSolveTermSimple() throws Narsese.NarseseException {

        Temporalize t = new Temporalize();
        t.knowTerm($("a"), 1);
        t.knowTerm($("b"), 3);
        assertEquals("(a &&+2 b)@[1..3]", t.solve($("(a &&+- b)")).toString());
        assertEquals("(a &&+2 b)@[1..3]", t.solve($("(b &&+- a)")).toString());

    }

    @Test
    public void testSolveIndirect() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.knowTerm($("(a ==>+1 c)"), ETERNAL);
        t.knowTerm($("a"), 0);
        Event s = t.solve($("c"));
        assertNotNull(s);
        assertEquals("c@1->a", s.toString());
    }

    @Test
    public void testSolveIndirect2() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.knowTerm($("(b ==>+1 c)"), ETERNAL);
        t.knowTerm($("(a ==>+1 b)"), 0);
        t.knowTerm($("c"), 2);

        Map<Term, Time> h = new HashMap();
        Event s = t.solve($("a"), h);
        assertNotNull(s);
        assertEquals("a@-1->b", s.toString());
        assertEquals(0, s.start(h).abs());
    }

    @Test
    public void testSolveEternalButRelative() throws Narsese.NarseseException {
        /*
                .believe("(x ==>+2 y)")
                .believe("(y ==>+3 z)")
                .mustBelieve(cycles, "(x ==>+5 z)", 1.00f, 0.81f);
                */
        Temporalize t = new Temporalize();
        t.knowTerm($("(x ==>+2 y)"), ETERNAL);
        t.knowTerm($("(y ==>+3 z)"), ETERNAL);

        Event s = t.solve($("(x ==>+- z)"));
        assertNotNull(s);
        assertEquals("(x ==>+5 z)@ETE", s.toString());
    }

    @Test
    public void testSolveEternalButRelative3() throws Narsese.NarseseException {
        /*
        RIGHT: (z ==>+1 x).
        WRONG:
        $1.0 (z ==>-2 x). %1.0;.45% {18: 1;2} (((%1==>%2),(%1==>%3),neqCom(%2,%3),notImpl(%3)),((%3 ==>+- %2),((Abduction-->Belief))))
            $.50 (y ==>+3 x). %1.0;.90% {0: 1}
            $.50 (y ==>+2 z). %1.0;.90% {0: 2}
         */
        Temporalize t = new Temporalize();
        t.knowTerm($("(y ==>+3 x)"), ETERNAL);
        t.knowTerm($("(y ==>+2 z)"), ETERNAL);

        Event s = t.solve($("(z ==>+- x)"));
        assertNotNull(s);
        assertEquals("(z ==>+1 x)@ETE", s.toString());
    }


    @Test
    public void testSolveRecursiveConjDecomposition() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.knowTerm($("((x &&+1 y) &&+1 z)"), 0);

        Map<Term, Time> h = new HashMap();
        h.clear();
        assertEquals(0, t.solve($("x"), h).start(h).abs());
        h.clear();
        assertEquals(1, t.solve($("y"), h).start(h).abs());
        h.clear();
        assertEquals(2, t.solve($("z"), h).start(h).abs());
    }

    @Test
    public void testSolveRecursiveConjDecomposition2() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.knowTerm($("((x &&+1 y) &&+1 z)"), 0);

        //System.out.println(t);
        //System.out.println("x: " + t.constraints.get($("x")));
        //System.out.println("z: " + t.constraints.get($("z")));

        assertEquals("(x &&+2 z)@[0..2]", t.solve($("(x &&+- z)")).toString());
        assertEquals("(x &&+1 y)@[0..1]", t.solve($("(x &&+- y)")).toString());
        assertEquals("(y &&+1 z)@[1..2]", t.solve($("(y &&+- z)")).toString());

        assertEquals("(x ==>+2 z)@0", t.solve($("(x ==>+- z)")).toString());
        assertEquals("(x ==>+1 y)@0", t.solve($("(x ==>+- y)")).toString());
        assertEquals("(y ==>+1 z)@1", t.solve($("(y ==>+- z)")).toString());
    }

    /**
     * tests temporalization of pure events which overlap, or are separated by a distance below a proximal threshold (see Param.java)
     */
    @Test
    public void testStatementEventsBothTemporal() throws Narsese.NarseseException {
//              .input(new NALTask($.$("(a-->b)"), GOAL, $.t(1f, 0.9f), 5, 10, 20, new long[]{100}).pri(0.5f))
//              .input(new NALTask($.$("(c-->b)"), BELIEF, $.t(1f, 0.9f), 4, 5, 25, new long[]{101}).pri(0.5f))
//                   .mustDesire(cycles, "(a-->c)", 1f, 0.4f, 10, 20)

        Temporalize t = new Temporalize();
        t.knowTerm($("(a-->b)"), 10, 20); //these two overlap, so there should be a derivation
        t.knowTerm($("(c-->b)"), 5, 25);

        Term st = $("(a-->c)");
        Event solution = t.solve(st);
        assertNotNull(solution);
        assertEquals("(a-->c)@[10..20]", solution.toString());
        assertNull("d not covered by known events", t.solve($("(a-->d)")));
    }

    /**
     * tests temporalization of pure events which overlap, or are separated by a distance below a proximal threshold (see Param.java)
     */
    @Test
    public void testStatementEventsNearlyOverlappingTemporal() throws Narsese.NarseseException {
//              .input(new NALTask($.$("(a-->b)"), GOAL, $.t(1f, 0.9f), 5, 10, 20, new long[]{100}).pri(0.5f))
//              .input(new NALTask($.$("(c-->b)"), BELIEF, $.t(1f, 0.9f), 4, 5, 25, new long[]{101}).pri(0.5f))
//                   .mustDesire(cycles, "(a-->c)", 1f, 0.4f, 10, 20)

        Temporalize t = new Temporalize();
        t.dur = 10;
        t.knowTerm($("(a-->b)"), 10, 15); //these two overlap, so there should be a derivation
        t.knowTerm($("(c-->b)"), 1, 5);

        Event solution = t.solve($("(a-->c)"));
        assertEquals("(a-->c)@7", solution.toString());
    }

    /**
     * tests temporalization of pure events which overlap, or are separated by a distance below a proximal threshold (see Param.java)
     */
    @Test
    public void testStatementEventsNonOverlappingTemporal() throws Narsese.NarseseException {
//              .input(new NALTask($.$("(a-->b)"), GOAL, $.t(1f, 0.9f), 5, 10, 20, new long[]{100}).pri(0.5f))
//              .input(new NALTask($.$("(c-->b)"), BELIEF, $.t(1f, 0.9f), 4, 5, 25, new long[]{101}).pri(0.5f))
//                   .mustDesire(cycles, "(a-->c)", 1f, 0.4f, 10, 20)

        Temporalize t = new Temporalize();
        t.knowTerm($("(a-->b)"), 10, 15); //these two overlap, so there should be a derivation
        t.knowTerm($("(c-->b)"), 1, 5);

        Event solution = t.solve($("(a-->c)"));
        assertNull(solution);
    }

    @Test
    public void testStatementEventsOneEternal() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.knowTerm($("(a-->b)"), ETERNAL); //these two overlap, so there should be a derivation
        t.knowTerm($("(c-->b)"), 5, 25);

        Event solution = t.solve($("(a-->c)"));
        assertNotNull(solution);
        assertEquals("(a-->c)@[5..25]", solution.toString());
    }

    @Test
    public void testSolveEternalButRelative2() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();

        //  b ==>+10 c ==>+20 e

        t.knowTerm($("(b ==>+10 c)"), ETERNAL);
        t.knowTerm($("(e ==>-20 c)"), ETERNAL);

//        System.out.println( Joiner.on('\n').join( t.constraints.entrySet() ) );

        HashMap h = new HashMap();
        Event s = t.solve($("(b ==>+- e)"), h);

//        System.out.println();
//        System.out.println( Joiner.on('\n').join( h.entrySet() ) );

        assertNotNull(s);
        assertEquals("(b ==>+30 e)@ETE", s.toString());
    }

    @Test
    public void testImplDT() throws Narsese.NarseseException {
        /*
            $1.0 ((d-->c) ==>-3 (a-->b)). 2 %1.0;.45% {6: 1;2} ((%1,%2,time(raw),task(positive),task("."),time(dtEvents),notImpl(%1),notImpl(%2)),((%1 ==>+- %2),((Induction-->Belief))))
              $.50 (d-->c). 5 %1.0;.90% {5: 2}
              $.50 (a-->b). 2 %1.0;.90% {2: 1}
        */


        Temporalize t = new Temporalize();
        t.knowTerm($("(d-->c)"), 5);
        t.knowTerm($("(a-->b)"), 2);

        Event solution = t.solve($("((d-->c) ==>+- (a-->b))"));
        assertNotNull(solution);
        assertEquals("((d-->c) ==>-3 (a-->b))@5", solution.toString());
    }

    @Test
    public void testImplConjDepvar() throws Narsese.NarseseException {

        Temporalize t = new Temporalize();
        t.knowTerm($("a"), 1);
        t.knowTerm($("((a &&+5 b) ==>+5 #1)"), 1);

        {
            Event solution = t.solve($("(a &&+- b)"));
            assertNotNull(solution);
            assertEquals("(a &&+5 b)@[1..6]", solution.toString());
        }
        {
            Event solution = t.solve($("(a &&+- #1)"));
            assertNotNull(solution);
            assertEquals("(a &&+10 #1)@[1..11]", solution.toString());
        }
    }

    @Test
    public void testSolveConjSequenceMerge() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();

        String A = "((a &&+3 c) &&+4 e)";
        t.knowTerm($(A), 1);
        String B = "b";
        t.knowTerm($(B), 4);

//        System.out.println( Joiner.on('\n').join( t.constraints.entrySet() ) );

        HashMap h = new HashMap();
        Event s = t.solve($("(" + A + " &&+- " + B + ")"), h);

//        System.out.println();
//        System.out.println( Joiner.on('\n').join( h.entrySet() ) );

        assertNotNull(s);
        assertEquals("((a &&+3 (b&|c)) &&+4 e)@[1..8]", s.toString());
    }

    @Test
    public void testRecursiveSolution1a() throws Narsese.NarseseException {
        /*
                 believe("(x ==>+5 z)")
                .believe("(y ==>+3 z)")
                .mustBelieve(cycles, "( (x &&+2 y) ==>+3 z)", 1f, 0.81f)
         */
        Temporalize t = new Temporalize();
        t.knowTerm($("(x ==>+5 z)"), ETERNAL);
        t.knowTerm($("(y ==>+3 z)"), ETERNAL);

        //System.out.println(Joiner.on('\n').join(t.constraints.entrySet()));

        {
            HashMap h = new HashMap();
            Event s = t.solve($("(x &&+- y)"), h);
            assertNotNull(s);
            assertEquals("(x &&+2 y)@ETE", s.toString());
        }

    }

    @Test
    public void testRecursiveSolution1b() throws Narsese.NarseseException {

        for (String op : new String[]{"==>", "&&"}) {
            Temporalize t = new Temporalize();
            t.knowTerm($("(x ==>+5 z)"), ETERNAL);
            t.knowTerm($("(y ==>+3 z)"), ETERNAL);

            HashMap h = new HashMap();
            Event s = t.solve($("((x &&+- y) " + op + "+- z)"), h);

//            System.out.println();
//            System.out.println(Joiner.on('\n').join(h.entrySet()));

            int dt = 3;
            String pattern = "((x &&+2 y) " + op + "+" + dt + " z)@ETE";

            assertNotNull(s);
            assertTrue(s.toString().startsWith(pattern));
        }

    }

//    @Test
//    public void testImplToEquiCircularity() throws Narsese.NarseseException {
//        Temporalize t = new Temporalize();
//        t.knowTerm($.$("(x ==>+5 y)"), ETERNAL);
//        t.knowTerm($.$("(y ==>-5 x)"), ETERNAL);
//        assertEquals("(x <=>+5 y)@ETE", t.solve($.$("(x <=>+- y)")).toString());
//
//        //    @Test public void testImplToEquiCircularityAvg() throws Narsese.NarseseException {
//        //        Temporalize t = new Temporalize();
//        //        t.knowTerm($.$("(x ==>+6 y)"), ETERNAL);
//        //        t.knowTerm($.$("(y ==>-4 x)"), ETERNAL);
//        //        assertEquals("(x <=>+5 y)@ETE", t.solve($.$("(x <=>+- y)")).toString());
//        //    }
//
//    }


    @Test
    public void testImplLinked() throws Narsese.NarseseException {
        /*
        ( $,TestNAR ): "ERR	(z ==>+1 x). %(1.0,1.0);(.45,.46)%  creation: (0,400)".
        ( $,TestNAR ): "SIM
        $1.0 (z ==>-2 x). %1.0;.45% {9: 1;2} (((%1==>%2),(%3==>%2),neqCom(%1,%3),notImpl(%3)),((%3 ==>+- %1),((Induction-->Belief))))
            $.50 (x ==>+2 y). %1.0;.90% {0: 1}
            $.50 (z ==>+3 y). %1.0;.90% {0: 2}
         */
        Temporalize t = new Temporalize();
        t.knowTerm($("(x ==>+2 y)"), ETERNAL);
        t.knowTerm($("(z ==>+3 y)"), ETERNAL);
        Event s = t.solve($("(z ==>+- x)"));
        assertNotNull(s);
        assertEquals("(z ==>+1 x)@ETE", s.toString());
    }

    @Test
    public void testConjLinked() throws Narsese.NarseseException {
// WRONG:
//        $.31 ((b &&+5 c) ==>+5 (a &&+5 b)). 6 %1.0;.45% {7: 1;2} ((%1,%2,time(raw),belief(positive),task("."),time(dtEvents),notImpl(%1),notImpl(%2)),((%2 ==>+- %1),((Abduction-->Belief))))
//          $.50 (a &&+5 b). 1⋈6 %1.0;.90% {1: 1}
//          $.50 (b &&+5 c). 6⋈11 %1.0;.90% {6: 2}
        Temporalize t = new Temporalize();
        t.knowTerm($("(a &&+5 b)"), 1);
        t.knowTerm($("(b &&+5 c)"), 6);
        assertEquals("(a &&+5 b)@[1..6]", t.solve($("(a &&+- b)")).toString());
        assertEquals("((a &&+5 b) &&+5 c)@[1..11]", t.solve($("((a &&+- b) &&+- c)")).toString());
        assertEquals("((a &&+5 b)=|>(b &&+5 c))@1", t.solve($("((a &&+5 b) ==>+- (b &&+5 c))")).toString());
        assertEquals("((b &&+5 c) ==>-10 (a &&+5 b))@6", t.solve($("((b &&+5 c) ==>+- (a &&+5 b))")).toString());

    }

    @Test
    public void testConjLinked2() throws Narsese.NarseseException {
// WRONG:
//    $.27 ((a &&+5 b) &&+5 (c &&+5 (c&|d))). 1⋈16 %1.0;.73% {20: 1;2;3} ((%1,%2,task("."),time(raw),time(dtEvents),notImpl(%1),notImpl(%2)),((polarize(%1,task) &&+- polarize(%2,belief)),((IntersectionDepolarized-->Belief))))
//      $.50 (c &&+5 d). 11⋈16 %1.0;.90% {11: 3}
//      $.31 ((a &&+5 b) &&+5 c). 1⋈11 %1.0;.81% {7: 1;2} ((%1,%2,task("."),time(raw),time(dtEvents),notImpl(%1),notImpl(%2)),((polarize(%1,task) &&+- polarize(%2,belief)),((IntersectionDepolarized-->Belief))))

        Temporalize t = new Temporalize();
        t.knowTerm($("(c &&+5 d)"), 11);
        t.knowTerm($("((a &&+5 b) &&+5 c)"), 1);
        assertEquals("((a &&+5 b) &&+5 (c &&+5 d))@[1..16]", t.solve($("((a &&+- b) &&+- (c &&+- d))")).toString());
    }

    @Test
    public void testImplFromConj() throws Narsese.NarseseException {
        //WRONG:    $.40 ((c) ==>-2 ((a) &&+1 (b))). 5 %1.0;.42% {9: 1;2;3} ((%1,%2,time(raw),task(positive),task("."),time(dtEvents),notImpl(%1),notImpl(%2)),((%1 ==>+- %2),((Induction-->Belief))))
        //               $.50 (c). 5 %1.0;.90% {5: 3}
        //               $1.0 ((a) &&+1 (b)). 1⋈2 %1.0;.81% {3: 1;2} ((%1,%2,task("."),time(raw),time(dtEvents),notImpl(%1),notImpl(%2)),((polarize(%1,task) &&+- polarize(%2,belief)),((IntersectionDepolarized-->Belief))))

        Temporalize t = new Temporalize();
        t.knowTerm($("c"), 5);
        t.knowTerm($("(a &&+1 b)"), 1, 2);

        Term P = $("(c ==>+- (a &&+- b))");

        Event s = t.solve(P);
        assertEquals("(c ==>-4 (a &&+1 b))@5", s.toString());
    }

    @Test
    public void testConjLinked3() throws Narsese.NarseseException {
        /*
          instability:
$.72 (a &&+5 b). -4⋈1 %1.0;.30% {151: 1;2;;} ((%1,(%2==>%3),belief(positive),notImpl(%1),time(urgent)),(subIfUnifiesAny(%3,%2,%1),((DeductionRecursive-->Belief),(InductionRecursive-->Goal))))
    $.97 (b &&+5 c). 6⋈11 %1.0;.66% {14: 1;2;;} ((%1,%1,task("&&")),(dropAnyEvent(%1),((StructuralDeduction-->Belief),(StructuralDeduction-->Goal))))
      $1.0 ((a &&+5 b) &&+5 c). 1⋈11 %1.0;.73% {9: 1;2;;} ((%1,%2,task("."),time(raw),time(dtEvents),notImpl(%1),notImpl(%2)),((polarize(%1,task) &&+- polarize(%2,belief)),((IntersectionDepolarized-->Belief))))
        $.50 (b &&+5 c). 6⋈11 %1.0;.90% {6: 2}
        $1.0 a. 1 %1.0;.81% {2: 1;;} ((%1,%1,task("&&")),(dropAnyEvent(%1),((StructuralDeduction-->Belief),(StructuralDeduction-->Goal))))
          $.50 (a &&+5 b). 1⋈6 %1.0;.90% {1: 1}
      $1.0 ((a &&+5 b) &&+5 c). 1⋈11 %1.0;.82% {14: 1;2;;}
    $.63 ((b &&+5 c) ==>-10 (a &&+5 b)). 6 %1.0;.45% {7: 1;2} ((%1,%2,time(raw),task(positive),task("."),time(dtEvents),notImpl(%1),notImpl(%2)),((%1 ==>+- %2),((Induction-->Belief))))
      $.50 (b &&+5 c). 6⋈11 %1.0;.90% {6: 2}
      $.50 (a &&+5 b). 1⋈6 %1.0;.90% {1: 1}
         */
        Term A = $("((b &&+5 c) ==>-10 (a &&+5 b))");

        {
            Temporalize t = new Temporalize();
            t.knowTerm(A, 6);

            Map<Term, Time> h = new HashMap();
            Event s = t.solve(the("a"), h);
            //assertNull(s); //no way to solve for 'a' except relatively:
            assertEquals("a@-5->b", s.toString());

        }

        {
            Temporalize t = new Temporalize();
            t.knowTerm(A, 6);

            Term B = $("(b &&+5 c)");
            t.knowTerm(B, 6, 11);

            //System.out.println(t);

            Term p = $("(a &&+- b)");
            Map<Term, Time> h = new HashMap();
            Event s = t.solve(p, h);
            //System.out.println(h);
            assertNotNull(s);
            assertEquals("(a &&+5 b)@[1..6]", s.toString());
        }
    }

    @Test
    public void testConjInvert() throws Narsese.NarseseException {
        //WRONG:    $.66 (((--,a)&|b) &&+5 a). 1⋈6 %1.0;.73% {10: 1;2;;} ((%1,%1,task("&&")),(dropAnyEvent(%1),((StructuralDeduction-->Belief),(StructuralDeduction-->Goal))))
        //              $.63 ((a &&+5 ((--,a)&|b)) &&+5 (--,b)). 1⋈11 %1.0;.81% {6: 1;2} ((%1,%2,task("."),time(raw),time(dtEvents),notImpl(%1),notImpl(%2)),((polarize(%1,task) &&+- polarize(%2,belief)),((IntersectionDepolarized-->Belief))))
        Temporalize t = new Temporalize();

        Term x = $("((a &&+5 ((--,a)&|b)) &&+5 (--,b))");
        assertEquals(10, x.dtRange());
        t.knowTerm(x, 1, 11);

        Term a = $("(((--,a)&|b) &&+- a)");
        //System.out.println(a);
        //System.out.println(b);
        String r = "(a &&+5 ((--,a)&|b))@[1..6]";

        System.out.println(t);
        System.out.println(a);
        Event ta = t.solve(a);
        assertNotNull(ta);
        assertEquals(r, ta.toString());

        Term b = $("(a &&+- ((--,a)&|b))"); //check mirror
        assertEquals(r, t.solve(b).toString());

    }

    @Test
    public void testConjComplex() throws Narsese.NarseseException {
        String src = "((a &&+5 ((--,a)&|b)) &&+5 ((--,b) &&+5 (--,c)))";
        Term x = $(src);
        assertEquals(15, x.dtRange());

        TermContext n = NARS.shell();

        Set<String> result = new TreeSet();
        for (int i = 0; i < 100; i++) {

            Temporalize t = new Temporalize();

            t.knowTerm(x, 1, 16);
//            if (i == 0) {
//                //CHECK THE KNOWN PATTERN
//
//                for (String subterm : new String[]{"a", "b", "((--,b) &&+5 (--,c))"}) {
//                    FasterList<Event> cc = t.constraints.get($(subterm)); /// ? @[11..16]
//                    assertEquals(subterm + " has non-unique temporalizations: " + cc, 1, cc.count(y -> !(y instanceof RelativeEvent)));
//                }
//
//                System.out.println(t);
//                System.out.println();
//            }


            Term a = $($("dropAnyEvent(" + src + ")").eval(n).toString()
                    .replace("&&+5", "&&+-")
                    .replace("&&+10", "&&+-")
            );

            Event r = t.solve(a);
            if (r != null) {

                String xy = a + "\t" + r;

                assertTrue(xy, r.start(null).abs() >= 1);
                assertTrue(xy, r.end(null).abs() <= 16);


                result.add(xy);

            }

        }

        //result.forEach(System.out::println);
    }

//    @Test
//    public void testPreconImplConjXXXWTF() throws Narsese.NarseseException {
//
//    /*
//    in: $.50 (a &&+5 b). 1⋈6 %1.0;.90%
//    in: $.50 (b &&+5 c). 6⋈11 %1.0;.90%
//    in: $.50 (c &&+5 d). 11⋈16 %1.0;.90%
//      instability:
//        $.33 ((b &&+5 b) &&+5 #1). 11⋈21 %1.0;.37% {18: 1;2;3;;} ((%1,(%2==>%3),belief(positive),notImpl(%1),time(urgent)),(subIfUnifiesAny(%3,%2,%1),((DeductionRecursive-->Belief),(InductionRecursive-->Goal))))
//            $.63 ((b &&+5 #1) &&+5 d). 6⋈16 %1.0;.82% {13: 1;2;3;;} ((%1,%2,task("."),time(raw),time(dtEventsOrEternals),neqAndCom(%1,%2),notImpl(%1),notImpl(%2)),(varIntro((polarize(%1,task) &&+- polarize(%2,belief))),((IntersectionDepolarized-->Belief))))
//            $.63 (($1 &&+5 d) ==>-10 (b &&+5 $1)). 11 %1.0;.45% {13: 1;2;3;;} ((%1,%2,time(raw),task(positive),task("."),time(dtEventsOrEternals),neqAndCom(%1,%2),notImpl(%1),notImpl(%2)),(varIntro((%1 ==>+- %2)),((Induction-->Belief))))
//     */
//
//        Temporalize t = new Temporalize();
//        t.knowTerm($.$("((b &&+5 #1) &&+5 d)"), 6, 16);
//        t.knowTerm($.$("(($1 &&+5 d) ==>-10 (b &&+5 $1))"), 11);
//
//        Term P = nars.$.$("((b &&+- b) &&+- #1))");
//
//        Temporalize.Event s = t.solve(P);
//        assertEquals("(c ==>-4 (a &&+1 b))@5", s.toString());
//    }

    @Ignore
    @Test
    public void testPreconImplConjPreConflict() throws Narsese.NarseseException {

        TreeSet<String> solutions = new TreeSet();

        for (int i = 0; i < 1; i++) {
            Temporalize t = new Temporalize();
            t.knowTerm($("(y ==>+1 z)"), 0);
            t.knowTerm($("(x ==>+2 z)"), 0);
            Event s = t.solve($("((x &&+- y) ==>+- z)"));
            if (s != null) {
                solutions.add(s.toString());
            }
        }

        assertEquals("[((x &&+1 y) ==>+1 z)@-1]", solutions.toString());
    }

    @Test
    public void testDropAnyEvent0() throws Narsese.NarseseException {
        //instability:
        //$.05 ((a-->b) ==>+4 (c-->d)). -3 %1.0;.38% {160: 1;2;3;;} (((%1==>%2),%1,belief("&&")),((dropAnyEvent(%1) ==>+- %2),((StructuralDeduction-->Belief))))
        //    $.13 (((a-->b) &&+1 (b-->c)) ==>+3 (c-->d)). 1 %1.0;.42% {7: 1;2;3} ((%1,%2,time(raw),task("."),time(dtEvents),notImpl(%1),notImpl(%2)),((%2 ==>+- %1),((Abduction-->Belief))))
        Temporalize t = new Temporalize();
        t.knowTerm($("(((a-->b) &&+1 (b-->c)) ==>+3 (c-->d))"), 1);
        Term p = $("((a-->b) ==>+- (c-->d))");
        Event s = t.solve(p);
        assertNotNull(s);
        assertEquals(
                //only the eternal nature can be inferred. but during derivation this will typically be un-eternalized if the premise has temporality
                "((a-->b) ==>+4 (c-->d))@ETE",
                s.toString());

    }

    @Test
    public void testDropAnyEvent1() throws Narsese.NarseseException {
        //dropAnyEvent( ((a &&+5 ((--,a)&|b)) &&+5 ((--,b) &&+5 (--,c))) )    range=16
        //  should not result:
        //      ((a &&+5 b) &&+15 ((--,b) &&+5 (--,c)))    range=26
        //                  ^ should be 5

        testDropAnyEvent("((a &&+5 ((--,a)&|b)) &&+5 ((--,b) &&+5 (--,c)))");
    }


    void testDropAnyEvent(String x) throws Narsese.NarseseException {
        Set<Term> result = new TreeSet();
        Term tx = $(x);

        List<ObjectLongPair<Term>> events = tx.events();

        for (int i = 0; i < 10 * events.size(); i++) {

            Term t = $("dropAnyEvent( " + x + " )").eval(n);

            assertNotEquals(tx, t);
            assertTrue(t.dtRange() <= tx.dtRange());

            List<ObjectLongPair<Term>> e2 = tx.events();
            e2.forEach(e -> {
                assertTrue(events.contains(e));
            });

            result.add(t);
        }
        //System.out.println(Joiner.on('\n').join(result));
        assertEquals(events.size(), result.size());
    }

}
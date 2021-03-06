//package nars.derive;
//
//import com.google.common.base.Joiner;
//import jcog.list.FasterList;
//import nars.$;
//import nars.NAR;
//import nars.NARS;
//import nars.Narsese;
//import nars.derive.time.*;
//import nars.term.Term;
//import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
//import org.eclipse.collections.impl.list.mutable.FastList;
//import org.junit.jupiter.api.Disabled;
//import org.junit.jupiter.api.Test;
//
//import java.util.*;
//
//import static nars.$.$;
//import static nars.$.the;
//import static nars.time.Tense.ETERNAL;
//import static org.junit.jupiter.api.Assertions.*;
//
//public class TemporalizeTest {
//
//    final NAR n = NARS.shell();
//
//    @Test
//    public void testEternalEquivalence() {
//        DeriveTime t = new DeriveTime();
//        t.knowEternal($.the("x"));
//        t.knowEternal($.the("x"));
//        assertEquals(1, t.constraints.get($.the("x")).size());
//    }
//    @Test
//    public void testEternalInequivalence() throws Narsese.NarseseException {
//        DeriveTime t = new DeriveTime();
//        t.knowEternal($.$("(x ==>+1 y)"));
//        t.knowEternal($.$("(x ==>+2 y)"));
//        assertEquals(2, t.constraints.get($.$("(x==>y)")).size());
//    }
//
//    @Test
//    public void testAbsoluteRanking() {
//        DeriveTime t = new DeriveTime();
//
//
//        //eternal should be ranked lower than non-eternal
//        Term x = the("x");
//        AbsoluteEvent ete = DeriveTime.absolute(x, ETERNAL, ETERNAL);
//        AbsoluteEvent tmp = DeriveTime.absolute(x, 0, 0);
//        assertEquals(+1, ete.compareTo(tmp));
//        assertEquals(-1, tmp.compareTo(ete));
//        assertEquals(0, ete.compareTo(ete));
//        assertEquals(0, tmp.compareTo(tmp));
//        FasterList<AbsoluteEvent> l = new FasterList<>();
//        l.add(ete);
//        l.add(tmp);
//        l.sortThis();
//        assertEquals("[x@0, x@ETE]", l.toString());
//    }
//
//    @Test
//    public void testRelativeRanking() throws Narsese.NarseseException {
//        DeriveTime t = new DeriveTime();
//
//        Term x = the("x");
//        AbsoluteEvent xa = DeriveTime.absolute(x, ETERNAL, ETERNAL);
//        t.constraints.put(x, new TreeSet(List.of(xa)));
//
//        Term y = the("y");
//        AbsoluteEvent ya = DeriveTime.absolute(y, 0, 0);
//        t.constraints.put(y, new TreeSet(List.of(ya)));
//
//        Term z = the("z");
//        RelativeEvent zx = t.relative(z, x, 0);
//        RelativeEvent zy = t.relative(z, y, 0);
//        assertEquals(0, zx.compareTo(zx));
//        assertEquals(0, zy.compareTo(zy));
//
//
//        Term ab = $("(a-->b)");
//        RelativeEvent zab = t.relative(z, ab, 0);
//        assertEquals(1, zx.compareTo(zab)); //prefer simpler referrents, always
//        assertEquals(1, zy.compareTo(zab)); //prefer simpler referrents, always
//
//        assertEquals(-1, zx.compareTo(zy));
//        assertEquals(-zy.compareTo(zx), zx.compareTo(zy));
//
//
//        FasterList<RelativeEvent> l = new FasterList<>();
//        l.add(zab);
//        l.add(zx);
//        l.add(zy);
//        l.sortThis();
//        assertEquals("[z@0->(a-->b), z@0->x, z@0->y]", l.toString()); //y first since it is non-eternal
//    }
//
//    @Test
//    public void testAbsoluteRelativeRanking() {
//        DeriveTime t = new DeriveTime();
//
//        //eternal should be ranked lower than non-eternal
//        Term x = the("x");
//        Term y = the("y");
//        Event yTmp0 = DeriveTime.absolute(y, 0, 0);
//        Event xEte = DeriveTime.absolute(x, ETERNAL, ETERNAL);
//        Event xRelY = t.relative(x, y, 0);
//        assertEquals(+1, xEte.compareTo(xRelY));
////        assertEquals(-1, tmp.compareTo(ete));
////        assertEquals(0, ete.compareTo(ete));
////        assertEquals(0, tmp.compareTo(tmp));
//        FasterList<Event> l = new FasterList<>();
//        l.add(xEte);
//        l.add(xRelY);
//        l.sortThis();
//        assertEquals("[x@0->y, x@ETE]", l.toString()); //ETE must be dead last
//    }
//
//
//    @Test
//    public void testSolveTermSimple() throws Narsese.NarseseException {
//
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("a"), 1);
//        t.knowAbsolute($("b"), 3);
//        assertEquals("(a &&+2 b)@[1..3]", t.solve($("(a &&+- b)")).toString());
//        assertEquals("(a &&+2 b)@[1..3]", t.solve($("(b &&+- a)")).toString());
//
//    }
//
//    @Test
//    public void testSolveIndirect() throws Narsese.NarseseException {
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("(a ==>+1 c)"), ETERNAL);
//        t.knowAbsolute($("a"), 0);
//        Event s = t.solve($("c"));
//        assertNotNull(s);
//        assertEquals("c@1->a", s.toString());
//    }
//
//    @Test
//    public void testSolveIndirect2() throws Narsese.NarseseException {
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("(b ==>+1 c)"), ETERNAL);
//        t.knowAbsolute($("(a ==>+1 b)"), 0);
//        t.knowAbsolute($("c"), 2);
//
//        Map<Term, Time> h = new HashMap();
//        Event s = t.solve($("a"), h);
//        assertNotNull(s);
//        assertEquals(0, s.start(h).abs());
//        //assertEquals("a@0", s.toString());
//        assertEquals("a@-1->b", s.toString());
//    }
//
//    @Test
//    public void testSolveEternalButRelative() throws Narsese.NarseseException {
//        /*
//                .believe("(x ==>+2 y)")
//                .believe("(y ==>+3 z)")
//                .mustBelieve(cycles, "(x ==>+5 z)", 1.00f, 0.81f);
//                */
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("(x ==>+2 y)"), ETERNAL);
//        t.knowAbsolute($("(y ==>+3 z)"), ETERNAL);
//
//        Event s = t.solve($("(x ==>+- z)"));
//        assertNotNull(s);
//        assertEquals("(x ==>+5 z)@ETE", s.toString());
//    }
//
//    @Test
//    public void testSolveEternalButRelative3() throws Narsese.NarseseException {
//        /*
//        RIGHT: (z ==>+1 x).
//        WRONG:
//        $1.0 (z ==>-2 x). %1.0;.45% {18: 1;2} (((%1==>%2),(%1==>%3),neqCom(%2,%3),notImpl(%3)),((%3 ==>+- %2),((Abduction-->Belief))))
//            $.50 (y ==>+3 x). %1.0;.90% {0: 1}
//            $.50 (y ==>+2 z). %1.0;.90% {0: 2}
//         */
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("(y ==>+3 x)"), ETERNAL);
//        t.knowAbsolute($("(y ==>+2 z)"), ETERNAL);
//
//        Event s = t.solve($("(z ==>+- x)"));
//        assertNotNull(s);
//        assertEquals("(z ==>+1 x)@ETE|2", s.toString());
//    }
//
//
//    @Test
//    public void testSolveRecursiveConjDecomposition() throws Narsese.NarseseException {
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("((x &&+1 y) &&+1 z)"), 0);
//
//        Map<Term, Time> h = new HashMap();
//        h.clear();
//        assertEquals(0, t.solve($("x"), h).start(h).abs());
//        h.clear();
//        assertEquals(1, t.solve($("y"), h).start(h).abs());
//        h.clear();
//        assertEquals(2, t.solve($("z"), h).start(h).abs());
//    }
//
//    @Test
//    public void testSolveRecursiveConjDecomposition2() throws Narsese.NarseseException {
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("((x &&+1 y) &&+1 z)"), 0);
//
//        //System.out.println(t);
//        //System.out.println("x: " + t.constraints.get($("x")));
//        //System.out.println("z: " + t.constraints.get($("z")));
//
//        assertEquals("(x &&+2 z)@[0..2]", t.solve($("(x &&+- z)")).toString());
//        assertEquals("(x &&+1 y)@[0..1]", t.solve($("(x &&+- y)")).toString());
//        assertEquals("(y &&+1 z)@[1..2]", t.solve($("(y &&+- z)")).toString());
//
//        assertEquals("(x ==>+2 z)@0", t.solve($("(x ==>+- z)")).toString());
//        assertEquals("(x ==>+1 y)@0", t.solve($("(x ==>+- y)")).toString());
//        assertEquals("(y ==>+1 z)@1", t.solve($("(y ==>+- z)")).toString());
//    }
//
//    /**
//     * tests temporalization of pure events which overlap, or are separated by a distance below a proximal threshold (see Param.java)
//     */
//    @Test
//    public void testStatementEventsBothTemporal() throws Narsese.NarseseException {
////              .input(new NALTask($.$("(a-->b)"), GOAL, $.t(1f, 0.9f), 5, 10, 20, new long[]{100}).pri(0.5f))
////              .input(new NALTask($.$("(c-->b)"), BELIEF, $.t(1f, 0.9f), 4, 5, 25, new long[]{101}).pri(0.5f))
////                   .mustDesire(cycles, "(a-->c)", 1f, 0.4f, 10, 20)
//
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("(a-->b)"), 10, 20); //these two overlap, so there should be a derivation
//        t.knowAbsolute($("(c-->b)"), 5, 25);
//
//        Term st = $("(a-->c)");
//        Event solution = t.solve(st);
//        assertNotNull(solution);
//        //assertEquals("(a-->c)@[10..20]", solution.toString());
//        assertEquals("(a-->c)@ETE", solution.toString());
//
//        assertEquals("(a-->d)@ETE", t.solve($("(a-->d)")).toString());
//        //assertNull("d not covered by known events", t.solve($("(a-->d)")));
//    }
//
//    /**
//     * tests temporalization of pure events which overlap, or are separated by a distance below a proximal threshold (see Param.java)
//     */
//    @Disabled
//    @Test
//    public void testStatementEventsNearlyOverlappingTemporal() throws Narsese.NarseseException {
////              .input(new NALTask($.$("(a-->b)"), GOAL, $.t(1f, 0.9f), 5, 10, 20, new long[]{100}).pri(0.5f))
////              .input(new NALTask($.$("(c-->b)"), BELIEF, $.t(1f, 0.9f), 4, 5, 25, new long[]{101}).pri(0.5f))
////                   .mustDesire(cycles, "(a-->c)", 1f, 0.4f, 10, 20)
//
//        DeriveTime t = new DeriveTime();
//        t.dur = 10;
//        t.knowAbsolute($("(a-->b)"), 10, 15); //these two overlap, so there should be a derivation
//        t.knowAbsolute($("(c-->b)"), 1, 5);
//
//        Event solution = t.solve($("(a-->c)"));
//        assertEquals("(a-->c)@7", solution.toString());
//    }
//
//    /**
//     * tests temporalization of pure events which overlap, or are separated by a distance below a proximal threshold (see Param.java)
//     */
//    @Test
//    public void testStatementEventsNonOverlappingTemporal() throws Narsese.NarseseException {
////              .input(new NALTask($.$("(a-->b)"), GOAL, $.t(1f, 0.9f), 5, 10, 20, new long[]{100}).pri(0.5f))
////              .input(new NALTask($.$("(c-->b)"), BELIEF, $.t(1f, 0.9f), 4, 5, 25, new long[]{101}).pri(0.5f))
////                   .mustDesire(cycles, "(a-->c)", 1f, 0.4f, 10, 20)
//
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("(a-->b)"), 10, 15); //these two overlap, so there should be a derivation
//        t.knowAbsolute($("(c-->b)"), 1, 5);
//
//        Event solution = t.solve($("(a-->c)"));
//        assertEquals("(a-->c)@ETE", solution.toString());
//        //assertNull(solution);
//    }
//
//    @Test
//    public void testImplConjWTFWTFSubj() throws Narsese.NarseseException {
//        //$.26 a. -4 %1.0;.40% {16: 1;2} ((%1,(%1==>%2),time(urgent),notImpl(%1)),(%2,((DeductionRecursivePB-->Belief),(InductionRecursivePB-->Goal))))
//        //    $.50 (b &&+5 c). 6⋈11 %1.0;.90% {6: 2}
//        //    $.38 ((b &&+5 c) ==>-10 a). 6 %1.0;.45% {6: 1;2} ((%1,%2,time(raw),task(positive),task("."),time(dtEvents),notImpl(%1)),((%1 ==>+- %2),((Induction-->Belief))))
//
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("(b &&+5 c)"), 6, 11); //these two overlap, so there should be a derivation
//        t.knowAbsolute($("((b &&+5 c) ==>-10 a)"), 6 /* shouldnt matter what this is */);
//
//        Term a = $("a");
//        HashMap h = new HashMap();
//        Event solution = t.solve(a, h);
//        assertEquals(1, solution.start(h).abs());
//    }
//
//    @Test
//    public void testImplConjWTFWTFPred() throws Narsese.NarseseException {
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("(b &&+5 c)"), 6, 11); //these two overlap, so there should be a derivation
//        t.knowAbsolute($("(a ==>+3 (b &&+5 c))"), ETERNAL /* shouldnt matter what this is */);
//
//        Term a = $("a");
//        HashMap h = new HashMap();
//        Event solution = t.solve(a, h);
//        System.out.println(h);
//        assertEquals(3, solution.start(h).abs());
//    }
//
//    @Test
//    public void testImplConjWTFWTFSubjPred() throws Narsese.NarseseException {
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("(b &&+5 c)"), 6, 11); //these two overlap, so there should be a derivation
//        t.knowAbsolute($("((x &&+1 y) ==>+3 (b &&+5 c))"), ETERNAL /* shouldnt matter what this is */);
//
//        {
//            Term a = $("y");
//            HashMap h = new HashMap();
//            Event solution = t.solve(a, h);
//            System.out.println(t);
//            System.out.println(h);
//            assertEquals(3, solution.start(h).abs());
//        }
//        System.out.println();
//        Term a = $("x");
//        System.out.println(t);
//        System.out.println(t.constraints.get(a));
//        HashMap h = new HashMap() {
//            @Override
//            public Object put(Object key, Object value) {
//                System.out.println(" --> " + key + "   " + value);
//                return super.put(key, value);
//            }
//        };
//        Event solution = t.solve(a, h);
//        System.out.println(h);
//        assertEquals(2, solution.start(h).abs());
//    }
//
//    @Test
//    public void testDTernalize() throws Narsese.NarseseException {
//        DeriveTime t = new DeriveTime();
//        Term dternal = $("(a&&b)");
//        Term xternal = $("(a &&+- b)");
//        t.knowAbsolute(dternal, ETERNAL); //these two overlap, so there should be a derivation
//
//        Event solution = t.solve(xternal);
//        assertNotNull(solution);
//        assertNotEquals("(a&|b)@ETE", solution.toString());
//        assertEquals("(a&&b)@ETE", solution.toString());
//    }
//
//    @Test
//    public void testEternalNotParallel() throws Narsese.NarseseException {
//        /*
//        (((#1-->swimmer)&&(#1-->$2))==>(swan-->$2)). %.90;.45%
//            ((#1-->swimmer) &&+- (#1-->$2))
//            YES: ((#1-->swimmer)&&(#1-->$2))
//             NO: ((#1-->swimmer)&|(#1-->$2))
//        */
//        DeriveTime t = new DeriveTime();
//
//
//        t.knowEternal($("(((#1-->swimmer)&&(#1-->$2))==>(swan-->$2))"));
//
//        System.out.println(t);
//        HashMap h = new HashMap();
//        Event solution = t.solve($("((#1-->swimmer) &&+- (#1-->$2))"), h);
//        System.out.println(h);
//        assertNotNull(solution);
//        //assertEquals("((#1-->swimmer)&&(#1-->$2))@ETE", solution.toString());
//        assertEquals("((#1-->swimmer)&&(#1-->$2))@ETE", solution.toString());
//        assertEquals(ETERNAL, solution.start(h).abs());
//    }
//
//    @Test
//    public void testStatementEventsOneEternal() throws Narsese.NarseseException {
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("(a-->b)"), ETERNAL); //these two overlap, so there should be a derivation
//        t.knowAbsolute($("(c-->b)"), 5, 25);
//
//        Event solution = t.solve($("(a-->c)"));
//        assertNotNull(solution);
//        assertEquals("(a-->c)@ETE", solution.toString());
//    }
//
//    @Test
//    public void testSolveEternalButRelative2() throws Narsese.NarseseException {
//        DeriveTime t = new DeriveTime();
//
//        //  b ==>+10 c ==>+20 e
//
//        t.knowAbsolute($("(b ==>+10 c)"), ETERNAL);
//        t.knowAbsolute($("(e ==>-20 c)"), ETERNAL);
//
////        System.out.println( Joiner.on('\n').join( t.constraints.entrySet() ) );
//
//        HashMap h = new HashMap();
//        Event s = t.solve($("(b ==>+- e)"), h);
//
////        System.out.println();
////        System.out.println( Joiner.on('\n').join( h.entrySet() ) );
//
//        assertNotNull(s);
//        assertEquals("(b ==>+30 e)@ETE", s.toString());
//    }
//
//    @Test
//    public void testImplDT() throws Narsese.NarseseException {
//        /*
//            $1.0 ((d-->c) ==>-3 (a-->b)). 2 %1.0;.45% {6: 1;2} ((%1,%2,time(raw),task(positive),task("."),time(dtEvents),notImpl(%1),notImpl(%2)),((%1 ==>+- %2),((Induction-->Belief))))
//              $.50 (d-->c). 5 %1.0;.90% {5: 2}
//              $.50 (a-->b). 2 %1.0;.90% {2: 1}
//        */
//
//
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("(d-->c)"), 5);
//        t.knowAbsolute($("(a-->b)"), 2);
//
//        Event solution = t.solve($("((d-->c) ==>+- (a-->b))"));
//        assertNotNull(solution);
//        assertEquals("((d-->c) ==>-3 (a-->b))@5", solution.toString());
//    }
//
//    @Test
//    public void testImplConjDepvar() throws Narsese.NarseseException {
//
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("a"), 1);
//        t.knowAbsolute($("((a &&+5 b) ==>+5 #1)"), 1);
//        t.print();
////        {
////            HashMap h = new HashMap();
////            Term depVar = $.varDep(1);
////            Event solution = t.solve(depVar, h);
////            assertNotNull(solution);
////            assertEquals(11, solution.start(h).abs());
////        }
//
//        {
//            Map<Term, Time> h = new HashMap();
//            Event solution = t.solve($("(a &&+- b)"), h);
//            assertNotNull(solution);
//            assertEquals("(a &&+5 b)", solution.term.toString());
//            assertEquals(1, solution.start(h).abs()); //@[1..6]
//        }
//        Event solution = t.solve($("(a &&+- #1)"));
//        assertNotNull(solution);
//        assertEquals("(a &&+10 #1)@[1..11]", solution.toString());
//    }
//
//    @Test
//    public void testSolveConjSequenceMerge() throws Narsese.NarseseException {
//        DeriveTime t = new DeriveTime();
//
//        String A = "((a &&+3 c) &&+4 e)";
//        t.knowAbsolute($(A), 1);
//        String B = "b";
//        t.knowAbsolute($(B), 4);
//
////        System.out.println( Joiner.on('\n').join( t.constraints.entrySet() ) );
//
//        HashMap h = new HashMap();
//        Event s = t.solve($("(" + A + " &&+- " + B + ")"), h);
//
////        System.out.println();
////        System.out.println( Joiner.on('\n').join( h.entrySet() ) );
//
//        assertNotNull(s);
//        assertEquals("((a &&+3 (b&|c)) &&+4 e)@[1..8]", s.toString());
//    }
//
//    @Test
//    public void testRecursiveSolution1a() throws Narsese.NarseseException {
//        /*
//                 believe("(x ==>+5 z)")
//                .believe("(y ==>+3 z)")
//                .mustBelieve(cycles, "( (x &&+2 y) ==>+3 z)", 1f, 0.81f)
//         */
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("(x ==>+5 z)"), ETERNAL);
//        t.knowAbsolute($("(y ==>+3 z)"), ETERNAL);
//
//        //System.out.println(Joiner.on('\n').join(t.constraints.entrySet()));
//
//        HashMap h = new HashMap();
//        Event s = t.solve($("(x &&+- y)"), h);
//        assertNotNull(s);
//        assertEquals("(x &&+2 y)@ETE", s.toString());
//
//    }
//
//    @Test
//    public void testRecursiveSolution1b() throws Narsese.NarseseException {
//
//        for (String op : new String[]{"==>", "&&"}) {
//            DeriveTime t = new DeriveTime();
//            t.knowAbsolute($("(x ==>+5 z)"), ETERNAL);
//            t.knowAbsolute($("(y ==>+3 z)"), ETERNAL);
//
//            HashMap h = new HashMap();
//            Event s = t.solve($("((x &&+- y) " + op + "+- z)"), h);
//
////            System.out.println();
////            System.out.println(Joiner.on('\n').join(h.entrySet()));
//
//            int dt = 3;
//            String pattern = "((x &&+2 y) " + op + "+" + dt + " z)@ETE";
//
//            assertNotNull(s);
//            assertTrue(s.toString().startsWith(pattern));
//        }
//
//    }
//
////    @Test
////    public void testImplToEquiCircularity() throws Narsese.NarseseException {
////        Temporalize t = new Temporalize();
////        t.knowTerm($.$("(x ==>+5 y)"), ETERNAL);
////        t.knowTerm($.$("(y ==>-5 x)"), ETERNAL);
////        assertEquals("(x <=>+5 y)@ETE", t.solve($.$("(x <=>+- y)")).toString());
////
////        //    @Test public void testImplToEquiCircularityAvg() throws Narsese.NarseseException {
////        //        Temporalize t = new Temporalize();
////        //        t.knowTerm($.$("(x ==>+6 y)"), ETERNAL);
////        //        t.knowTerm($.$("(y ==>-4 x)"), ETERNAL);
////        //        assertEquals("(x <=>+5 y)@ETE", t.solve($.$("(x <=>+- y)")).toString());
////        //    }
////
////    }
//
//
//    @Test
//    public void testImplLinked() throws Narsese.NarseseException {
//        /*
//        ( $,TestNAR ): "ERR	(z ==>+1 x). %(1.0,1.0);(.45,.46)%  creation: (0,400)".
//        ( $,TestNAR ): "SIM
//        $1.0 (z ==>-2 x). %1.0;.45% {9: 1;2} (((%1==>%2),(%3==>%2),neqCom(%1,%3),notImpl(%3)),((%3 ==>+- %1),((Induction-->Belief))))
//            $.50 (x ==>+2 y). %1.0;.90% {0: 1}
//            $.50 (z ==>+3 y). %1.0;.90% {0: 2}
//         */
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("(x ==>+2 y)"), ETERNAL);
//        t.knowAbsolute($("(z ==>+3 y)"), ETERNAL);
//        Event s = t.solve($("(z ==>+- x)"));
//        assertNotNull(s);
//        assertEquals("(z ==>+1 x)@ETE|-3", s.toString());
//    }
//
//    @Test
//    public void testConjConjImpl() throws Narsese.NarseseException {
//        /*
//        WRONG, should be @ 1:
//        //    $.25 ((a,#1) &&+1 (#1,c)). 2⋈3 %1.0;.27% {70: 1;2;3} ((%1,(%2==>%1),time(urgent),notImpl(%1)),(%2,((AbductionRecursivePB-->Belief),(DeductionRecursivePB-->Goal))))
//        //      $.50 (c,d). 5 %1.0;.90% {5: 3}
//        //      $.13 (((a,#1) &&+1 (#1,c)) ==>+3 (c,d)). 1 %1.0;.42% {59: 1;2;3} ((%1,%2,time(raw),belief(positive),task("."),time(dtEventsOrEternals),neqAndCom(%1,%2),notImpl(%1),notImpl(%2)),(varIntro((%2 ==>+- %1)),((Abduction-->Belief))))
//         */
//
//        assertEquals($("( (#1,c) &&+- (a,#1) )"), $("( (a,#1) &&+- (#1,c) )"));
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("(c,d)"), 5);
//        t.knowAbsolute($("(((a,#1) &&+1 (#1,c)) ==>+3 (c,d))"), 1);
//        Map<Term, Time> h = new HashMap();
//        Event s = t.solve($("( (#1,c) &&+- (a,#1) )"), h);
//        assertNotNull(s);
//        assertEquals("((a,#1) &&+1 (#1,c))", s.term.toString());
//        assertEquals(1, s.start(h).abs()); //@[1..2]
//
//    }
//
//    @Test
//    public void testCoNegationCrossloop() throws Narsese.NarseseException {
//        /*
//        WRONG, should be dt=-10
//            //$.04 ((--,(x-->b))=|>(x-->a)). 0 %0.0;.07% {13: 1;2;;} ((((--,%1)==>%2),%2),(((--,%2) ==>+- %1),((Contraposition-->Belief))))
//            //    $.08 ((--,(x-->a)) ==>+10 (x-->b)).
//         */
//
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("((--,(x-->a)) ==>+10 (x-->b))"), ETERNAL);
//        Event s = t.solve($("((--,(x-->b)) ==>+- (x-->a))"));
//        assertNotNull(s);
//        assertEquals("((--,(x-->b)) ==>-10 (x-->a))@ETE", s.toString());
//    }
//
//    @Test
//    public void testConjLinked() throws Narsese.NarseseException {
//// WRONG:
////        $.31 ((b &&+5 c) ==>+5 (a &&+5 b)). 6 %1.0;.45% {7: 1;2} ((%1,%2,time(raw),belief(positive),task("."),time(dtEvents),notImpl(%1),notImpl(%2)),((%2 ==>+- %1),((Abduction-->Belief))))
////          $.50 (a &&+5 b). 1⋈6 %1.0;.90% {1: 1}
////          $.50 (b &&+5 c). 6⋈11 %1.0;.90% {6: 2}
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("(a &&+5 b)"), 1);
//        t.knowAbsolute($("(b &&+5 c)"), 6);
//        assertEquals("(a &&+5 b)@[1..6]", t.solve($("(a &&+- b)")).toString());
//        assertEquals("((a &&+5 b) &&+5 c)@[1..11]", t.solve($("((a &&+- b) &&+- c)")).toString());
//        assertEquals("((a &&+5 b) ==>+5 c)@1", t.solve($("((a &&+5 b) ==>+- (b &&+5 c))")).toString());
//        assertEquals("((b &&+5 c) ==>-10 a)@6", t.solve($("((b &&+5 c) ==>+- (a &&+5 b))")).toString());
//
//    }
//
//    @Test
//    public void testConjLinked2() throws Narsese.NarseseException {
//// WRONG:
////    $.27 ((a &&+5 b) &&+5 (c &&+5 (c&|d))). 1⋈16 %1.0;.73% {20: 1;2;3} ((%1,%2,task("."),time(raw),time(dtEvents),notImpl(%1),notImpl(%2)),((polarize(%1,task) &&+- polarize(%2,belief)),((IntersectionDepolarized-->Belief))))
////      $.50 (c &&+5 d). 11⋈16 %1.0;.90% {11: 3}
////      $.31 ((a &&+5 b) &&+5 c). 1⋈11 %1.0;.81% {7: 1;2} ((%1,%2,task("."),time(raw),time(dtEvents),notImpl(%1),notImpl(%2)),((polarize(%1,task) &&+- polarize(%2,belief)),((IntersectionDepolarized-->Belief))))
//
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("(c &&+5 d)"), 11);
//        t.knowAbsolute($("((a &&+5 b) &&+5 c)"), 1);
//        assertEquals("((a &&+5 b) &&+5 (c &&+5 d))@[1..16]", t.solve($("((a &&+- b) &&+- (c &&+- d))")).toString());
//    }
//
//    @Test
//    public void testConjImpl23424234234() throws Narsese.NarseseException {
////$.09 (c &&+5 d). 16⋈21 %1.0;.12% {48: 1;2;3;;} ((%1,(%2==>%1),time(urgent),notImpl(%1)),(%2,((AbductionRecursivePB-->Belief),(DeductionRecursivePB-->Goal))))
////    $.50 (a &&+5 b). 1⋈6 %1.0;.90% {1: 1}
////    $.11 ((c &&+5 d) ==>-15 (a &&+5 b)). 11 %1.0;.51% {48: 1;2;3;;} Revection Merge
//        for (long invariant : new long[]{ETERNAL, 11, -10}) {
//            DeriveTime t = new DeriveTime();
//            t.knowAbsolute($("(a &&+5 b)"), 1, 6);
//            t.knowAbsolute($("((c &&+5 d) ==>-15 (a &&+5 b))"), invariant);
//            Map<Term, Time> h = new HashMap();
//            Event s = t.solve($("(c &&+- d)"), h);
//
//            assertEquals("(c &&+5 d)", s.term.toString());
//            assertEquals(11, s.start(h).abs()); //@[11..16]
//        }
//    }
//
//    @Test
//    public void testConjImpl23424232354234() throws Narsese.NarseseException {
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("a"), 1);
//        t.knowAbsolute($("(((b &&+5 c) &&+5 d) ==>-15 a)"), 6);
//        System.out.println(t);
//
//        assertTrue(t.constraints.get($("((b &&+5 c) &&+5 d)")).toString().contains("((b &&+5 c) &&+5 d)@[5..15]->a"));
//        //assertEquals("[(b &&+5 c)@[5..10]->a, (b &&+5 c)@[-10..-5]->d]", t.constraints.get($("(b &&+5 c)")).toString());
//
//        Term r = $("((b &&+- c) &&+- d)");
//        Map<Term, Time> h = new HashMap();
//        Event e = t.solve(r, h);
//        assertNotNull(e);
//        assertEquals("((b &&+5 c) &&+5 d)@[6..16]", e.toString());
//        assertEquals(6, e.start(h).abs());
//    }
//
//    @Test
//    public void testImplFromConj() throws Narsese.NarseseException {
//        //WRONG:    $.40 ((c) ==>-2 ((a) &&+1 (b))). 5 %1.0;.42% {9: 1;2;3} ((%1,%2,time(raw),task(positive),task("."),time(dtEvents),notImpl(%1),notImpl(%2)),((%1 ==>+- %2),((Induction-->Belief))))
//        //               $.50 (c). 5 %1.0;.90% {5: 3}
//        //               $1.0 ((a) &&+1 (b)). 1⋈2 %1.0;.81% {3: 1;2} ((%1,%2,task("."),time(raw),time(dtEvents),notImpl(%1),notImpl(%2)),((polarize(%1,task) &&+- polarize(%2,belief)),((IntersectionDepolarized-->Belief))))
//
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("c"), 5);
//        t.knowAbsolute($("(a &&+1 b)"), 1, 2);
//
//        Term P = $("(c ==>+- (a &&+- b))");
//
//        Event s = t.solve(P);
//        assertEquals("(c ==>-4 (a &&+1 b))@5", s.toString());
//    }
//
//    @Test
//    public void testAnotherInstability2342348927342734891() throws Narsese.NarseseException {
//        //            $.01 (((a&|b) &&+5 (b&|c)) ==>+5 (c &&+5 d)). 6 %1.0;.29% {100: 1;2;3} ((((&&,%1073742337..+)==>%2),%3,neqRCom(%2,%3),notImpl(%3)),((((&&,%1073742337..+) &&+- %3) ==>+- %2),((Induction-->Belief))))
//        //              $.10 ((a &&+5 b) ==>+5 (c &&+5 d)). 1 %1.0;.45% {12: 1;3} ((%1,%2,time(raw),belief(positive),task("."),time(dtEvents),neq(%1,%2),notImpl(%2)),((%2 ==>+- %1),((Induction-->Belief))))
//        //              $.50 (b &&+5 c). 6⋈11 %1.0;.90% {6: 2}
//
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("((a &&+5 b) ==>+5 (c &&+5 d))"),1);
//        t.knowAbsolute($("(b &&+5 c)"), 6, 11);
//
//        Term P = $("(((a&|b) &&+- (b&|c)) ==>+- (c &&+- d))");
//
//        Event s = t.solve(P);
//        assertEquals("(((a &&+5 b) &&+5 c) ==>+5 d)@1", s.toString());
//    }
//    @Test
//    public void testAnotherInstability234234892742() throws Narsese.NarseseException {
//        //$0.0 ((a&|b) &&+5 (b&|c)). -9⋈-4 %1.0;.13% {124: 1;2;3} ((%1,(%2==>%3),time(urgent),neq(%1,%2),notImpl(%1)),(subIfUnifiesAny(%2,%3,%1),((AbductionRecursivePB-->Belief),(DeciInduction-->Goal))))
//        //    $.50 (c &&+5 d). 11⋈16 %1.0;.90% {11: 3}
//        //    $.01 (((a&|b) &&+5 (b&|c)) ==>+5 (c &&+5 d)). 6
//
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("(c &&+5 d)"), 11, 16);
//        t.knowAbsolute($("(((a&|b) &&+5 (b&|c)) ==>+5 (c &&+5 d))"), 6);
//
//        Term P = $("((a&|b) &&+- (b&|c))");
//
//        Event s = t.solve(P);
//        assertEquals("((a&|b) &&+5 (b&|c))@[1..6]", s.toString());
//    }
//
//    @Test
//    public void testConjLinked3() throws Narsese.NarseseException {
//        /*
//          instability:
//$.72 (a &&+5 b). -4⋈1 %1.0;.30% {151: 1;2;;} ((%1,(%2==>%3),belief(positive),notImpl(%1),time(urgent)),(subIfUnifiesAny(%3,%2,%1),((DeductionRecursive-->Belief),(InductionRecursive-->Goal))))
//    $.97 (b &&+5 c). 6⋈11 %1.0;.66% {14: 1;2;;} ((%1,%1,task("&&")),(dropAnyEvent(%1),((StructuralDeduction-->Belief),(StructuralDeduction-->Goal))))
//      $1.0 ((a &&+5 b) &&+5 c). 1⋈11 %1.0;.73% {9: 1;2;;} ((%1,%2,task("."),time(raw),time(dtEvents),notImpl(%1),notImpl(%2)),((polarize(%1,task) &&+- polarize(%2,belief)),((IntersectionDepolarized-->Belief))))
//        $.50 (b &&+5 c). 6⋈11 %1.0;.90% {6: 2}
//        $1.0 a. 1 %1.0;.81% {2: 1;;} ((%1,%1,task("&&")),(dropAnyEvent(%1),((StructuralDeduction-->Belief),(StructuralDeduction-->Goal))))
//          $.50 (a &&+5 b). 1⋈6 %1.0;.90% {1: 1}
//      $1.0 ((a &&+5 b) &&+5 c). 1⋈11 %1.0;.82% {14: 1;2;;}
//    $.63 ((b &&+5 c) ==>-10 (a &&+5 b)). 6 %1.0;.45% {7: 1;2} ((%1,%2,time(raw),task(positive),task("."),time(dtEvents),notImpl(%1),notImpl(%2)),((%1 ==>+- %2),((Induction-->Belief))))
//      $.50 (b &&+5 c). 6⋈11 %1.0;.90% {6: 2}
//      $.50 (a &&+5 b). 1⋈6 %1.0;.90% {1: 1}
//         */
//        Term A = $("((b &&+5 c) ==>-10 (a &&+5 b))");
//
//        {
//            DeriveTime t = new DeriveTime();
//            t.knowAbsolute(A, 6);
//
//            Map<Term, Time> h = new HashMap();
//            Event s = t.solve(the("a"), h);
//
//            //assertEquals("a@0->(a &&+5 b)" /*"a@-5->b"*/, s.toString());
//
//        }
//
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute(A, 6);
//
//        Term B = $("(b &&+5 c)");
//        t.knowAbsolute(B, 6, 11);
//
//        //System.out.println(t);
//
//        Term p = $("(a &&+- b)");
//        Map<Term, Time> h = new HashMap();
//        Event s = t.solve(p, h);
//        //System.out.println(h);
//        assertNotNull(s);
//        assertEquals("(a &&+5 b)@[1..6]", s.toString());
//    }
//
//    @Test
//    public void testConjInvert() throws Narsese.NarseseException {
//        //WRONG:    $.66 (((--,a)&|b) &&+5 a). 1⋈6 %1.0;.73% {10: 1;2;;} ((%1,%1,task("&&")),(dropAnyEvent(%1),((StructuralDeduction-->Belief),(StructuralDeduction-->Goal))))
//        //              $.63 ((a &&+5 ((--,a)&|b)) &&+5 (--,b)). 1⋈11 %1.0;.81% {6: 1;2} ((%1,%2,task("."),time(raw),time(dtEvents),notImpl(%1),notImpl(%2)),((polarize(%1,task) &&+- polarize(%2,belief)),((IntersectionDepolarized-->Belief))))
//        DeriveTime t = new DeriveTime();
//
//        int start = 1;
//        Term x = $("((a &&+5 ((--,a)&|b)) &&+5 (--,b))");
//        assertEquals(10, x.dtRange());
//        t.knowAbsolute(x, start, start + 10);
//
//
//        Term A = $("a");
//        Term Aneg = A.neg();
//
//        System.out.println(t);
//        System.out.println(t.constraints.get(A));
//        System.out.println(t.constraints.get(Aneg));
//
//        {
//            HashMap h = new HashMap();
//            Event st = t.solve(Aneg, h);
//            assertEquals("(--,a)", st.term.toString());
//            assertEquals((x.subTime(Aneg) + start), st.start(h).abs());
//        }
//        HashMap h = new HashMap();
//        Event st = t.solve(A, h);
//        assertEquals("a", st.term.toString());
//        assertEquals((x.subTime(A) + start), st.start(h).abs());
//
//
//        //System.out.println(a);
//        //System.out.println(b);
//        String r = "(a &&+5 ((--,a)&|b))@[1..6]";
//
//        Event ta = t.solve($("(((--,a)&|b) &&+- a)"));
//        assertNotNull(ta);
//        assertEquals(r, ta.toString());
//
//        Term b = $("(a &&+- ((--,a)&|b))"); //check mirror
//        assertEquals(r, t.solve(b).toString());
//
//    }
//
//    @Test
//    public void testConjComplex() throws Narsese.NarseseException {
//        String src = "((a &&+5 ((--,a)&|b)) &&+5 ((--,b) &&+5 (--,c)))";
//        Term x = $(src);
//        assertEquals(15, x.dtRange());
//
//        NAR n = NARS.shell();
//
//        Set<String> result = new TreeSet();
//        for (int i = 0; i < 100; i++) {
//
//            DeriveTime t = new DeriveTime();
//
//            t.knowAbsolute(x, 1, 16);
////            if (i == 0) {
////                //CHECK THE KNOWN PATTERN
////
////                for (String subterm : new String[]{"a", "b", "((--,b) &&+5 (--,c))"}) {
////                    FasterList<Event> cc = t.constraints.get($(subterm)); /// ? @[11..16]
////                    assertEquals(subterm + " has non-unique temporalizations: " + cc, 1, cc.count(y -> !(y instanceof RelativeEvent)));
////                }
////
////                System.out.println(t);
////                System.out.println();
////            }
//
//
//            Term a = $($("dropAnyEvent(" + src + ")").eval(n.terms).toString()
//                    .replace("&&+5", "&&+-")
//                    .replace("&&+10", "&&+-")
//            );
//
//            Event r = t.solve(a);
//            if (r != null) {
//
//                String xy = a + "\t" + r;
//
//                assertTrue(r.start(null).abs() >= 1, xy);
//                assertTrue(r.end(null).abs() <= 16, xy);
//
//
//                result.add(xy);
//
//            }
//
//        }
//
//        //result.forEach(System.out::println);
//    }
//
//    @Test
//    public void testImplInvert() throws Narsese.NarseseException {
//
//        int start = 1;
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("(--x ==>+10 x)"), start, start);
//
//         {
//            HashMap h = new HashMap();
//            Event st = t.solve($.the("x").neg(), h);
//            assertEquals("(--,x)", st.term.toString());
//            assertEquals(0, st.start(h).offset);
//        }
//        {
//            HashMap h = new HashMap();
//            Event st = t.solve($.the("x"), h);
//            assertEquals("x", st.term.toString());
//            assertEquals("x@10->(--,x)", st.toString());
//        }
//
//    }
//
////    @Test
////    public void testPreconImplConjXXXWTF() throws Narsese.NarseseException {
////
////    /*
////    in: $.50 (a &&+5 b). 1⋈6 %1.0;.90%
////    in: $.50 (b &&+5 c). 6⋈11 %1.0;.90%
////    in: $.50 (c &&+5 d). 11⋈16 %1.0;.90%
////      instability:
////        $.33 ((b &&+5 b) &&+5 #1). 11⋈21 %1.0;.37% {18: 1;2;3;;} ((%1,(%2==>%3),belief(positive),notImpl(%1),time(urgent)),(subIfUnifiesAny(%3,%2,%1),((DeductionRecursive-->Belief),(InductionRecursive-->Goal))))
////            $.63 ((b &&+5 #1) &&+5 d). 6⋈16 %1.0;.82% {13: 1;2;3;;} ((%1,%2,task("."),time(raw),time(dtEventsOrEternals),neqAndCom(%1,%2),notImpl(%1),notImpl(%2)),(varIntro((polarize(%1,task) &&+- polarize(%2,belief))),((IntersectionDepolarized-->Belief))))
////            $.63 (($1 &&+5 d) ==>-10 (b &&+5 $1)). 11 %1.0;.45% {13: 1;2;3;;} ((%1,%2,time(raw),task(positive),task("."),time(dtEventsOrEternals),neqAndCom(%1,%2),notImpl(%1),notImpl(%2)),(varIntro((%1 ==>+- %2)),((Induction-->Belief))))
////     */
////
////        Temporalize t = new Temporalize();
////        t.knowTerm($.$("((b &&+5 #1) &&+5 d)"), 6, 16);
////        t.knowTerm($.$("(($1 &&+5 d) ==>-10 (b &&+5 $1))"), 11);
////
////        Term P = nars.$.$("((b &&+- b) &&+- #1))");
////
////        Temporalize.Event s = t.solve(P);
////        assertEquals("(c ==>-4 (a &&+1 b))@5", s.toString());
////    }
//
//    @Disabled
//    @Test
//    public void testPreconImplConjPreConflict() throws Narsese.NarseseException {
//
//        TreeSet<String> solutions = new TreeSet();
//
//        for (int i = 0; i < 1; i++) {
//            DeriveTime t = new DeriveTime();
//            t.knowAbsolute($("(y ==>+1 z)"), 0);
//            t.knowAbsolute($("(x ==>+2 z)"), 0);
//            Event s = t.solve($("((x &&+- y) ==>+- z)"));
//            if (s != null) {
//                solutions.add(s.toString());
//            }
//        }
//
//        assertEquals("[((x &&+1 y) ==>+1 z)@-1]", solutions.toString());
//    }
//
//    @Test
//    public void testDropAnyEvent0() throws Narsese.NarseseException {
//        //instability:
//        //$.05 ((a-->b) ==>+4 (c-->d)). -3 %1.0;.38% {160: 1;2;3;;} (((%1==>%2),%1,belief("&&")),((dropAnyEvent(%1) ==>+- %2),((StructuralDeduction-->Belief))))
//        //    $.13 (((a-->b) &&+1 (b-->c)) ==>+3 (c-->d)). 1 %1.0;.42% {7: 1;2;3} ((%1,%2,time(raw),task("."),time(dtEvents),notImpl(%1),notImpl(%2)),((%2 ==>+- %1),((Abduction-->Belief))))
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("(((a-->b) &&+1 (b-->c)) ==>+3 (c-->d))"), 1);
//        t.print();
//        Term p = $("((a-->b) ==>+- (c-->d))");
//        Event s = t.solve(p);
//        assertNotNull(s);
//        assertEquals(
//                "((a-->b) ==>+4 (c-->d))@1",
//                s.toString());
//
//    }
//
//    @Test
//    public void testDropAnyEvent1() throws Narsese.NarseseException {
//        testDropAnyEvent("(a &&+5 b)");
//    }
//
//    @Test
//    public void testDropAnyEvent2() throws Narsese.NarseseException {
//        testDropAnyEvent("((a &&+5 b) &&+5 c)");
//    }
//
//    @Test
//    public void testDropAnyEvent3() throws Narsese.NarseseException {
//        testDropAnyEvent("((a &&+5 b) &&+5 (#1 &&+5 d))");
//    }
//
//    @Test
//    public void testDropAnyEvent4() throws Narsese.NarseseException {
//        testDropAnyEvent("((a &&+5 ((--,a)&|b)) &&+5 ((--,b) &&+5 (--,c)))");
//    }
//
//
//    void testDropAnyEvent(String xs) throws Narsese.NarseseException {
//        Set<Term> result = new TreeSet();
//
//        Term x = $(xs);
//        int xdt = x.dtRange();
//
//        FastList<LongObjectPair<Term>> xe = x.eventList();
//        Term first = xe.getFirst().getTwo();
//        Term last = xe.getLast().getTwo();
//
//        for (int i = 0; i < 10 * xe.size(); i++) {
//
//            Term y = $("dropAnyEvent( " + xs + " )").eval(n.terms);
//            int ydt = y.dtRange();
//
//            assertNotEquals(xe, y);
//            assertTrue(ydt <= xdt);
//
//            FastList<LongObjectPair<Term>> ye = y.eventList();
//            if (ye.getFirst().getTwo().equals(first) && ye.getLast().getTwo().equals(last)) {
//                assertEquals(xdt, ydt, y + " has different dt span");
//            }
//
//            //same relative timing
//            for (int j = 1; j < ye.size(); j++) {
//                Term y1 = ye.get(j - 1).getTwo();
//                Term y2 = ye.get(j).getTwo();
//                assertEquals(
//                        y.subTime(y2) - y.subTime(y1),
//                        x.subTime(y2) - x.subTime(y1)
//                );
//            }
//
//            result.add(y);
//        }
//        System.out.println();
//        System.out.println(Joiner.on('\n').join(result));
//        assertEquals(xe.size(), result.size());
//    }
//
//    @Test
//    public void testDropAnyEventInnerSubj() throws Narsese.NarseseException {
//        /*
//        BAD
//        $.02 ((at(SELF,{t001})&|open({t001})) ==>+5 ({t001}-->[opened])). %1.0;.81% {7: 1;;} (((%1==>%2),%1,belief("&&")),((dropAnyEvent(%1) ==>+- %2),((StructuralDeduction-->Belief))))
//            $.50 (((hold(SELF,{t002}) &&+5 at(SELF,{t001})) &&+5 open({t001})) ==>+5 ({t001}-->[opened])). %1.0;.90% {0: 1}
//        */
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("(((hold(SELF,{t002}) &&+5 at(SELF,{t001})) &&+5 open({t001})) ==>+5 ({t001}-->[opened]))"), ETERNAL);
//        t.print();
//
//        Term P = $("((at(SELF,{t001}) &&+- open({t001})) ==>+- ({t001}-->[opened]))");
//
//        Event s = t.solve(P);
//        assertEquals("((at(SELF,{t001}) &&+5 open({t001})) ==>+5 ({t001}-->[opened]))", s.term.toString());
//    }
//
//    @Test
//    public void testDropAnyEventInnerPred() throws Narsese.NarseseException {
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("(({t001}-->[opened]) ==>+5 ((hold(SELF,{t002}) &&+5 at(SELF,{t001})) &&+5 open({t001})))"), ETERNAL);
//        Event s = t.solve($("(({t001}-->[opened]) ==>+- (at(SELF,{t001}) &&+- open({t001})))"));
//        assertEquals("(({t001}-->[opened]) ==>+10 (at(SELF,{t001}) &&+5 open({t001})))", s.term.toString());
//    }
//
//    @Test
//    public void testRepeatEvents1() throws Narsese.NarseseException {
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("(x &&+5 x)"), ETERNAL);
//        Event s = t.solve($("(x &&+- x)"));
//        assertNotNull(s);
//        assertEquals("(x &&+5 x)", s.term.toString());
//    }
//
//    @Test
//    public void testRepeatEvents2() throws Narsese.NarseseException {
//        DeriveTime t = new DeriveTime();
//        Term x = $("x");
//        t.knowAbsolute(x, 0);
//        t.knowAbsolute(x, 5);
//        t.print();
//
//        Event s = t.solve($("(x &&+- x)"));
//        assertNotNull(s);
//        assertEquals("(x &&+5 x)", s.term.toString());
//    }
//
//    @Test
//    public void testRepeatEvents3() throws Narsese.NarseseException {
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("(x &&+5 x)"), ETERNAL);
//        t.print();
//        Map h = new HashMap();
//        Event s = t.solve($("((x &&+2 #1) &&+- x)"), h);
//        assertNotNull(s);
//        assertEquals("((x &&+2 #1) &&+3 x)", s.term.toString());
//    }
//
//    @Test
//    public void testDropAnyEvent23423423() throws Narsese.NarseseException {
//        /* bad:
//         $.07 ((a,b) ==>+1 (b,c)). 0 %1.0;.38% {59: 1;2;3;;} (((%1==>%2),%2,belief("&&")),((%1 ==>+- dropAnyEvent(%2)),((StructuralDeduction-->Belief))))
//               $.13 ((a,b) ==>+1 ((b,c) &&+3 (c,d))). 1
//        */
//        DeriveTime t = new DeriveTime();
//        t.knowAbsolute($("((a,b) ==>+1 ((b,c) &&+3 (c,d)))"), 1);
//        {
//            Map h = new HashMap();
//            Event s = t.solve($("((a,b) ==>+- (b,c))"), h);
//            assertNotNull(s);
//            assertEquals("((a,b) ==>+1 (b,c))", s.term.toString());
//        }
//        Map h = new HashMap();
//        Event s = t.solve($("((a,b) ==>+- (c,d))"), h);
//        assertNotNull(s);
//        assertEquals("((a,b) ==>+4 (c,d))", s.term.toString());
//        //assertEquals(1, s.start(h).abs());
//
//    }
//
//}
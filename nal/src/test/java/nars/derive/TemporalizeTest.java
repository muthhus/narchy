package nars.derive;

import com.google.common.base.Joiner;
import nars.$;
import nars.Narsese;
import nars.term.Term;
import org.junit.Test;

import java.util.HashMap;
import java.util.TreeSet;

import static nars.time.Tense.ETERNAL;
import static org.junit.Assert.*;

public class TemporalizeTest {

    @Test
    public void testEventize1() throws Narsese.NarseseException {

        assertEquals("b@0,b@0->a,a@0,a@0->b,(a&&b)@0", new Temporalize()
                .knowTerm($.$("(a && b)"), 0).toString());

        assertEquals("b@0->a,b@0->(a&&b),a@0->b,a@0->(a&&b),(a&&b)@ETE", new Temporalize()
                .knowTerm($.$("(a && b)"), ETERNAL).toString());

        assertEquals("b@0,b@0->a,a@0,a@0->b,(a&|b)@0", new Temporalize()
                .knowTerm($.$("(a &| b)"), 0).toString());
    }

    @Test
    public void testEventize3() throws Narsese.NarseseException {

        assertEquals("(x)@0,(x)@-1->(y),((x) &&+1 (y))@[0..1],((x) &&+1 (y))@[-2..-1]->(z),(((x) &&+1 (y)) &&+1 (z))@[0..2],(z)@2,(z)@2->((x) &&+1 (y)),(y)@1,(y)@1->(x)", new Temporalize()
                .knowTerm($.$("(((x) &&+1 (y)) &&+1 (z))"), 0).toString());
    }

    @Test
    public void testEventize2() throws Narsese.NarseseException {

        assertEquals("b@5,b@5->a,(a &&+5 b)@[0..5],a@0,a@-5->b", new Temporalize()
                .knowTerm($.$("(a &&+5 b)"), 0).toString());
    }

    @Test
    public void testEventize2b() throws Narsese.NarseseException {

        assertEquals("b@5->a,b@5->(a &&+5 b),(a &&+5 b)@ETE,a@-5->b,a@0->(a &&+5 b)", new Temporalize()
                .knowTerm($.$("(a &&+5 b)"), ETERNAL).toString());


        Temporalize t = new Temporalize().knowTerm($.$("(a &&+2 (b &&+2 c))"), 0);
        assertEquals("((a &&+2 b) &&+2 c)@[0..4],b@2,b@2->a,a@0,a@-2->b,(a &&+2 b)@[0..2],(a &&+2 b)@[-4..-2]->c,c@4,c@4->(a &&+2 b)", t.toString());


        assertEquals("b@2,b@2->a,a@0,a@-2->b,(a ==>+2 b)@0",
                new Temporalize().knowTerm($.$("(a ==>+2 b)"), 0).toString());
        assertEquals("b@-2,b@-2->a,(a ==>-2 b)@0,a@0,a@2->b",
                new Temporalize().knowTerm($.$("(a ==>-2 b)"), 0).toString());
    }

    @Test
    public void testEventizeEqui() throws Narsese.NarseseException {
        assertEquals("b@2,b@2->a,a@0,a@-2->b,(a <=>+2 b)@0",
                new Temporalize().knowTerm($.$("(a <=>+2 b)"), 0).toString());
    }

    @Test
    public void testEventizeEquiReverse() throws Narsese.NarseseException {
        assertEquals("b@0,b@-2->a,(b <=>+2 a)@0,a@2,a@2->b",
                new Temporalize().knowTerm($.$("(a <=>-2 b)"), 0).toString());
    }

    @Test
    public void testEventizeImplConj() throws Narsese.NarseseException {
        assertEquals("((a &&+2 b) ==>+3 c)@0,b@2,b@2->a,a@0,a@-2->b,(a &&+2 b)@[0..2],(a &&+2 b)@[-5..-3]->c,c@5,c@5->(a &&+2 b)",
                new Temporalize().knowTerm($.$("((a &&+2 b) ==>+3 c)"), 0).toString());
    }

    @Test
    public void testEventizeCrossDir() throws Narsese.NarseseException {

        //cross directional
        assertEquals("b@2,b@2->a,((a &&+2 b) ==>-3 c)@0,a@0,a@-2->b,(a &&+2 b)@[0..2],(a &&+2 b)@[1..3]->c,c@-1,c@-1->(a &&+2 b)",
                new Temporalize().knowTerm($.$("((a &&+2 b) ==>-3 c)"), 0).toString());
        assertEquals("b@2->a,b@2->((a &&+2 b) ==>-3 c),((a &&+2 b) ==>-3 c)@ETE,a@-2->b,a@0->((a &&+2 b) ==>-3 c),(a &&+2 b)@[1..3]->c,(a &&+2 b)@[0..2]->((a &&+2 b) ==>-3 c),c@-1->(a &&+2 b),c@-1->((a &&+2 b) ==>-3 c)",
                new Temporalize().knowTerm($.$("((a &&+2 b) ==>-3 c)"), ETERNAL).toString());
    }

    @Test
    public void testSolveTermSimple() throws Narsese.NarseseException {

        Temporalize t = new Temporalize();
        t.knowTerm($.$("a"), 1);
        t.knowTerm($.$("b"), 3);
        assertEquals("(a &&+2 b)@[1..3]", t.solve($.$("(a &&+- b)")).toString());
        assertEquals("(a &&+2 b)@[1..3]", t.solve($.$("(b &&+- a)")).toString());

    }

    @Test
    public void testSolveIndirect() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.knowTerm($.$("(a ==>+1 c)"), ETERNAL);
        t.knowTerm($.$("a"), 0);
        Temporalize.Event s = t.solve($.$("c"));
        assertNotNull(s);
        assertEquals("c@1->a", s.toString());
    }

    @Test
    public void testSolveIndirect2() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.knowTerm($.$("(b ==>+1 c)"), ETERNAL);
        t.knowTerm($.$("(a ==>+1 b)"), 0);
        t.knowTerm($.$("c"), 2);

        Temporalize.Event s = t.solve($.$("a"));
        assertNotNull(s);
        assertEquals("a@0", s.toString());
    }

    @Test
    public void testSolveEternalButRelative() throws Narsese.NarseseException {
        /*
                .believe("(x ==>+2 y)")
                .believe("(y ==>+3 z)")
                .mustBelieve(cycles, "(x ==>+5 z)", 1.00f, 0.81f);
                */
        Temporalize t = new Temporalize();
        t.knowTerm($.$("(x ==>+2 y)"), ETERNAL);
        t.knowTerm($.$("(y ==>+3 z)"), ETERNAL);

        Temporalize.Event s = t.solve($.$("(x ==>+- z)"));
        assertNotNull(s);
        assertEquals("(x ==>+5 z)@ETE", s.toString());
    }

    @Test
    public void testSolveRecursiveConjDecomposition() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.knowTerm($.$("((x &&+1 y) &&+1 z)"), 0);

        assertEquals("x@0", t.solve($.$("x")).toString());
        assertEquals("y@1", t.solve($.$("y")).toString());
        assertEquals("z@2", t.solve($.$("z")).toString());

        assertEquals("(x &&+2 z)@[0..2]", t.solve($.$("(x &&+- z)")).toString());
        assertEquals("(x &&+1 y)@[0..1]", t.solve($.$("(x &&+- y)")).toString());
        assertEquals("(y &&+1 z)@[1..2]", t.solve($.$("(y &&+- z)")).toString());

        assertEquals("(x ==>+2 z)@0", t.solve($.$("(x ==>+- z)")).toString());
        assertEquals("(x ==>+1 y)@0", t.solve($.$("(x ==>+- y)")).toString());
        assertEquals("(y ==>+1 z)@1", t.solve($.$("(y ==>+- z)")).toString());
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
        t.knowTerm($.$("(a-->b)"), 10, 20); //these two overlap, so there should be a derivation
        t.knowTerm($.$("(c-->b)"), 5, 25);

        Term st = nars.$.$("(a-->c)");
        Temporalize.Event solution = t.solve(st);
        assertNotNull(solution);
        assertEquals("(a-->c)@[10..20]", solution.toString());
        assertNull("d not covered by known events", t.solve($.$("(a-->d)")));
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
        t.knowTerm($.$("(a-->b)"), 10, 15); //these two overlap, so there should be a derivation
        t.knowTerm($.$("(c-->b)"), 1, 5);

        Temporalize.Event solution = t.solve($.$("(a-->c)"));
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
        t.knowTerm($.$("(a-->b)"), 10, 15); //these two overlap, so there should be a derivation
        t.knowTerm($.$("(c-->b)"), 1, 5);

        Temporalize.Event solution = t.solve($.$("(a-->c)"));
        assertNull(solution);
    }

    @Test
    public void testStatementEventsOneEternal() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.knowTerm($.$("(a-->b)"), ETERNAL); //these two overlap, so there should be a derivation
        t.knowTerm($.$("(c-->b)"), 5, 25);

        Temporalize.Event solution = t.solve($.$("(a-->c)"));
        assertNotNull(solution);
        assertEquals("(a-->c)@[5..25]", solution.toString());
    }

    @Test
    public void testSolveEternalButRelative2() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();

        //  b ==>+10 c ==>+20 e

        t.knowTerm($.$("(b ==>+10 c)"), ETERNAL);
        t.knowTerm($.$("(e ==>-20 c)"), ETERNAL);

//        System.out.println( Joiner.on('\n').join( t.constraints.entrySet() ) );

        HashMap h = new HashMap();
        Temporalize.Event s = t.solve($.$("(b ==>+- e)"), h);

//        System.out.println();
//        System.out.println( Joiner.on('\n').join( h.entrySet() ) );

        assertNotNull(s);
        assertEquals("(b ==>+30 e)@ETE", s.toString());
    }

    @Test
    public void testSolveConjSequenceMerge() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();

        String A = "((a &&+3 c) &&+4 e)";
        t.knowTerm($.$(A), 1);
        String B = "b";
        t.knowTerm($.$(B), 4);

//        System.out.println( Joiner.on('\n').join( t.constraints.entrySet() ) );

        HashMap h = new HashMap();
        Temporalize.Event s = t.solve($.$("(" + A + " &&+- " + B + ")"), h);

//        System.out.println();
//        System.out.println( Joiner.on('\n').join( h.entrySet() ) );

        assertNotNull(s);
        assertEquals("((a &&+3 (b&|c)) &&+4 e)@[1..8]", s.toString());
    }

    @Test
    public void testRecursiveSolution1() throws Narsese.NarseseException {
        /*
                 believe("(x ==>+5 z)")
                .believe("(y ==>+3 z)")
                .mustBelieve(cycles, "( (x &&+2 y) ==>+3 z)", 1f, 0.81f)
         */
        Temporalize t = new Temporalize();
        t.knowTerm($.$("(x ==>+5 z)"), ETERNAL);
        t.knowTerm($.$("(y ==>+3 z)"), ETERNAL);

        System.out.println(Joiner.on('\n').join(t.constraints.entrySet()));

        {
            HashMap h = new HashMap();
            Temporalize.Event s = t.solve($.$("(x &&+- y)"), h);
            assertEquals("(x &&+2 y)@ETE", s.toString());
        }

        //try for both impl and conj, they should produce similar results
        for (String op : new String[]{"==>", "&&"}) {
            HashMap h = new HashMap();
            Temporalize.Event s = t.solve($.$("((x &&+- y) " + op + "+- z)"), h);

            System.out.println();
            System.out.println(Joiner.on('\n').join(h.entrySet()));

            String pattern = "((x &&+2 y) " + op + "+3 z)@ETE";

            assertNotNull(op + ":" + pattern, s);
            assertEquals(op + ":" + pattern, pattern, s.toString());
        }

    }

    @Test
    public void testImplToEquiCircularity() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.knowTerm($.$("(x ==>+5 y)"), ETERNAL);
        t.knowTerm($.$("(y ==>-5 x)"), ETERNAL);
        assertEquals("(x <=>+5 y)@ETE", t.solve($.$("(x <=>+- y)")).toString());

        //    @Test public void testImplToEquiCircularityAvg() throws Narsese.NarseseException {
        //        Temporalize t = new Temporalize();
        //        t.knowTerm($.$("(x ==>+6 y)"), ETERNAL);
        //        t.knowTerm($.$("(y ==>-4 x)"), ETERNAL);
        //        assertEquals("(x <=>+5 y)@ETE", t.solve($.$("(x <=>+- y)")).toString());
        //    }

    }

    @Test
    public void testPreconImplConjPreConflict() throws Narsese.NarseseException {

        TreeSet<String> solutions = new TreeSet();

        //multiple solutions since x is specified to occurr at 0, yet y also is so x also occurs at -1

        for (int i = 0; i < 100; i++) {
            Temporalize t = new Temporalize();
            t.knowTerm($.$("(y ==>+1 z)"), 0);
            t.knowTerm($.$("(x ==>+2 z)"), 0);
            Temporalize.Event s = t.solve($.$("((x &&+- y) ==>+- z)"));
            if (s != null) {
                assertNotNull(s);
                solutions.add(s.toString());
            }
        }

        //assertEquals(2, solutions.size());
        assertEquals("[((x &&+1 y) ==>+1 z)@-1, ((x&|y) ==>+1 z)@0]", solutions.toString());
    }

//    @Test
//    public void testUnsolveableTerm() throws Narsese.NarseseException {
//        Temporalize t = new Temporalize();
//        t.add(new Settings() {
//            @Override
//            public boolean debugPropagation() {
//                return true;
//            }
//
////            @Override
////            public boolean warnUser() {
////                return true;
////            }
//
//            @Override
//            public boolean checkModel(Solver solver) {
//                return ESat.TRUE.equals(solver.isSatisfied());
//            }
//        });
//
//        t.know($.$("a"), 1, 1);
//
//        //"b" is missing any temporal basis
//        assertEquals( "(a &&+- b)", t.solve($.$("(a &&+- b)")).toString() );
//
//
////        assertEquals("",
////                t.toString());
//
//    }

}
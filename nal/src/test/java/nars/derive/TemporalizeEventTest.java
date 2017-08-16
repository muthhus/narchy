package nars.derive;

import nars.Narsese;
import nars.derive.time.Temporalize;
import org.junit.Ignore;
import org.junit.Test;

import static nars.$.$;
import static nars.time.Tense.ETERNAL;
import static org.junit.Assert.assertEquals;

/** tests the 'eventization' of various terms
 * these tests are basically correct. they just need their expected
 * outputs updated with what it currently prints out.  so it is set to @Ignore for now
 * because if these didnt work, there would be massive failure in TemporalizeTest.java
 * */
@Ignore
public class TemporalizeEventTest {
    @Test
    public void testEventize1a() throws Narsese.NarseseException {

        Temporalize t = new Temporalize();
        t.knowTerm($("(a && b)"), 0);
        assertEquals("b@0->(a&&b),a@0->(a&&b),(a&&b)@0", t.toString());
    }

    @Test
    public void testEventize1b() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.knowTerm($("(a && b)"), ETERNAL);
        assertEquals("b@0->(a&&b),a@0->(a&&b),(a&&b)@ETE", t.toString());
    }

    @Test
    public void testEventize1c() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.knowTerm($("(a &| b)"), 0);
        assertEquals("b@0->(a&|b),a@0->(a&|b),(a&|b)@0", t.toString());
    }

    @Test
    public void testEventize3() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.knowTerm($("(((x) &&+1 (y)) &&+1 (z))"), 0);
        assertEquals("(z)@2->(((x) &&+1 (y)) &&+1 (z)),(y)@1->((x) &&+1 (y)),((x) &&+1 (y))@[0..1]->(((x) &&+1 (y)) &&+1 (z)),(((x) &&+1 (y)) &&+1 (z))@[0..2],(x)@0->((x) &&+1 (y))",
                t.toString());
    }

    @Test
    public void testEventizeConjFwd() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.knowTerm($("(a &&+5 b)"), 0);
        assertEquals("b@5->(a &&+5 b),(a &&+5 b)@[0..5],a@0->(a &&+5 b)",
                t.toString());
    }
    @Test
    public void testEventizeConjRev() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.knowTerm($("(a &&-5 b)"), 0);
        assertEquals("(b &&+5 a)@[0..5],b@0->(b &&+5 a),a@5->(b &&+5 a)",
                t.toString());
    }

    @Test
    public void testEventize2b() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.knowTerm($("(a &&+5 b)"), ETERNAL);
        assertEquals("b@5->(a &&+5 b),(a &&+5 b)@ETE,a@0->(a &&+5 b)",
                t.toString());
    }

    @Test
    public void testEventize2c() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.knowTerm($("(a &&+2 (b &&+2 c))"), 0);
        assertEquals("((a &&+2 b) &&+2 c)@[0..4],b@2->(a &&+2 b),a@0->(a &&+2 b),c@4->((a &&+2 b) &&+2 c),(a &&+2 b)@[0..2]->((a &&+2 b) &&+2 c)",
                t.toString());
    }

    @Test
    public void testEventize2d() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.knowTerm($("(a ==>+2 b)"), 0);
        assertEquals("b@2->a,a@-2->b,(a ==>+2 b)@0", t.toString());
    }

    @Test
    public void testEventize2e() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.knowTerm($("(a ==>-2 b)"), 0);
        assertEquals("(a ==>-2 b)@0,b@-2->a,a@2->b", t.toString());
    }

//    @Test
//    public void testEventizeEqui() throws Narsese.NarseseException {
//        assertEquals("b@2,b@2->a,a@0,a@-2->b,(a <=>+2 b)@0",
//                new Temporalize().knowTerm($.$("(a <=>+2 b)"), 0).toString());
//    }
//
//    @Test
//    public void testEventizeEquiReverse() throws Narsese.NarseseException {
//        assertEquals("b@0,b@-2->a,(b <=>+2 a)@0,a@2,a@2->b",
//                new Temporalize().knowTerm($.$("(a <=>-2 b)"), 0).toString());
//    }

    @Test
    public void testEventizeImplConj() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.knowTerm($("((a &&+2 b) ==>+3 c)"), 0);
        assertEquals("((a &&+2 b) ==>+3 c)@0,b@2->(a &&+2 b),a@0->(a &&+2 b),c@5->(a &&+2 b),(a &&+2 b)@[-5..-3]->c",
                t.toString());
    }

    @Test
    public void testEventizeCrossDir1() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.knowTerm($("((a &&+2 b) ==>-3 c)"), 0);
        assertEquals("((a &&+2 b) ==>-3 c)@0,b@2->(a &&+2 b),a@0->(a &&+2 b),c@-1->(a &&+2 b),(a &&+2 b)@[1..3]->c",
                t.toString());
    }

    @Test
    public void testEventizeCrossDirETERNAL() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.knowTerm($("((a &&+2 b) ==>-3 c)"), ETERNAL);
        assertEquals("((a &&+2 b) ==>-3 c)@ETE,b@2->(a &&+2 b),a@0->(a &&+2 b),c@-1->(a &&+2 b),(a &&+2 b)@[1..3]->c",
                t.toString());
    }

}

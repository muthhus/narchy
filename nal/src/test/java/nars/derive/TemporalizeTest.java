package nars.derive;

import nars.$;
import nars.Narsese;

import org.junit.Test;

import static nars.time.Tense.ETERNAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TemporalizeTest {

    @Test
    public void testEventize() throws Narsese.NarseseException {


        assertEquals("{b=b@0, a=a@0, (a&&b)=(a&&b)@0}", new Temporalize()
                .knowTerm($.$("(a && b)"), 0).toString());
        assertEquals("{b=b@0->(a&&b)@ETE, a=a@0->(a&&b)@ETE, (a&&b)=(a&&b)@0->(a&&b)@ETE}", new Temporalize()
                .knowTerm($.$("(a && b)"), ETERNAL).toString());

        assertEquals("{b=b@0, a=a@0, (a&|b)=(a&|b)@0}", new Temporalize()
                .knowTerm($.$("(a &| b)"), 0).toString());

        assertEquals("{b=b@5, (a &&+5 b)=(a &&+5 b)@[0..5], a=a@0}", new Temporalize()
                .knowTerm($.$("(a &&+5 b)"), 0).toString());
        assertEquals("{b=b@5->(a &&+5 b)@ETE, (a &&+5 b)=(a &&+5 b)@[0..5]->(a &&+5 b)@ETE, a=a@0->(a &&+5 b)@ETE}", new Temporalize()
                .knowTerm($.$("(a &&+5 b)"), ETERNAL).toString());


        Temporalize t = new Temporalize().knowTerm($.$("(a &&+2 (b &&+2 c))"));
        assertEquals("{(b &&+2 c)=(b &&+2 c)@[2..4], b=b@2, (a &&+2 (b &&+2 c))=(a &&+2 (b &&+2 c))@[0..4], a=a@0, c=c@4}", t.toString());


        assertEquals("{b=b@2, (a ==>+2 b)=(a ==>+2 b)@0, a=a@0}",
                new Temporalize().knowTerm($.$("(a ==>+2 b)")).toString());
        assertEquals("{b=b@-2, (a ==>-2 b)=(a ==>-2 b)@0, a=a@0}",
                new Temporalize().knowTerm($.$("(a ==>-2 b)")).toString());
        assertEquals("{b=b@2, a=a@0, (a <=>+2 b)=(a <=>+2 b)@0}",
                new Temporalize().knowTerm($.$("(a <=>+2 b)")).toString());
        assertEquals("{b=b@0, (b <=>+2 a)=(b <=>+2 a)@0, a=a@2}",
                new Temporalize().knowTerm($.$("(a <=>-2 b)")).toString());

        assertEquals("{((a &&+2 b) ==>+3 c)=((a &&+2 b) ==>+3 c)@0, b=b@2, (a &&+2 b)=(a &&+2 b)@[0..2], a=a@0, c=c@5}",
                new Temporalize().knowTerm($.$("((a &&+2 b) ==>+3 c)")).toString());

        //cross directional
        assertEquals("{b=b@2, ((a &&+2 b) ==>-3 c)=((a &&+2 b) ==>-3 c)@0, (a &&+2 b)=(a &&+2 b)@[0..2], a=a@0, c=c@-1}",
                new Temporalize().knowTerm($.$("((a &&+2 b) ==>-3 c)"), 0).toString());
        assertEquals("{b=b@2->((a &&+2 b) ==>-3 c)@ETE, ((a &&+2 b) ==>-3 c)=((a &&+2 b) ==>-3 c)@0->((a &&+2 b) ==>-3 c)@ETE, (a &&+2 b)=(a &&+2 b)@[0..2]->((a &&+2 b) ==>-3 c)@ETE, a=a@0->((a &&+2 b) ==>-3 c)@ETE, c=c@-1->((a &&+2 b) ==>-3 c)@ETE}",
                new Temporalize().knowTerm($.$("((a &&+2 b) ==>-3 c)"), ETERNAL).toString());
    }

//    @Test
//    public void testSolveTerm() throws Narsese.NarseseException {
//
//        {
//            Temporalize t = new Temporalize();
//            t.know($.$("a"), 1, 1);
//            t.know($.$("b"), 3, 3);
//            assertEquals("(a &&+2 b)", t.solve($.$("(a &&+- b)")).toString());
//        }
//        {
//            Temporalize t = new Temporalize();
//            t.know($.$("a"), 1, 1);
//            t.know($.$("b"), 3, 3);
//            assertEquals("(a &&+2 b)", t.solve($.$("(b &&+- a)")).toString());
//        }
//    }

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
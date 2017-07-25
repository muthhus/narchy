package nars.derive;

import nars.$;
import nars.Narsese;
import nars.term.Term;
import org.chocosolver.solver.Settings;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.explanations.Explanation;
import org.chocosolver.util.ESat;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TemporalizeTest {

    @Test
    public void testEventize() throws Narsese.NarseseException {


        assertEquals("(a&&b) = 0,a = 0,b = 0", new Temporalize()
                .eventize($.$("(a && b)")).toString());

        assertEquals("(a&|b) = 0,a = 0,b = 0", new Temporalize()
                .eventize($.$("(a &| b)")).toString());

        assertEquals("(a &&+5 b) = [0,5],a = 0,b = 5", new Temporalize()
                .eventize($.$("(a &&+5 b)")).toString());


        Temporalize t = new Temporalize().eventize($.$("(a &&+2 (b &&+2 c))"));
        assertEquals("(a &&+2 (b &&+2 c)) = [0,4],a = 0,(b &&+2 c) = [2,4],b = 2,c = 4", t.toString());


        assertEquals("(a ==>+2 b) = 0,a = 0,b = 2",
                new Temporalize().eventize($.$("(a ==>+2 b)")).toString());
        assertEquals("(a ==>-2 b) = 0,a = 0,b = -2",
                new Temporalize().eventize($.$("(a ==>-2 b)")).toString());
        assertEquals("(a <=>+2 b) = 0,a = 0,b = 2",
                new Temporalize().eventize($.$("(a <=>+2 b)")).toString());
        assertEquals("(b <=>+2 a) = 0,b = 0,a = 2",
                new Temporalize().eventize($.$("(a <=>-2 b)")).toString());

        assertEquals("((a &&+2 b) ==>+3 c) = 0,(a &&+2 b) = [0,2],a = 0,b = 2,c = 5",
                new Temporalize().eventize($.$("((a &&+2 b) ==>+3 c)")).toString());

        //cross directional
        assertEquals("((a &&+2 b) ==>-3 c) = 0,(a &&+2 b) = [0,2],a = 0,b = 2,c = -1",
                new Temporalize().eventize($.$("((a &&+2 b) ==>-3 c)")).toString());

    }

    @Test
    public void testSolveTerm() throws Narsese.NarseseException {

        {
            Temporalize t = new Temporalize();
            t.known($.$("a"), 1, 1);
            t.known($.$("b"), 3, 3);
            assertEquals("(a &&+2 b)", t.solve($.$("(a &&+- b)")).toString());
        }
        {
            Temporalize t = new Temporalize();
            t.known($.$("a"), 1, 1);
            t.known($.$("b"), 3, 3);
            assertEquals("(a &&+2 b)", t.solve($.$("(b &&+- a)")).toString());
        }
    }

    @Test
    public void testUnsolveableTerm() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();
        t.set(new Settings() {
            @Override
            public boolean debugPropagation() {
                return true;
            }

//            @Override
//            public boolean warnUser() {
//                return true;
//            }

            @Override
            public boolean checkModel(Solver solver) {
                return ESat.TRUE.equals(solver.isSatisfied());
            }
        });

        t.known($.$("a"), 1, 1);

        //"b" is missing any temporal basis
        assertEquals( "(a &&+- b)", t.solve($.$("(a &&+- b)")).toString() );


//        assertEquals("",
//                t.toString());

    }

}
package nars.derive;

import nars.$;
import nars.Narsese;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.explanations.Explanation;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
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
    public void testXternal() throws Narsese.NarseseException {
        Temporalize t = new Temporalize();

        t.known($.$("a"), 1, 1);
        t.known($.$("b"), 3, 3);
        t.solve($.$("(a &&+- b)"));


//        assertEquals("",
//                t.toString());

    }

}
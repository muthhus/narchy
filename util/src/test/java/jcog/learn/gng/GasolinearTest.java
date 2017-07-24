package jcog.learn.gng;

import org.junit.Test;

import static java.lang.System.out;
import static org.junit.Assert.assertEquals;

public class GasolinearTest {

    @Test
    public void testSimpleDiscretization() {

        Gasolinear g = Gasolinear.of(3,

     0.1, 0.2, 0.3, 0.25, 0.14, 0.14,

            0.66, 0.67, 0.68,

            1f, 0.8f, 0.95f, 0.83f
        );
        out.println(g);

        assertEquals(0, g.which(-0.1));
        assertEquals(0, g.which(0.1));
        assertEquals(0, g.which(0.2));

        assertEquals(1, g.which(0.5));
        assertEquals(1, g.which(0.6));

        assertEquals(2, g.which(0.9));
        assertEquals(2, g.which(1.1));
    }
}
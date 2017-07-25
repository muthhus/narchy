package nars.derive;

import nars.$;
import nars.Narsese;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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


        assertEquals("(a ==>+2 b) = 0,a = 0,b = 2", new Temporalize().eventize($.$("(a ==>+2 b)")).toString());
        assertEquals("(a ==>-2 b) = 0,a = 0,b = -2", new Temporalize().eventize($.$("(a ==>-2 b)")).toString());
        assertEquals("(a <=>+2 b) = 0,a = 0,b = 2", new Temporalize().eventize($.$("(a <=>+2 b)")).toString());
        assertEquals("(b <=>+2 a) = 0,b = 0,a = 2", new Temporalize().eventize($.$("(a <=>-2 b)")).toString());


    }
}
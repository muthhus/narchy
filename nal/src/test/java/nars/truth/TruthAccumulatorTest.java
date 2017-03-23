package nars.truth;

import nars.$;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by me on 3/22/17.
 */
public class TruthAccumulatorTest {

    @Test
    public void test1() {
        TruthAccumulator a = new TruthAccumulator();
        assertNull(a.commitAverage());
        a.add($.t(0, 0.5f));
        assertEquals("%0.0;.50%", a.peekAverage().toString());
        assertEquals("%0.0;.50%", a.peekSum().toString());
        a.add($.t(1f, 0.5f));
        assertEquals("%.50;.50%", a.peekAverage().toString());
        assertEquals("%.50;.67%", a.peekSum().toString());
        a.commit();
        assertNull(a.peekAverage());
        assertNull(a.commitAverage());
    }
}
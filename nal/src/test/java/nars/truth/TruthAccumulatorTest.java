package nars.truth;

import nars.$;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by me on 3/22/17.
 */
public class TruthAccumulatorTest {

    @Test
    public void test1() {
        int dur = 1;
        TruthAccumulator a = new TruthAccumulator();
        assertNull(a.commitAverage(dur));
        a.add($.t(0, 0.5f), dur);
        assertEquals("%0.0;.50%", a.peekAverage(dur).toString());
        assertEquals("%0.0;.50%", a.peekSum(dur).toString());
        a.add($.t(1f, 0.5f), dur);
        assertEquals("%.50;.50%", a.peekAverage(dur).toString());
        assertEquals("%.50;.67%", a.peekSum(dur).toString());
        a.commit();
        assertNull(a.peekAverage(dur));
        assertNull(a.commitAverage(dur));
    }
}
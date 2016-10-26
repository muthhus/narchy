package nars.term;

import nars.IO;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by me on 10/26/16.
 */
public class JSONTest {

    @Test
    public void testJSON1() {
        Term t = IO.fromJSON("{ \"a\": [1, 2], \"b\": \"x\", \"c\": { \"d\": 1 } }");
        assertEquals("{(\"x\"-->b),a(1,2),({(1-->d)}-->c)}", t.toString());
    }
}

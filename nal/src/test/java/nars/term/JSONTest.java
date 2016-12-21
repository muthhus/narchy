package nars.term;

import nars.IO;
import nars.nar.Terminal;
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
    @Test
    public void testJSON2() {
        assertEquals("{a(1,2)}", IO.fromJSON("{ \"a\": [1, 2] }").toString());
    }

    @Test
    public void testJSONTermFunction() {

        Term u = new Terminal().term("json(\"{ \"a\": [1, 2] }\")").term();
        assertEquals("{a(1,2)}", u.toString());
        //assertEquals("(&,(\"x\"-->b),((1-->d)-->c),a(1,2))", t.toString());
    }
}

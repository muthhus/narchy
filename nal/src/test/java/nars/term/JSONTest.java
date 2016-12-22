package nars.term;

import nars.IO;
import nars.nar.Terminal;
import org.junit.Test;

import static nars.$.$;
import static org.junit.Assert.assertEquals;

/**
 * Created by me on 10/26/16.
 */
public class JSONTest {

    @Test
    public void testJSON1() {
        Term t = IO.fromJSON("{ \"a\": [1, 2], \"b\": \"x\", \"c\": { \"d\": 1 } }");
        assertEquals($("{(\"x\"-->b),a(1,2),({(1-->d)}-->c)}").toString(), t.toString());
    }

    @Test
    public void testJSON2() {
        assertEquals("{a(1,2)}", IO.fromJSON("{ \"a\": [1, 2] }").toString());
    }

    @Test
    public void testParseJSONTermFunction() {
        Term u = new Terminal().inputAndGet("jsonParse(\"{ \"a\": [1, 2] }\").").term();
        assertEquals("{a(1,2)}", u.toString());
    }
    @Test
    public void testToStringJSONTermFunction() {
        Term u = new Terminal().inputAndGet("jsonStringify((x,y,z)).").term();
        assertEquals("json(\"[\"x\",\"y\",\"z\"]\")", u.toString());
    }
}

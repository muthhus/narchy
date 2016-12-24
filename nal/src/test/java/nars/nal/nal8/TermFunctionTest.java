package nars.nal.nal8;

import nars.IO;
import nars.nar.Default;
import nars.nar.Terminal;
import nars.term.Term;
import org.junit.Test;

import static nars.$.$;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class TermFunctionTest {

    @Test
    public void testImmediateTransformOfInput() { //as opposed to deriver's use of it
        Default d = new Default();


        d.log();
        final boolean[] got = {false};
        d.onTask(t -> {
            String s = t.toString();
            if (s.contains("union"))
               assertTrue(false);
            if (s.contains("[a,b]"))
                got[0] = true;
        });
        d.input("union([a],[b]).");

        d.run(1);

        assertTrue(got[0]);
    }

    @Test
    public void testAdd1() {
        Default d = new Default();
        d.log();
        d.input("add(1,2,#x)!");
        d.run(16);
        d.input("add(4,5,#x)!");
        d.run(16);
    }

    @Test
    public void testAdd1Temporal() {
        Default d = new Default();
        d.log();
        d.input("add(1,2,#x)! :|:");
        d.run(16);
        d.input("add(4,5,#x)! :|:");
        d.run(16);
    }

    @Test
    public void testFunctor1() {
        Default d = new Default();
        d.log();
//        d.input("date(); :|:");
//        d.run(16);
        d.input("(now<->date()). :|:");
        d.run(16);
    }
//    @Test
//    public void testExecutionREsultIsCondition() {
//        Default d = new Default();
//        d.log();
//        d.input("(add(#x,1,#y) <=> inc(#x,#y)).");
//        d.input("add(1,2,#x)! :|:");
//        d.run(16);
//        d.input("add(3,4,#x)! :|:");
//        d.run(16);
//    }

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

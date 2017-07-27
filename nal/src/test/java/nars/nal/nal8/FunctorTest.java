package nars.nal.nal8;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Param;
import nars.test.TestNAR;
import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class FunctorTest {

    @Test
    public void testImmediateTransformOfInput() throws Narsese.NarseseException { //as opposed to deriver's use of it
        NAR d = new NARS().get();


        d.log();
        final boolean[] got = {false};
        d.onTask(t -> {
            String s = t.toString();
            assertFalse(s.contains("union"));
            if (s.contains("[a,b]"))
                got[0] = true;
        });
        d.input("union([a],[b]).");

        d.run(1);

        assertTrue(got[0]);
    }

    @Test
    public void testAdd1() throws Narsese.NarseseException {
        NAR d = new NARS().get();

        d.input("add(1,2,#x)!");
        d.run(16);
        d.input("add(4,5,#x)!");
        d.run(16);
    }

    @Test
    public void testAdd1Temporal() throws Narsese.NarseseException {
        NAR d = new NARS().get();

        d.input("add(1,2,#x)! :|:");
        d.run(16);
        d.input("add(4,5,#x)! :|:");
        d.run(16);
    }

    /** tests correct TRUE fall-through behavior, also backward question triggered execution */
    @Test public void testFunctor1() throws Narsese.NarseseException {
        Param.DEBUG = true;

        TestNAR t = new TestNAR(new NARS().get());
        //t.log();
        t.believe("((complexity($1)<->3)==>c3($1))");
        t.ask("c3(x:y)");
        t.mustBelieve(512, "c3(x:y)", 1f, 0.81f);
        t.run(true);
    }

    @Test
    public void testFunctor2() throws Narsese.NarseseException {
        //Param.DEBUG = true;

        int TIME = 2048;
        TestNAR t = new TestNAR(new NARS().get());

        Param.DEBUG = true; t.log();
        t.believe("(equal(complexity($1),complexity($2)) ==> c({$1,$2}))");
        t.ask("c({x, y})");
        t.ask("c({x, (x)})");
        t.mustBelieve(TIME, "c({x,y})", 1f, 0.81f);
        t.mustBelieve(TIME, "c({x,(x)})", 0f, 0.81f);
        t.run(true);
    }

    @Test
    public void testExecutionResultIsCondition() throws Narsese.NarseseException {
        NAR d = NARS.tmp();
        d.log();
        d.input("(add($x,1) <=> inc($x)).");
        d.input("(inc(1) <=> two).");
        d.run(128);
        d.input("(inc(two) <=> ?x)?");
    }

//    @Test
//    public void testJSON1() throws Narsese.NarseseException {
//        Term t = IO.fromJSON("{ \"a\": [1, 2], \"b\": \"x\", \"c\": { \"d\": 1 } }");
//        assertEquals($("{(\"x\"-->b),a(1,2),({(1-->d)}-->c)}").toString(), t.toString());
//    }
//
//    @Test
//    public void testJSON2() {
//        assertEquals("{a(1,2)}", IO.fromJSON("{ \"a\": [1, 2] }").toString());
//    }

//    @Test
//    public void testParseJSONTermFunction() throws Narsese.NarseseException {
//        Term u = NARS.shell().inputAndGet("fromJSON(\"{ \"a\": [1, 2] }\").").term();
//        assertEquals("{a(1,2)}", u.toString());
//    }
//    @Test
//    public void testToStringJSONTermFunction() throws Narsese.NarseseException {
//        Term u = NARS.shell().inputAndGet("toJSON((x,y,z)).").term();
//        assertEquals("json(\"[\"x\",\"y\",\"z\"]\")", u.toString());
//    }

}

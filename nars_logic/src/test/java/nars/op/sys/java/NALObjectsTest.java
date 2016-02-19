package nars.op.sys.java;

import com.google.common.collect.Lists;
import nars.Global;
import nars.NAR;
import nars.nar.Default;
import nars.term.Term;
import nars.util.signal.EventCount;
import org.junit.Ignore;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.junit.Assert.*;


public class NALObjectsTest  {

    /** test class */
    public static class T {

        public int count = 0;
        public int val = -1;


        public double the() {
            double v = (64 * Math.random());
            val = (int)v;
            return v;
        }

        public void noParamMethodReturningVoid() {
            count++;
            //System.out.println("base call");
            //return Math.random();
        }

        public float mul(float a, float b) {
            count++;
            return a * b;
        }

        @Override
        public String toString() {
            return "T[" + count + ']';
        }

        public List<Method> getClassMethods() {
            Method[] m = getClass().getMethods();
            List<Method> l = Global.newArrayList(m.length);
            for (Method x : m)
                if (Lobjects.isMethodVisible(x))
                    if (!"getClassMethods".equals(x.getName()))
                        l.add(x);
            return l;
        }
    }


    @Test public void testInvocationExternal()  {
        testMethodInvocationAndFeedback(true);
    }

    @Test public void testInvocationInternal()  {
        testMethodInvocationAndFeedback(false);
    }


    /** test that the methods of invoking an instance method are indistinguishable
     * whether it occurred from outside, or from a NAR goal
     *
     * only one invocation and one feedback should occurr
     * regardless of the method and preconditions
     */
    public void testMethodInvocationAndFeedback(boolean external)  {

        Global.DEBUG = true;

        NAR n = new Default(128, 1, 2, 3);

        StringWriter ns = new StringWriter();
        n.log(new PrintWriter(ns));


        //n.log();

        String instance = "o";

        int startSize = n.memory.exe.size();

        Lobjects no = new Lobjects(n);

        T wrapper = no.theOrNull(instance, T.class);

        assertEquals("one ClassOperator registered", 1, n.memory.exe.size() - startSize);

        assertNotEquals(T.class, wrapper.getClass());
        assertEquals(T.class, wrapper.getClass().getSuperclass());

        if (external) {
            //INVOKE EXTERNALLY
            wrapper.mul(2, 3);
        }
        else {
            //INVOKE VOLITIONALLY
            n.input("T(mul, " + instance + ",(2, 3),#x)! :|:");
        }

        AtomicInteger puppets = new AtomicInteger(0);
        AtomicInteger inputs = new AtomicInteger(0);

        n.memory.eventTaskProcess.on(t -> {
            List log = t.log();
            if (log == null)
                return;

            String l = log.toString();
            //boolean hasPuppet = l.contains("Puppet");
            boolean isMultiply = t.toString().contains("mul");
//            if (!external && hasPuppet && t.isGoal() && isMultiply)
//                assertFalse(t + " internal mode registered a Puppet invocation", true);
            boolean hasInput = l.contains("Input");
            if (external && hasInput && t.isGoal() && isMultiply)
                assertFalse(t + " external mode registered a volition invocation", true);
            if (t.isGoal() && isMultiply) {
                //if (hasPuppet) puppets.incrementAndGet();
                if (hasInput) inputs.incrementAndGet();
            }
        });

        n.run(12);

        //assertEquals(0, wrapped.count); //unaffected
        assertEquals(1, wrapper.count); //wrapper fields affected

        //TODO use TestNAR and test for right tense

        String bs = ns.getBuffer().toString();

        System.out.println(bs);


        String invocationGoal = "T(mul,o,(2,3),#1)! :|: %1.0;.90%";
        assertEquals(1, countMatches(bs, invocationGoal));

        String exeuctionNotice = "T(mul,o,(2,3),#1). :|: %1.0;.90%";
        assertEquals(1, countMatches(bs, exeuctionNotice));

        String feedbackResult = "(6-->(/,^T,mul,o,(2,3),_)). :|: %1.0;.90%";
        assertEquals(1, countMatches(bs, feedbackResult));


        if (!external) assertEquals( 1, inputs.get() );
        //else assertEquals(1, puppets.get() );


        if (external) {
            //assertEquals(1, countMatches(bs, invocationGoal + " Puppet"));
        }
        else {
            //assertEquals(1, countMatches(bs, invocationGoal0 + " Input"));
        }

        //TaskProcess: $.50;.50;.95$
        String feedback = "(6-->(/,^T,mul,o,(2,3),_)).";

        System.out.println(bs);

        assertEquals(1, countMatches(bs, feedback));
        //assertEquals(1, countMatches(bs, "Feedback"));

    }


    @Test
    public void testDynamicProxyObjects() throws Exception {


        NAR n = new Default();

        EventCount count = new EventCount(n);

        T tc = new Lobjects(n).the("myJavaObject", T.class);

        tc.noParamMethodReturningVoid();
        assertEquals(6.0, tc.mul(2, 3), 0.001);
        assertNotNull( tc.the() );


        n.run(4);


        assertNotEquals(tc.getClass(), T.class);
        assertTrue(1 <= count.numInputs());


    }


    @Test public void testTermizerPrimitives() {

        testTermizer(null);

        testTermizer(0);
        testTermizer(3.14159);

        testTermizer('a');

        testTermizer("a b c"); //should result in quoted
    }

    @Test public void testTermizerBoxed() {
        testTermizer(1);
        testTermizer(3.14159f);
    }
    @Test public void testTermizerCollections() {
        testTermizer(Lists.newArrayList("x", "y"));
    }
    @Test public void testTermizerArray() {
        testTermizer(new String[] { "x", "y" } );
    }

    @Test public void testMapTermizer() {
        Map map = new HashMap();
        map.put("k1", "v1");
        map.put("k2", "v2");
        testTermizer(map, "{(\"v1\"-->\"k1\"),(\"v2\"-->\"k2\")}");
    }

    static void testTermizer(Object o, String termtoString) {
        DefaultTermizer t = new DefaultTermizer();
        Term term = t.term(o);
        assertNotNull(term);
        assertEquals(termtoString, term.toString(false));
    }

    static void testTermizer(Object o) {
        DefaultTermizer t = new DefaultTermizer();
        Term term = t.term(o);
        assertNotNull(term);
        Object p = t.object(term);

        //System.out.println(t.objects);

        //if (o!=null)
            assertEquals(p, o);
        /*else
            assertNull(p==null ? "('null' value)" : p.getClass().toString(),
                       p);*/


    }

    //TODO
    @Ignore
    @Test public void testOverloadedMethods() throws Exception {
        NAR n = new Default();

        Lobjects no = new Lobjects(n);
        ArrayList nc = no.the("ourList", ArrayList.class);


        //n.stdout();
        nc.add("item");
        //nc.add("x");

        n.run(2);

        nc.toArray();

        nc.size();

        nc.clear();

        nc.size();


        nc.add("item");
        nc.add("item");

        nc.toArray();

        nc.size();



        nc.add(1);
        nc.get(0);
        nc.size();
        nc.clear();

        n.run(50);

    }

    @Test
    public void testLearnMethods() throws Exception {


        NAR n = new Default(512,8,4,2);

        //n.log();

        //EventCount count = new EventCount(n);

        T tc = new Lobjects(n).the("obj", T.class);


        System.out.println( tc.getClassMethods() );


        n.run(16);



    }

}
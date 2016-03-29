package nars.op.java;

import nars.NAR;
import nars.nar.Default;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ExpressionEvaluator;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.assertEquals;


public class javaTest  {

    @Test
    public void testJaninoExpression() throws CompileException, InvocationTargetException {

        //http://unkrig.de/w/Janino#Janino_as_an_Expression_Evaluator

        // Now here's where the story begins...
        ExpressionEvaluator ee = new ExpressionEvaluator();

        // The expression will have two "int" parameters: "a" and "b".
        ee.setParameters(new String[] { "a", "b" }, new Class[] { int.class, int.class });

        // And the expression (i.e. "result") type is also "int".
        ee.setExpressionType(int.class);

        // And now we "cook" (scan, parse, compile and load) the fabulous expression.
        ee.cook("a + b");

        // Eventually we evaluate the expression - and that goes super-fast.
        int result = (Integer) ee.evaluate(new Object[] { 19, 23 });

        assertEquals(42, result);
    }

    @Test public void testClassResolved() {
        //nars.op.sys.java

        NAR d = new Default();
        d.onExec(new java());
        d.input("java(\"nars.op.sys.java.java\", #x)!");
        d.log();
        d.run(5);
        //expect: ("class nars.op.sys.java.java"-->(/,^java,"nars.op.sys.java.java",_)). :|: %1.0;.90% {2+1: 3} Execution Result

    }

    @Test public void testStaticMethodResolved() {
    }

    @Test public void testAmbientExpression() {

        NAR d = new Default();
        d.onExec(new java());


        d.input("java(\"System.currentTimeMillis()\", #x)!");
        d.log();
        d.run(5);

    }
}
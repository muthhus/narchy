package nars.op.sys.java;

import nars.nal.nal8.operator.TermFunction;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermIndex;
import nars.term.atom.Atom;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ExpressionEvaluator;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by me on 2/24/16.
 */
public class java extends TermFunction {



    @Override
    public Object function(Compound args, TermIndex i) {
        //TODO handle multi-arg by returning a tuple of that # elements each processed
        Term x = args.term(0);
        if (x instanceof Atom) {
            String s = Atom.unquote(x);

            Class cl = resolveClass(s);
            if (cl != null)
                return cl;

//            //attempt lookup of method
//            int lastPeriod = s.lastIndexOf('.');
//            if (lastPeriod!=-1) {
//                String actualClass = s.substring(0, lastPeriod);
//                String methodName = s.substring(lastPeriod+1, s.length());
//                cl = resolveClass(actualClass);
//                if (cl!=null) {
//                    return cl; //TODO return the method itself
//                }
//            }

            try {
                return eval(s);
            } catch (Exception e) {
                return e;
            }
        }

        return null;
    }

    protected static Object eval(String expressionString) throws CompileException, InvocationTargetException {
        //http://unkrig.de/w/Janino#Janino_as_an_Expression_Evaluator

        // Now here's where the story begins...
        ExpressionEvaluator ee = new ExpressionEvaluator();

        // The expression will have two "int" parameters: "a" and "b".
        //ee.setParameters(new String[] { "a", "b" }, new Class[] { int.class, int.class });


        // And the expression (i.e. "result") type is also "int".
        ee.setExpressionType(Object.class);

        // And now we "cook" (scan, parse, compile and load) the fabulous expression.
        ee.cook(expressionString);

        // Eventually we evaluate the expression - and that goes super-fast.
        return ee.evaluate(null);
    }
    public
    @Nullable
    static Class resolveClass(String s) {
        Class cl;
        try {
            cl = Class.forName(s);
            return cl;
        } catch (ClassNotFoundException e) {

        }
        return null;
    }
}

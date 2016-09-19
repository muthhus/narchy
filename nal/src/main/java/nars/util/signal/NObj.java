package nars.util.signal;

import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.concept.FuzzyScalarConcepts;
import nars.concept.SensorConcept;
import nars.nar.Default;
import nars.util.math.FloatNormalized;
import ognl.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * generates concepts for reading, writing, and invoking components of a live Java instance
 * via reflection
 */
public class NObj<X> {

    private final X o;


    final Map<String, Object> read = new ConcurrentHashMap();

    final List<SensorConcept> sensors = $.newArrayList();
    private final NAR nar;
    private final String id;

    public NObj(String id, X o, NAR nar) {
        this.id = id;
        this.o = o;
        this.nar = nar;

        //this.type = TypeToken.of(o.getClass());
    }

    public NObj readAllFields(boolean notOnlyPublic) {
        Field[] ff = o.getClass().getDeclaredFields();
        for (Field f : ff) {
            if (notOnlyPublic || Modifier.isPublic(f.getModifiers())) {
                read(f.getName());
            }
        }
        return this;
    }

    public NObj read(String... expr) {
        for (String e : expr)
            read(e);
        return this;
    }

    public NObj read(String expr) {

        read.computeIfAbsent(expr, exp -> {
            try {
                Object x = Ognl.parseExpression(exp);
                Object initialValue = Ognl.getValue(x, o);

                Object y;

                String classString = initialValue.getClass().toString().substring(6);
                switch (classString) {
                    case "java.lang.Double":
                    case "java.lang.Float":
                    case "java.lang.Long":
                    case "java.lang.Integer":
                    case "java.lang.Short":
                    case "java.lang.Byte":
                    case "java.lang.Boolean":
                        y = readNumber(x);
                        break;

                    //TODO String

                    default:
                        throw new RuntimeException("not handled: " + classString);
                }
                /*if (y != null) {
                    System.out.println("read: \t" + o + " " + exp + " " + x + " " + x.getClass() + " " + initialValue + " " + initialValue.getClass());
                }*/
                return y;
            } catch (Exception e1) {
                //e1.printStackTrace();
                System.err.println("\t" + e1);
                return null;
            }
        });
        return this;
    }

    /**
     * generic lowest common denominator numeric input
     */
    private Object readNumber(Object expr) {
        FuzzyScalarConcepts fs = new FuzzyScalarConcepts(

                new FloatNormalized(() -> {
                    try {
                        return ((Number) Ognl.getValue(expr, o, Number.class)).floatValue();
                    } catch (OgnlException e) {
                        e.printStackTrace();
                        return Float.NaN;
                    }
                }), nar, id + '(' + term(expr) + ')'
        ).resolution(0.05f);
        sensors.addAll(fs.sensors);
        return fs;
    }

    private String term(Object expr) {

        if (expr instanceof ASTConst ){

            String ae = expr.toString();
            return ae
                    .substring(1, ae.length()-1); //it's raw field name, wont need quoted

        } else if (expr instanceof ASTStaticMethod) {
            String ae = expr.toString();
            String key = //"\"" +
                    ae.substring(0, ae.indexOf('('));
                    //+ "\"";
            key = key.replace("@", "X");
            //HACK remove the '@' from the key so it doesnt need quoted:
            return key + '(' +
                    term(((ASTStaticMethod) expr).jjtGetChild(0))
                    + ')';
        } else if (expr instanceof SimpleNode) {
            return term((SimpleNode) expr);
        } else {
            //safest for unknown type but semantics are lost
            return "\"" + expr + '"';
        }
    }


    private String term(SimpleNode a) {
        int c = a.jjtGetNumChildren();

        StringBuilder sb = new StringBuilder(16);//.append('(');
        for (int i = 0; i < c; i++) {
            sb.append(term(a.jjtGetChild(i)));
            if (i!=c-1)
                sb.append(',');
        }
        return sb./*.append(')').*/toString();
    }

    public void in(NAgent agent) {
        agent.sensors.addAll(sensors);
        NAgent.logger.info("{} added {}", this, sensors);
    }

    private static class Test1 {
        public float f1 = 1.5f;
        public int i1 = 3;

        public static class Inner {
            public boolean b = true;
        }

        public final Inner inner = new Inner();
    }

    public static void main(String[] args) {
        NAR nar = new Default();
        Test1 t1 = new Test1();
        NObj<Test1> x = new NObj<Test1>("x", t1, nar)
                .readAllFields(true)
                .read("inner.b")
                .read("@Math@sin(f1)");

        System.out.println(x.sensors);
    }
}

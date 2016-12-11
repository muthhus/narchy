package nars;

import jcog.Util;
import jcog.math.FloatSupplier;
import nars.concept.FuzzyScalarConcepts;
import nars.concept.SensorConcept;
import nars.term.Compound;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.function.*;

/**
 * Created by me on 9/30/16.
 */
public interface NSense {

    @NotNull Collection<SensorConcept> sensors();

    NAR nar();


    @NotNull
    default SensorConcept sense(@NotNull String term, @NotNull BooleanSupplier value) {
        return sense(term, () -> value.getAsBoolean() ? 1f : 0f);
    }

    @NotNull
    default SensorConcept sense(@NotNull String term, FloatSupplier value) {
        return sense($.$(term), value);
    }
    @NotNull
    default SensorConcept sense(@NotNull Compound term, FloatSupplier value) {
        return sense(term, value, (v)->$.t(v, alpha()));
    }

    @NotNull
    default SensorConcept sense(@NotNull String term, FloatSupplier value, FloatToObjectFunction<Truth> truthFunc) {
        return sense($.$(term), value, truthFunc);
    }
    @NotNull
    default SensorConcept sense(@NotNull Compound term, FloatSupplier value, FloatToObjectFunction<Truth> truthFunc) {
        return sense(term, value, nar().truthResolution.floatValue(), truthFunc);
    }
    @NotNull
    default SensorConcept sense(@NotNull String term, FloatSupplier value, float resolution, FloatToObjectFunction<Truth> truthFunc) {
        return sense($.$(term), value, resolution, truthFunc);
    }

    @NotNull
    default SensorConcept sense(@NotNull Compound term, FloatSupplier value, float resolution, FloatToObjectFunction<Truth> truthFunc) {
        SensorConcept s = new SensorConcept(term, nar(), value, truthFunc);
        s.resolution(resolution);

        sensors().add(s);
        return s;
    }

    /**
     * learning rate
     */
    default float alpha() {
        return nar().confidenceDefault(Symbols.BELIEF);
    }

    /**
     * interpret an int as a selector between enumerated values
     */
    default <E extends Enum> void senseSwitch(String term, @NotNull Supplier<E> value) {
        E[] values = ((Class<? extends E>) value.get().getClass()).getEnumConstants();
        for (E e : values) {
            String t = switchTerm(term, e.toString());
            sense(t, () -> value.get() == e);
        }
    }

    @NotNull
    static String switchTerm(String term, String e) {
        //return "(" + e + " --> " + term + ")";
        return "(" + term + " , " + e + ")";
    }

    default void senseSwitch(String term, @NotNull IntSupplier value, int min, int max) {
        senseSwitch(term, value, Util.intSequence(min, max));
    }

    /**
     * interpret an int as a selector between (enumerated) integer values
     */
    default void senseSwitch(String term, @NotNull IntSupplier value, @NotNull int[] values) {
        for (int e : values) {
            String t = switchTerm(term, String.valueOf(e));
            sense(t, () -> value.getAsInt() == e);
        }
    }

    /**
     * interpret an int as a selector between (enumerated) object values
     */
    default <O> void senseSwitch(String term, @NotNull Supplier<O> value, @NotNull O... values) {
        for (O e : values) {
            String t = switchTerm(term, "\"" + e.toString() + "\"");
            sense(t, () -> value.get().equals(e));
        }
    }


    default void senseFields(String id, @NotNull Object o) {
        Field[] ff = o.getClass().getDeclaredFields();
        for (Field f : ff) {
            if (Modifier.isPublic(f.getModifiers())) {
                sense(id, o, f.getName());
            }
        }
    }

//    public NObj read(String... expr) {
//        for (String e : expr)
//            read(e);
//        return this;
//    }

    default void sense(String id, Object o, @NotNull String exp) {

//        try {
//            //Object x = Ognl.parseExpression(exp);
//            Object initialValue = Ognl.getValue(exp, o);
//
//
//            String classString = initialValue.getClass().toString().substring(6);
//            switch (classString) {
//                case "java.lang.Double":
//                case "java.lang.Float":
//                case "java.lang.Long":
//                case "java.lang.Integer":
//                case "java.lang.Short":
//                case "java.lang.Byte":
//                case "java.lang.Boolean":
//                    senseNumber(id, o, exp);
//                    break;
//
//                //TODO String
//
//                default:
//                    throw new RuntimeException("not handled: " + classString);
//            }
//                /*if (y != null) {
//                    System.out.println("read: \t" + o + " " + exp + " " + x + " " + x.getClass() + " " + initialValue + " " + initialValue.getClass());
//                }*/
//        } catch (Exception e1) {
//            e1.printStackTrace();
//        }
    }

    @NotNull
    default List<SensorConcept> senseNumber(int from, int to, IntFunction<String> id, IntFunction<FloatSupplier> v) {
        List<SensorConcept> l = $.newArrayList(to-from);
        for (int i = from; i < to; i++) {
            l.addAll( senseNumber(id.apply(i), v.apply(i)).sensors );
        }
        return l;
    }


    @NotNull
    default FuzzyScalarConcepts senseNumber(String id, FloatSupplier v) {
        String[] states = {id};
        return senseNumber(v, states);
    }

    @NotNull
    default FuzzyScalarConcepts senseNumber(FloatSupplier v, String... states) {
        FuzzyScalarConcepts fs = new FuzzyScalarConcepts(
               v, nar(), states
        );//.resolution(0.05f);
        sensors().addAll(fs.sensors);
        return fs;
    }

    @NotNull
    default FuzzyScalarConcepts senseNumberBi(String id, FloatSupplier v) {
        return senseNumber(v,  "hi:" + id, "lo:" + id);
    }
    @NotNull
    default FuzzyScalarConcepts senseNumberTrii(String id, FloatSupplier v) {
        return senseNumber(v,  "hi:" + id, "mid:" + id, "lo:" + id);
    }

    @NotNull
    default FuzzyScalarConcepts senseNumber(String id, DoubleSupplier v) {

        return senseNumber(id, ()->(float)v.getAsDouble());
    }

    /**
     * generic lowest common denominator numeric input
     */
    @NotNull
    default Object senseNumber(String id, Object o, @NotNull String _expr) {
        return null;

        //TODO use Nashorn to evaluate expressions

//        Object expr;
//        try {
//            expr = Ognl.parseExpression(_expr);
//        } catch (OgnlException e) {
//            throw new RuntimeException(e);
//        }
//        FuzzyScalarConcepts fs = new FuzzyScalarConcepts(
//
//                new FloatNormalized(() -> {
//                    try {
//                        Object v = Ognl.getValue(expr, o, Object.class);
//                        if (v instanceof Boolean) {
//                            return (Boolean) v ? 1f : 0f;
//                        } else if (v instanceof Number) {
//                            return ((Number) v).floatValue();
//                        } else {
//                            return Float.NaN; //unknown
//                        }
//                    } catch (OgnlException e) {
//                        e.printStackTrace();
//                        return Float.NaN;
//                    }
//                }), nar(), id + ":(" + term(expr) + ')'
//        );//.resolution(0.05f);
//        sensors().addAll(fs.sensors);
//        return fs;
    }
//
//    @NotNull
//    private static String term(Object expr) {
//
//        if (expr instanceof ASTConst) {
//
//            String ae = expr.toString();
//            return ae
//                    .substring(1, ae.length() - 1); //it's raw field name, wont need quoted
//
//        } else if ((expr instanceof ASTStaticMethod) || (expr instanceof ASTMethod)) {
//            String ae = expr.toString();
//            String key = //"\"" +
//                    ae.substring(0, ae.indexOf('('));
//            //+ "\"";
//            key = key.replace("@", "X");
//            //HACK remove the '@' from the key so it doesnt need quoted:
//
//            return key + '(' +
//                    term((SimpleNode) expr)
//                    + ')';
//        } else if (expr instanceof SimpleNode) {
//            return term((SimpleNode) expr);
//        } else {
//            //safest for unknown type but semantics are lost
//            return "\"" + expr + '"';
//        }
//    }
//
//
//    private static String term(@NotNull SimpleNode a) {
//        int c = a.jjtGetNumChildren();
//
//        StringBuilder sb = new StringBuilder(16);//.append('(');
//        for (int i = 0; i < c; i++) {
//            sb.append(term(a.jjtGetChild(i)));
//            if (i != c - 1)
//                sb.append(',');
//        }
//        return sb./*.append(')').*/toString();
//    }
//

}

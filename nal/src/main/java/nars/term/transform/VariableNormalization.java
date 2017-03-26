package nars.term.transform;

import nars.$;
import nars.term.Compound;
import nars.term.Term;
import nars.term.var.GenericVariable;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Variable normalization
 *
 * Destructive mode modifies the input Compound instance, which is
 * fine if the concept has been created and unreferenced.
 *
 * The term 'destructive' is used because it effectively destroys some
 * information - the particular labels the input has attached.
 *
 */
public class VariableNormalization extends VariableTransform implements Function<Variable,Variable> {

    @NotNull
    public final Map<Variable /* Input Variable */, Variable /*Variable*/> map;

    //private boolean renamed;

    /*public VariableNormalization() {
        this(0);
    }*/

    /** for use with compounds that have exactly one variable */
    public static final VariableTransform singleVariableNormalization = new VariableTransform() {

        @Override
        public Term apply(@Nullable Compound containing, @NotNull Term t) {

            //if (current instanceof Ellipsis)
            //throw new RuntimeException("not allowed");
            //return null;

            if (t instanceof Variable)
                return $.v(t.op(), 1);
            else
                return t;
        }
    };


    @NotNull
    @Override
    public Variable apply(@NotNull Variable x) {
        return newVariable(x, map.size()+1);
    }

    @Override
    public final Term apply(@Nullable Compound ct, @NotNull Term v) {
        if (v instanceof Variable)
            return map.computeIfAbsent((Variable)v, this);
        else
            return v;
    }

    @NotNull
    protected Variable newVariable(@NotNull Variable x, int serial) {
        Variable y;
        if (x instanceof GenericVariable) {
            y = ((GenericVariable) x).normalize(serial); //HACK
        } else {
            y = $.v(x.op(), serial);
            y = y.equals(x) ? x : y; //attempt to use the original if they are equal, this can help prevent unnecessary transforms etc
        }

        return y ;

    }


    public VariableNormalization(int size /* estimate */) {
        this(new HashMap<>(size));
    }

    public VariableNormalization(@NotNull Map<Variable, Variable> r) {
        map = r;

        //NOTE:
        //rename = new ConcurrentHashMap(size); //doesnt work being called recursively
        //rename = new HashMap(size); //doesnt work being called recursively
    }


//    final static Comparator<Map.Entry<Variable, Variable>> comp = new Comparator<Map.Entry<Variable, Variable>>() {
//        @Override
//        public int compare(Map.Entry<Variable, Variable> c1, Map.Entry<Variable, Variable> c2) {
//            return c1.getKey().compareTo(c2.getKey());
//        }
//    };

//    /**
//     * overridden keyEquals necessary to implement alternate variable hash/equality test for use in normalization's variable transform hashmap
//     */
//    static final class VariableMap extends FastPutsArrayMap<Pair<Variable,Term>, Variable> {
//
//
//
//        public VariableMap(int initialCapacity) {
//            super(initialCapacity);
//        }
//
//        @Override
//        public final boolean keyEquals(final Variable a, final Object ob) {
//            if (a == ob) return true;
//            Variable b = ((Variable) ob);
//            return Byted.equals(a, b);
//        }
//
////        @Override
////        public Variable put(Variable key, Variable value) {
////            Variable removed = super.put(key, value);
////            /*if (size() > 1)
////                Collections.sort(entries, comp);*/
////            return removed;
////        }
//    }


}

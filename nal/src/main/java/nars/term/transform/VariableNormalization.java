package nars.term.transform;

import nars.$;
import nars.nal.meta.match.Ellipsis;
import nars.term.Compound;
import nars.term.Termed;
import nars.term.variable.AbstractVariable;
import nars.term.variable.GenericVariable;
import nars.term.variable.Variable;
import nars.util.data.map.UnifriedMap;
import org.jetbrains.annotations.NotNull;

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
    private final UnifriedMap<Variable, Variable /*Variable*/> rename;

    private boolean renamed;

    public VariableNormalization() {
        this(0);
    }



    public VariableNormalization(int size /* estimate */) {
        this(new UnifriedMap<>(size));
    }

    public VariableNormalization(@NotNull UnifriedMap<Variable, Variable> r) {
        rename = r;

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

    /** for use with compounds that have exactly one variable */
    public static final VariableTransform singleVariableNormalization = new VariableTransform() {

        @NotNull @Override
        public Termed apply(Compound containing, @NotNull Variable current) {

            if (current instanceof Ellipsis)
                throw new RuntimeException("not allowed");
                //return null;

            return $.v(current.op(), 1);
        }
    };


    @NotNull
    @Override
    public final Variable apply(@NotNull Variable v) {
        Variable rvv = newVariable(v, rename.size()+1);

        //track if modification occurred
        this.renamed |= (rvv!=v); //!rvv.equals(v);

        return rvv;
    }

    @NotNull @Override
    public final Termed apply(Compound ct, @NotNull Variable v) {
        return rename.computeIfAbsent(v, this);
    }

    @NotNull
    protected Variable newVariable(@NotNull Variable v, int serial) {
        if (v instanceof GenericVariable) {
            return ((GenericVariable) v).normalize(serial); //HACK
        } else {
            @NotNull AbstractVariable vn = $.v(v.op(), serial);
            if (v instanceof Ellipsis) {
                return ((Ellipsis) v).clone(vn, this);
            } else {
                return vn; //N/A
            }
        }

    }

}

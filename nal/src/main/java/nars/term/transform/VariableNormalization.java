package nars.term.transform;

import nars.$;
import nars.Global;
import nars.nal.meta.match.Ellipsis;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.variable.GenericVariable;
import nars.term.variable.Variable;
import org.jetbrains.annotations.NotNull;

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
public class VariableNormalization extends VariableTransform implements Function<Term,Variable> {

    @NotNull
    final Map<Term, Variable /*Variable*/> rename;

    boolean renamed;

    public VariableNormalization() {
        this(0);
    }

    public VariableNormalization(int size) {
        rename = Global.newHashMap(size);
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
        public Termed apply(Compound containing, @NotNull Term current) {

            if (current instanceof Ellipsis)
                throw new RuntimeException("not allowed");
                //return null;

            return $.v(current.op(), 1);
            //return _newVariable(current, 1);
        }
    };


    @NotNull
    @Override
    public final Variable apply(@NotNull Term v) {
        Variable rvv = newVariable(v, rename.size()+1);
        if (!this.renamed) {
            //track if modification occurred
            this.renamed = (rvv!=v); //!rvv.equals(v);
        }
        return rvv;
    }

    @NotNull @Override
    public final Termed apply(Compound ct, Term v) {
        return rename.computeIfAbsent(v, this);
    }

    @NotNull
    protected Variable newVariable(@NotNull Term v, int serial) {
        if (v instanceof GenericVariable) {
            return ((GenericVariable) v).normalize(serial); //HACK
        } else if (v instanceof Ellipsis) {
            return ((Ellipsis)v).clone($.v(v.op(), serial), this);
        } else {
            return $.v(v.op(), serial); //N/A
        }

    }

}

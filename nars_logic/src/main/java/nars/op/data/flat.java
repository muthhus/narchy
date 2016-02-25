package nars.op.data;

import nars.$;
import nars.Op;
import nars.term.Operator;
import nars.nal.nal8.operator.TermFunction;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * recursively collects the contents of set/list compound term argument's
 * into a list, to one of several resulting term types:
 *      product
 *      set (TODO)
 *      conjunction (TODO)
 *
 * TODO recursive version with order=breadth|depth option
 */
public abstract class flat extends TermFunction {

    @Override
    public Term function(@NotNull Compound op, TermBuilder i) {
        List<Term> l = new ArrayList();
        collect(Operator.argArray(op), l);
        return result(l);
    }

    @NotNull
    public static List<Term> collect(@NotNull Term[] x, @NotNull List<Term> l) {
        for (Term a : x) {
            if (a.op() == Op.PRODUCT || a.op().isSet() || a.op() == Op.CONJUNCTION) {
                ((Compound)a).addAllTo(l);
            }
            else
                l.add(a);
        }
        return l;
    }

    @NotNull
    public abstract Term result(List<Term> terms);

    public static class flatProduct extends flat {


        @NotNull
        @Override
        public Term result(@NotNull List<Term> terms) {
            return $.p(terms);
        }

    }

    //public Flat(boolean productOrSet, boolean breadthOrDepth) {
        //generate each of the 4 different operate names

    //}
}

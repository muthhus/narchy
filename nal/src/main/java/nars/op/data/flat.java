package nars.op.data;

import nars.$;
import nars.Op;
import nars.index.TermIndex;
import nars.nal.nal8.operator.TermFunction;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

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

    @NotNull
    @Override
    public Object function(@NotNull Compound op, TermIndex i) {
        List<Term> l = $.newArrayList();
        collect(((Compound)op.term(0)).terms(), l);
        return result(l);
    }

    @NotNull
    public static List<Term> collect(@NotNull Term[] x, @NotNull List<Term> l) {
        for (Term a : x) {
            if (a.op() == Op.PROD || a.op().isSet() || a.op() == Op.CONJ) {
                ((Compound)a).copyInto(l);
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

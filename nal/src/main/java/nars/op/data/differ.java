package nars.op.data;

import nars.$;
import nars.Op;
import nars.nal.TermBuilder;
import nars.nal.op.BinaryTermOperator;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** all X which are in the first term AND not in the second term */
public class differ extends BinaryTermOperator {


    /**
     * returns null if empty set
     */
    @Nullable
    public static Compound difference(@NotNull Op op, @NotNull Compound a, @NotNull Compound b) {
        return difference($.terms, op, a, b);
    }

    @Nullable
    public static Compound difference(@NotNull TermBuilder t, @NotNull Compound a, @NotNull TermContainer b) {
        return difference(t, a.op(), a, b);
    }

    @Nullable
    public static Compound difference(@NotNull TermBuilder t, @NotNull Op o, @NotNull Compound a, @NotNull TermContainer b) {

        //intersect the mask: if nothing in common, then it's entirely the first term
        if ((a.structure() & b.structure()) == 0) {
            return a;
        }

        Term[] aa = a.terms();

        List<Term> terms = $.newArrayList(aa.length);

        int retained = 0, size = a.size();
        for (int i = 0; i < size; i++) {
            Term x = a.term(i);
            if (!b.containsTerm(x)) {
                terms.add(x);
                retained++;
            }
        }

        if (retained == size) { //same as 'a'
            return a;
        } else if (retained == 0) {
            return null;
        } else {
            return (Compound) t.build(o, terms.toArray(new Term[retained]));
        }

    }

    @Nullable
    @Override
    public Term apply(@NotNull Term a, Term b) {
        if (!(a instanceof Compound) || !(b instanceof Compound))
            return null;

        return difference($.terms.builder(),
                (Compound) a, (Compound) b
        );
    }
}

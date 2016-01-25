package nars.op.math;

import nars.nal.Tense;
import nars.nal.nal8.operator.TermFunction;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 1/25/16.
 */
public abstract class IntIntTo extends TermFunction<Integer> {
    @NotNull
    @Override
    public Integer function(@NotNull Compound o, TermBuilder i) {

        Term[] x = o.terms();

        if (x.length < 2) {
            throw new RuntimeException("Requires 2 arguments");
        }

        int n1;

        try {
            n1 = integer(x[0]);
        } catch (NumberFormatException e) {
            throw new RuntimeException("1st parameter not an integer: " + x[0]);
        }

        int n2;
        try {
            n2 = integer(x[1]);
        } catch (NumberFormatException e) {
            throw new RuntimeException("2nd parameter not an integer: " + x[1]);
        }

        return n1 + n2;
    }

    @NotNull
    @Override
    public Tense getResultTense() {
        return Tense.Eternal;
    }
}

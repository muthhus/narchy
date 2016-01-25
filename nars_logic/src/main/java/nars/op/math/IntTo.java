package nars.op.math;

import nars.nal.Tense;
import nars.nal.nal8.operator.TermFunction;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermBuilder;
import org.jetbrains.annotations.NotNull;


public abstract class IntTo<Y> extends TermFunction<Y> {

    @NotNull
    @Override
    public final Y function(@NotNull Compound o, TermBuilder i) {

        Term[] x = o.terms();

        if (x.length < 1) {
            throw new RuntimeException("Requires 1 arguments");
        }

        int n1;

        try {
            n1 = integer(x[0]);
        } catch (NumberFormatException e) {
            throw new RuntimeException("1st parameter not an integer: " + x[0]);
        }

        return function(n1);
    }

    protected abstract Y function(int a);

    @NotNull
    @Override
    public Tense getResultTense() {
        return Tense.Eternal;
    }
}

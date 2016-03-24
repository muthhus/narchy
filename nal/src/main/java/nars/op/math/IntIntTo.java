package nars.op.math;

import nars.nal.Tense;
import nars.nal.nal8.operator.TermFunction;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermIndex;
import org.jetbrains.annotations.NotNull;


public abstract class IntIntTo<Y> extends TermFunction<Y> {

    @Override
    public final Y function(@NotNull Compound o, TermIndex i) {

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

        return function(n1, n2);
    }

    @NotNull
    protected abstract Y function(int a, int b);

    @NotNull
    @Override
    public Tense getResultTense(Task goal) {
        return Tense.Eternal;
    }
}

package nars.op.math;

import nars.index.TermIndex;
import nars.time.Tense;
import nars.nal.nal8.operator.TermFunction;
import nars.term.Compound;
import org.jetbrains.annotations.NotNull;


public abstract class IntIntTo<Y> extends TermFunction<Y> {

    @Override
    public final Y function(@NotNull Compound o, TermIndex i) {

        if (o.size() < 2) {
            throw new RuntimeException("Requires 2 arguments");
        }

        try {
            int n1 = integer(o.term(0));
            int n2 = integer(o.term(1));
            return apply(n1, n2);
        } catch (NumberFormatException e) {
            throw new RuntimeException("non-integer parameter" + o);
        }

    }

    @NotNull
    protected abstract Y apply(int a, int b);

    @NotNull
    @Override
    public Tense getResultTense() {
        return Tense.Eternal;
    }
}

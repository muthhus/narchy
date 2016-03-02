package nars.nal.meta.op;

import nars.Op;
import nars.nal.meta.AtomicBooleanCondition;
import nars.nal.meta.PremiseEval;
import nars.term.Compound;
import org.jetbrains.annotations.NotNull;

/**
 * assumes it will be matched against a 2-size compound (ex: (task,belief))
 */
public final class SubTermOps2 extends AtomicBooleanCondition<PremiseEval> {


    private final transient String id;
    @NotNull private final Op left, right;


    public SubTermOps2(Op left, Op right) {
        this.left = left;
        this.right = right;
        this.id = "SubTermOps:(\"" + left + "\",\"" + right + '"' + "\")";
    }

    @NotNull
    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean booleanValueOf(@NotNull PremiseEval ff) {
        Compound parent = (Compound) ff.term;
        return parent.term(0, left) &&
                parent.term(1, right);
    }
}

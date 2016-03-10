package nars.nal.meta.op;

import nars.Op;
import nars.nal.meta.AtomicBooleanCondition;
import nars.nal.meta.PremiseEval;
import nars.term.Compound;
import org.jetbrains.annotations.NotNull;

/**
 * requires a specific subterm type
 */
public final class SubTermOp extends AtomicBooleanCondition<PremiseEval> {

    public final int subterm;
    public final Op op;

    @NotNull
    private final transient String id;


    public SubTermOp(int subterm, Op op) {
        this.subterm = subterm;
        this.op = op;
        id = subterm + ":\"" + op + '"';
    }

    @NotNull
    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean booleanValueOf(@NotNull PremiseEval ff) {
        Compound parent = (Compound) ff.term;
        return parent.term(subterm, op);
    }
}

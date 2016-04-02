package nars.nal.meta.op;

import nars.Op;
import nars.nal.meta.AtomicBooleanCondition;
import nars.nal.meta.PremiseEval;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

/**
 * requires a specific subterm to have minimum bit structure
 */
public final class SubTermStructure extends AtomicBooleanCondition<PremiseEval> {
    public final int subterm;
    public final int bits;
    @NotNull
    private final transient String id;


    public SubTermStructure(@NotNull Op matchingType, int subterm, int bits) {
        this.subterm = subterm;

        if (matchingType != Op.VAR_PATTERN)
            bits &= (~matchingType.bit());
        //bits &= ~(Op.VariableBits);

        this.bits = bits;
        id = subterm + ":" +
                Integer.toString(bits, 16);
    }


    @NotNull
    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean booleanValueOf(@NotNull PremiseEval ff) {
        Term t = subterm == 0 ? ff.taskTerm : ff.beliefTerm;
        return !t.impossibleStructureMatch(bits);
    }
}

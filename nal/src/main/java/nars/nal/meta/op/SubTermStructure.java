package nars.nal.meta.op;

import nars.Op;
import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.PremiseEval;
import org.jetbrains.annotations.NotNull;

/**
 * requires a specific subterm to have minimum bit structure
 */
public final class SubTermStructure extends AtomicBoolCondition {
    public final int subterm;
    public final int bits;
    @NotNull
    private final transient String id;


    public SubTermStructure(int subterm, int bits) {
        this(Op.VAR_PATTERN, subterm, bits);
    }

    public SubTermStructure(@NotNull Op matchingType, int subterm, int bits) {
        this.subterm = subterm;


        this.bits = filter(matchingType, bits);
        id = "SubTermStruct" + subterm + ":" +
                Integer.toString(bits, 16);
    }


    @NotNull
    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean booleanValueOf(@NotNull PremiseEval ff) {
        /*Compound t = ff.term;
        return !t.term(subterm).impossibleStructureMatch(bits);*/
        return ff.subTermMatch(subterm, bits);
    }

    static int filter(Op matchingType, int bits) {
        if (matchingType != Op.VAR_PATTERN)
            bits &= (~matchingType.bit);

        //bits &= (~Op.NEGATE.bit); //filter based on negation

        return bits;
    }
}

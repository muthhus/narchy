package nars.derive.meta.op;

import nars.Op;
import nars.derive.meta.AtomicPredicate;
import nars.premise.Derivation;
import org.jetbrains.annotations.NotNull;

/**
 * requires a specific subterm to have minimum bit structure
 */
public final class SubTermStructure extends AtomicPredicate<Derivation> {
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
        if (this.bits == 0) {
            throw new RuntimeException("no filter effected");
        }
        id = "SubTermStruct" + subterm + ':' +
                Integer.toString(bits, 16);
    }


    @NotNull
    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean test(@NotNull Derivation ff) {
        //if the OR produces a different result compared to subterms,
        // it means there is some component of the other term which is not found
        //return ((possibleSubtermStructure | existingStructure) != existingStructure);
        return Op.hasAll((subterm == 0 ? ff.termSub0Struct : ff.termSub1Struct), bits);
    }

    static int filter(@NotNull Op matchingType, int bits) {
        if (matchingType != Op.VAR_PATTERN)
            bits &= (~matchingType.bit);

        bits &= (~Op.NEG.bit); //filter based on negation

        return bits;
    }
}

package nars.derive.meta.op;

import nars.$;
import nars.Op;
import nars.control.premise.Derivation;
import nars.derive.meta.AbstractPred;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * requires a specific subterm to have minimum bit structure
 */
public final class SubTermStructure extends AbstractPred<Derivation> {

    /** higher number means a stucture with more enabled bits will be decomposed to its components */
    public static final int SPLIT_THRESHOLD = 3;

    public final int subterm;
    public final int bits;

    public static List<SubTermStructure> get(int subterm, int bits) {
        int numBits = Integer.bitCount(bits);
        assert (numBits > 0);
        if ((numBits == 1) || (numBits > SPLIT_THRESHOLD)) {
            return Collections.singletonList(new SubTermStructure(subterm, bits));
        } else {
            List<SubTermStructure> components = $.newArrayList(numBits);
            for (Op o : Op.values()) {

                int b = o.bit;
                if ((bits & b) > 0) { //HACK
                    components.add(new SubTermStructure(subterm, b));
                }
            }
            return components;
        }
    }

    private SubTermStructure(int subterm, int bits) {
        this(Op.VAR_PATTERN, subterm, bits);
    }

    private SubTermStructure(@NotNull Op matchingType, int subterm, int bits) {
        super($.func("subTermStruct", $.the(subterm), $.the(bits)));
//                ((Integer.bitCount(bits) == 1) ?
//                        ("onBit_" + Integer.numberOfTrailingZeros(bits)) //shorthand for n'th bit
//                            :
//                        ("onAll_" + Integer.toBinaryString(bits))
//                ) + ')');
        this.subterm = subterm;


        this.bits = filter(matchingType, bits);
        if (this.bits == 0) {
            throw new RuntimeException("no filter effected");
        }

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

        //bits &= (~Op.NEG.bit); //filter based on negation

        return bits;
    }
}

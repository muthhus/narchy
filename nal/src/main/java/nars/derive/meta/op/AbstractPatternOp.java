package nars.derive.meta.op;

import nars.$;
import nars.Op;
import nars.derive.meta.AtomicPredicate;
import nars.derive.meta.BoolPredicate;
import nars.premise.Derivation;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

/**
 * a condition on the op of a pattern term (task=0, or belief=1)
 */
public enum AbstractPatternOp  {
    ;

    @NotNull
    static Compound name(@NotNull Class c, int subterm, String param) {
        return $.func(c.getSimpleName(), $.the("p" + Integer.toString(subterm))
                , $.quote(param));
    }

    public static final class PatternOp extends AtomicPredicate<Derivation> {

        public final int subterm;
        public final int opOrdinal;

        @NotNull private final transient String id;

        public PatternOp(int subterm, @NotNull Op op) {

            this.subterm = subterm;
            this.opOrdinal = op.ordinal();
            this.id = name(getClass(), subterm, op.str).toString();
        }

        @NotNull
        @Override
        public String toString() {
            return id;
        }

        @Override
        public boolean test(@NotNull Derivation ff) {
            return (subterm == 0 ? ff.termSub0op : ff.termSub1op) == opOrdinal;
        }

    }

//    /** tests op membership in a given vector
//      * the bit must not be set in the structure
//      * */
//    public static final class PatternOpNot extends BoolPredicate.DefaultBoolPredicate<Derivation> {
//
//        public PatternOpNot(int subterm, int structure) {
//            super(name(PatternOpNot.class, subterm, Integer.toString(structure,2)), (ff) -> {
//                return (structure & (subterm == 0 ? ff.termSub0opBit : ff.termSub1opBit)) == 0;
//            });
//        }
//
//    }

//    /** tests op membership wotjom in a given vector */
//    public static final class PatternOpNotContaining extends AtomicPredicate<Derivation> {
//
//        public final int subterm;
//        public final int opBits;
//
//        @NotNull private final transient String id;
//
//
//        public PatternOpNotContaining(int subterm, int structure) {
//            this.subterm = subterm;
//            this.opBits = structure;
//            this.id = name(getClass(), subterm, Integer.toString(structure,2)).toString();
//        }
//
//        @Override
//        public @NotNull String toString() {
//            return id;
//        }
//
//        @Override
//        public boolean test(@NotNull Derivation ff) {
//            return (opBits & (subterm == 0 ? ff.termSub0Struct : ff.termSub1Struct)) == 0;
//        }
//    }

}

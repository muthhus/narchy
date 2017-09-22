package nars.truth.func;

import nars.$;
import nars.NAR;
import nars.Op;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import nars.truth.func.annotation.AllowOverlap;
import nars.truth.func.annotation.SinglePremise;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Map;

import static nars.truth.TruthFunctions.*;

public enum GoalFunction implements TruthOperator {

    @AllowOverlap Strong() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return desireStrongOriginal(T, B, minConf);
            //return TruthFunctions.desire(T, B, minConf, false);
        }
    },

    @AllowOverlap Weak() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return desireWeakOriginal(T, B, minConf);
            //return TruthFunctions.desire(T, B, minConf, true);
        }
    },


    @AllowOverlap DeciDeduction() {
        @Override
        public Truth apply(Truth T, Truth B, NAR m, float minConf) {
            return BeliefFunction.DeductionPB.apply(T, B, m, minConf);
//            if (B.isNegative()) {
//                Truth x = deduction(T, B.neg(), minConf);
//                //Truth x = desireDed(T, B.neg(), minConf);
//                return x != null ? x.neg() : null;
//            } else {
//                return deduction(T, B, minConf);
//                //return desireDed(T, B, minConf);
//            }
//
//            //return desireStrongNew(T, B, minConf, true);
        }

    },

    @AllowOverlap DeciInduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return BeliefFunction.AbductionPB.apply(B, T, m, minConf); //swap B and T to compute Induction from the Abduction formula

//            if (B.isNegative()) {
//                Truth x = induction(T, B.neg(), minConf);
//                //Truth x = desireInd(T, B.neg(), minConf);
//                return x != null ? x.neg() : null;
//            } else  {
//                return induction(T, B, minConf);
//                //return desireInd(T, B, minConf);
//            }
        }
    },

    @AllowOverlap DecomposePositiveNegativeNegative() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return decompose(T, B, true, false, false, minConf);
        }
    },

    @AllowOverlap DecomposePositiveNegativePositive() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T, B, true, false, true, minConf);
        }
    },

    @AllowOverlap DecomposeNegativeNegativeNegative() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return decompose(T, B, false, false, false, minConf);
        }
    },

    @AllowOverlap DecomposeNegativePositivePositive() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return TruthFunctions.decompose(T, B, false, true, true, minConf);
        }
    },



    @SinglePremise
    @AllowOverlap
    StructuralDeduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return deduction1(T, defaultConf(m), minConf);
        }
    },

//    @SinglePremise
//    @AllowOverlap
//    BeliefStructuralDeduction() {
//        @Override
//        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
//            return deduction1(B, defaultConf(m), minConf);
//        }
//    },


//    @AllowOverlap @SinglePremise
//    StructuralStrongNeg() {
//        
//        @Override public Truth apply( final Truth T,  final Truth B, Memory m, float minConf) {
//            return TruthFunctions.desireStrong(T, defaultTruth(m).negated(), minConf);
//        }
//    },


    Union() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return union(T, B, minConf);
        }
    },
   Intersection() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return intersection(T, B, minConf);
        }
    },

    Difference() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return difference(T, B, minConf);
        }
    },;

//    StructuralIntersection() {
//        @Override
//        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
//            return B != null ? TruthFunctions.intersection(B, defaultTruth(m), minConf) : null;
//        }
//    },


    @NotNull
    private static Truth defaultTruth(NAR m) {
        return m.truthDefault(Op.GOAL);
    }


    static final Map<Term, TruthOperator> atomToTruthModifier = $.newHashMap(GoalFunction.values().length);

    static {
        TruthOperator.permuteTruth(GoalFunction.values(), atomToTruthModifier);
    }


    public static TruthOperator get(Term a) {
        return atomToTruthModifier.get(a);
    }


    private final boolean single;
    private final boolean overlap;

    GoalFunction() {

        try {
            Field enumField = getClass().getField(name());
            this.single = enumField.isAnnotationPresent(SinglePremise.class);
            this.overlap = enumField.isAnnotationPresent(AllowOverlap.class);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public boolean single() {
        return single;
    }

    @Override
    public boolean allowOverlap() {
        return overlap;
    }

    private static float defaultConf(NAR m) {
        return m.confDefault(Op.GOAL);
    }
}
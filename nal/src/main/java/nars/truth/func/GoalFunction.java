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

    Strong() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return desireStrongOriginal(T, B, minConf);
        }
    },

    Weak() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return desireWeakOriginal(T, B, minConf);
        }
    },

    Deduction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return deduction(T, B, minConf);
        }
    },
//    Goduction() {
//        @Override
//        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
//            return deduction(T, B, minConf);
//        }
//    },
//    @AllowOverlap  GoductionRecursivePB() {
//        @Override
//        public Truth apply(Truth T, Truth B, NAR m, float minConf) {
//              if (B.isNegative()) {
//                Truth x = Goduction.apply(T, B.neg(), m, minConf);
//                return x != null ? x.neg() : null;
//            } else {
//                return Goduction.apply(T, B, m, minConf);
//            }
//        }
//    },

//    Goduction() {
//        
//        @Override public Truth apply( final Truth T,  final Truth B, NAR m, float minConf) {
//            return TruthFunctions.desireStrongOriginal(T, B, minConf);
//        }
//    },

//    @SinglePremise StructuralGoduction() {
//        
//        @Override public Truth apply( final Truth T,  final Truth B, NAR m, float minConf) {
//            return TruthFunctions.desireStrongOriginal(T, defaultTruth(m), minConf);
//        }
//    },


    @SinglePremise
    Negation() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return negation(T, minConf);
        }
    },

    Induction() {
        @Override
        public Truth apply(final Truth T, final Truth B, NAR m, float minConf) {
            return induction(T, B, minConf);
        }
    },

    @AllowOverlap DeciDeduction() {
        @Override
        public Truth apply(Truth T, Truth B, NAR m, float minConf) {
            if (B.isNegative()) {
                Truth x = deduction(T, B.neg(), minConf);
                return x != null ? x.neg() : null;
            } else {
                return deduction(T, B, minConf);
            }
        }

    },

    @AllowOverlap DeciInduction() {
        @Override public Truth apply ( final Truth T, final Truth B, NAR m,float minConf){
            if (B.isNegative()) {
                Truth x = induction(T.neg(), B.neg(), minConf);
                return x != null ? x : null;
            } else {
                return induction(T, B, minConf);
            }
        }
    },

    DecomposePositiveNegativeNegative() {
        @Override
        public Truth apply ( final Truth T, final Truth B, NAR m,float minConf){
            return decompose(T, B, true, false, false, minConf);
        }
    },

    DecomposePositiveNegativePositive() {
        @Override
        public Truth apply ( final Truth T, final Truth B, NAR m,float minConf){
            return TruthFunctions.decompose(T, B, true, false, true, minConf);
        }
    },

    DecomposeNegativeNegativeNegative() {
        @Override
        public Truth apply ( final Truth T, final Truth B, NAR m,float minConf){
            return decompose(T, B, false, false, false, minConf);
        }
    },

    DecomposeNegativePositivePositive() {
        @Override
        public Truth apply ( final Truth T, final Truth B, NAR m,float minConf){
            return TruthFunctions.decompose(T, B, false, true, true, minConf);
        }
    },

    @SinglePremise
    Identity() {
        @Override
        public Truth apply ( final Truth T, final Truth B, NAR m,float minConf){
            return TruthOperator.identity(T, minConf);
        }
    },

    /**
     * same as identity but allows overlap
     */
    @SinglePremise
    //@AllowOverlap
    IdentityTransform() {
        @Override
        public Truth apply ( final Truth T, final Truth B, NAR m,float minConf){
            return TruthOperator.identity(T, minConf);
        }
    },


    @SinglePremise
    @AllowOverlap
    StructuralDeduction() {
        @Override
        public Truth apply ( final Truth T, final Truth B, NAR m,float minConf){
            return deduction1(T, defaultTruth(m).conf(), minConf);
        }
    },

    @SinglePremise
    @AllowOverlap
    BeliefStructuralDeduction() {
        @Override
        public Truth apply ( final Truth T, final Truth B, NAR m,float minConf){
            return deduction1(B, defaultConfidence(m), minConf);
        }
    },


//    @AllowOverlap @SinglePremise
//    StructuralStrongNeg() {
//        
//        @Override public Truth apply( final Truth T,  final Truth B, Memory m, float minConf) {
//            return TruthFunctions.desireStrong(T, defaultTruth(m).negated(), minConf);
//        }
//    },


    Union() {
        @Override
        public Truth apply ( final Truth T, final Truth B, NAR m,float minConf){
            return union(T, B, minConf);
        }
    },

    StructuralIntersection() {
        @Override
        public Truth apply ( final Truth T, final Truth B, NAR m,float minConf){
            return B != null ? TruthFunctions.intersection(B, defaultTruth(m), minConf) : null;
        }
    },


    Intersection() {
        @Override
        public Truth apply ( final Truth T, final Truth B, NAR m,float minConf){
            return intersection(T, B, minConf);
        }
    },

    Difference() {
        @Override
        public Truth apply ( final Truth T, final Truth B, NAR m,float minConf){
            return difference(T, B, minConf);
        }
    },;

    @NotNull
    private static Truth defaultTruth(NAR m) {
        return m.truthDefault(Op.GOAL /* goal? */);
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

    private static float defaultConfidence(NAR m) {
        return m.confDefault(Op.GOAL);
    }
    }
package nars.derive.op;

import nars.control.Derivation;
import nars.derive.AbstractPred;
import nars.derive.PrediTerm;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

/**
 * belief truth is postiive
 */
public enum BeliefPolarity {
    ;

    public static final PrediTerm<Derivation> beliefPos = new AbstractPred<Derivation>("(BeliefPos)") {
        @Override public boolean test(Derivation d) {
            Truth B = d.beliefTruth;
            return B != null && B.freq() >= 0.5f;
        }
    };
    public static final PrediTerm<Derivation> beliefNeg = new AbstractPred<Derivation>("(BeliefNeg)") {
        @Override public boolean test(Derivation d) {
            Truth B = d.beliefTruth;
            return B != null && B.freq() < 0.5f;
        }
    };
    public static final PrediTerm<Derivation> beliefExist = new AbstractPred<Derivation>("(BeliefExist)") {
        @Override public boolean test(Derivation d) {
            return d.belief!=null;
        }
    };

}
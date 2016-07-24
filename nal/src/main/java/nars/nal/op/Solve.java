package nars.nal.op;

import nars.Op;
import nars.Symbols;
import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.PremiseEval;
import nars.truth.func.TruthOperator;
import org.jetbrains.annotations.NotNull;

/**
 * Evaluates the truth of a premise
 */
abstract public class Solve extends AtomicBoolCondition {

    private final transient String id;

    public final Derive derive;

    public final TruthOperator belief;
    public final TruthOperator desire;

    public Solve(String id, Derive derive, TruthOperator belief, TruthOperator desire) {
        super();
        this.id = id;
        this.derive = derive;
        this.belief = belief;
        this.desire = desire;
    }

    @NotNull
    @Override
    public String toString() {
        return id;
    }

    final boolean measure(@NotNull PremiseEval m, char punct) {

        switch (punct) {
            case Symbols.BELIEF:
            case Symbols.GOAL:
                TruthOperator tf = (punct == Symbols.BELIEF) ? belief : desire;

                if (!((tf!=null) &&
                    (tf.single() || m.belief!=null) && //if double premise, then belief must be present
                    (tf.allowOverlap() || !m.overlap)))
                    return false;

                break;
            case Symbols.QUESTION:
            case Symbols.QUEST:
                //a truth function is not involved, so succeed
                break;
            default:
                throw new Op.InvalidPunctuationException(punct);
        }

        m.punct.set(punct);
        return true;
    }


}


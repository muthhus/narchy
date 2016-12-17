package nars.nal.meta;

import nars.Op;
import nars.truth.Truth;
import nars.truth.func.TruthOperator;
import org.jetbrains.annotations.NotNull;

import static nars.Symbols.*;

/**
 * Evaluates the truth of a premise
 */
abstract public class Solve extends AtomicBoolCondition {

    private final transient String id;

    public final Conclude conclude;

    public final TruthOperator belief;
    public final TruthOperator desire;

    public Solve(String id, Conclude conclude, TruthOperator belief, TruthOperator desire) {
        super();
        this.id = id;
        this.conclude = conclude;
        this.belief = belief;
        this.desire = desire;
    }

    @NotNull
    @Override
    public String toString() {
        return id;
    }

    final boolean measure(@NotNull Derivation m, char punct) {

        boolean single;
        Truth t;

        switch (punct) {
            case BELIEF:
            case GOAL:
                TruthOperator f = (punct == BELIEF) ? belief : desire;
                if (f == null)
                    return false; //there isnt a truth function for this punctuation

                single = f.single();
                if (!single && m.belief == null)  //double premise, but belief is null
                    return false;

                if (!f.allowOverlap() && m.overlap(single))
                    return false;

                //task truth is not involved in the outcome of this; set task truth to be null to prevent any negations below:
                Truth taskTruth = m.taskTruth;

                //truth function is single premise so set belief truth to be null to prevent any negations below:

                Truth beliefTruth = (single) ? null : m.beliefTruth;

                t = f.apply(
                        taskTruth,
                        beliefTruth,
                        m.nar,
                        m.confMin
                );

                if (t == null)
                    return false;

                t = t.dither(m.truthResolution);

                break;

            case QUESTION:
            case QUEST:
                //a truth function so check cyclicity
                if (m.cyclic)
                    return false;
                single = true;
                t = null;
                break;

            default:
                throw new Op.InvalidPunctuationException(punct);
        }



        return m.setPunct(t, punct, m.evidence(single));
    }


}


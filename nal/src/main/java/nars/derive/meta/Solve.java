package nars.derive.meta;

import nars.Op;
import nars.premise.Derivation;
import nars.premise.TruthPuncEvidence;
import nars.truth.Truth;
import nars.truth.func.TruthOperator;
import org.jetbrains.annotations.NotNull;

import static nars.Op.*;

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

                //truth function is single premise so set belief truth to be null to prevent any negations below:
                float confMin = m.confMin;


                if ((t = f.apply(
                        m.taskTruth, //task truth is not involved in the outcome of this; set task truth to be null to prevent any negations below:
                        (single) ? null : m.beliefTruth,
                        m.nar, confMin
                ))==null)
                    return false;

                float eFactor = m.nar.derivedEvidenceGain.asFloat();
                if (eFactor != 1) {
                    if ((t = t.confWeightMult(eFactor))==null)
                        return false;
                }

                if ((t = t.dither(m.truthResolution, confMin))==null)
                    return false;

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


        return m.punct.set(new TruthPuncEvidence(t, punct,
                single ? m.evidenceSingle() : m.evidenceDouble()
        ))!=null;
    }


}


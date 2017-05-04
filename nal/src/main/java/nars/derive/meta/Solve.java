package nars.derive.meta;

import nars.Op;
import nars.premise.Derivation;
import nars.premise.TruthPuncEvidence;
import nars.term.Compound;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import nars.truth.func.TruthOperator;
import org.jetbrains.annotations.NotNull;

import static nars.Op.*;
import static nars.truth.TruthFunctions.w2c;

/**
 * Evaluates the truth of a premise
 */
abstract public class Solve extends AbstractPred<Derivation> {


    public final Conclude conclude;

    public final TruthOperator belief;
    public final TruthOperator goal;
    public final boolean beliefProjected;

    public Solve(Compound id, Conclude conclude, TruthOperator belief, TruthOperator goal, boolean beliefProjected) {
        super(id);
        this.conclude = conclude;
        this.belief = belief;
        this.goal = goal;
        this.beliefProjected = beliefProjected;
    }


    final boolean measure(@NotNull Derivation m, byte punct) {

        boolean single;
        Truth t;

        switch (punct) {
            case BELIEF:
            case GOAL:
                TruthOperator f = (punct == BELIEF) ? belief : goal;
                if (f == null)
                    return false; //there isnt a truth function for this punctuation

                single = f.single();
                if (!single && m.belief == null) {  //double premise requiring a belief, but belief is null
                    return false;
                }

                if (!f.allowOverlap() && (single ? m.cyclic : m.overlap))
                    return false;

                //truth function is single premise so set belief truth to be null to prevent any negations below:
                float confMin = m.confMin;

                if ((t = f.apply(
                        m.taskTruth, //task truth is not involved in the outcome of this; set task truth to be null to prevent any negations below:
                        (single) ? null : (beliefProjected ? m.beliefTruth : m.beliefTruthRaw),
                        m.nar, confMin
                ))==null)
                    return false;

                if ((t = t.dither(m.truthResolution, confMin, m.nar.derivedEvidenceGain.asFloat()))==null)
                    return false;

                break;

            case QUEST:
            case QUESTION:
                //a truth function so check cyclicity
                if (m.cyclic || m.overlap)
                    return false;

                switch (m.taskPunct) {
                    case BELIEF:
                        break;
                    case QUESTION:
                        break;
                    case GOAL:
                    case QUEST:
                        punct = QUEST; //apply similar behavior for Question to Quests
                        break;
                }

                single = true;
                t = null;
                break;

            default:
                throw new Op.InvalidPunctuationException(punct);
        }

        m.punct = (new TruthPuncEvidence(t, punct,
                single ? m.evidenceSingle() : m.evidenceDouble()
        ));
        return true;
    }


}


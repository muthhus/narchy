package nars.budget;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.concept.TruthDelta;
import nars.index.TermIndex;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import nars.util.Util;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Param.TRUTH_EPSILON;

/**
 * Created by me on 8/22/16.
 */
public class Activation {

    private static final int TASKLINK_DEPTH_LIMIT = 1;

    public final Budgeted in;

    public final Concept src;

    public final ObjectFloatHashMap<Concept> concepts = new ObjectFloatHashMap<>();
    public final MutableFloat linkOverflow = new MutableFloat(0);
    public final MutableFloat conceptOverflow = new MutableFloat(0);
    private final NAR nar;


    public Activation(Budgeted in, Concept src, NAR nar) {

        this.nar = nar;
        this.in = in;
        this.src = src;

    }

    /**
     * runs the task activation procedure
     */
    public Activation(Budgeted in, Concept c, NAR nar, float scale) {
        this(in, c, nar);

        link(scale);

    }

    public Activation(@NotNull Task in, @NotNull NAR nar, float scale) {
        this(in, in.concept(nar), nar);
        activateConcept(src, scale);
        link(scale);
    }

    /**
     *
     * @param input
     * @param concept
     * @param nar
     * @param scale
     * @param delta - null for questions/quests
     */
    public Activation(@NotNull Task input, CompoundConcept concept, NAR nar, float scale, @Nullable TruthDelta delta) {
        this(input, concept, nar, scale);

        if (delta!=null) {
            feedback(input, delta, concept);
        }
    }


    /**
     * apply derivation feedback and update NAR emotion state
     */
    protected void feedback(Task input, TruthDelta delta, CompoundConcept concept) {


        //update emotion happy/sad
        Truth before = delta.before;
        Truth after = delta.after;

        float deltaSatisfaction, deltaConf;

        if (before !=null && after !=null) {

            float deltaFreq = after.freq() - before.conf();
            deltaConf = after.conf() - before.conf();

            Truth other;
            float polarity =  0;

            if (input.isBelief() && concept.hasGoals()) {
                //compare against the current goal state
                other = concept.goals().truth(nar.time());
                polarity = +1f;
            } else if (input.isGoal() && concept.hasBeliefs()) {
                //compare against the current belief state
                other = concept.beliefs().truth(nar.time());
                polarity = -1f;
            } else {
                other = null;
            }


            if (other!=null) {

                float f = other.freq();

                if (Util.equals(f, 0.5f, TRUTH_EPSILON)) {

                    //ambivalence: no change
                    deltaSatisfaction = 0;

                } else if (f > 0.5f) {
                    //measure how much the freq increased since goal is positive
                    deltaSatisfaction = +polarity * deltaFreq / (2f * (other.freq() - 0.5f));
                } else {
                    //measure how much the freq decreased since goal is negative
                    deltaSatisfaction = -polarity * deltaFreq / (2f * (0.5f - other.freq()));
                }

                nar.emotion.happy(deltaSatisfaction, input.term());

            } else {
                deltaSatisfaction = 0;
            }

        } else {
            if (before == null && after!=null) {
                deltaConf = after.conf();
            } else {
                deltaConf = 0;
            }
            deltaSatisfaction = 0;
        }

        if (!Util.equals(deltaConf, 0f, TRUTH_EPSILON))
            nar.emotion.confident(deltaConf, input.term());

        input.feedback(delta, deltaConf, deltaSatisfaction, nar);

    }

    protected final void link(float scale) {
        link(src, src.term(), scale, 0);

        commit(scale); //values will already be scaled
    }

    public void linkTermLinks(Concept src, float scale) {
        src.termlinks().forEach(n -> {
            link(src, n.get(), scale, 0);
        });
    }

    public void linkTerms(@NotNull Concept src, @NotNull Term[] tgt, float scale, float minScale, int depth) {

        int n = tgt.length;
        float tStrength = 1f / n;
        float subScale = scale * tStrength;

        if (subScale >= minScale) { //TODO use a min bound to prevent the iteration ahead of time

            //then link this to terms
            for (int i = 0; i < n; i++) {
                Term tt = tgt[i];

                link(src, tt, subScale, depth+1); //Link the peer termlink bidirectionally
            }
        }

    }

    /**
     * crosslinks termlinks
     */
    @Nullable
    Concept linkSubterm(@NotNull Concept source, @NotNull Termed target, float subScale, int depth) {

    /* activate concept */
        Concept targetConcept;

        Term targetTerm = target.term();

        if (!TermIndex.linkable(targetTerm)) {
            targetConcept = null;
        } else {
            targetConcept = nar.concept(target, true);
            if (targetConcept == null)
                throw new NullPointerException(target + " did not resolve to a concept");
            //if (targetConcept!=null)

            activateConcept(targetConcept, subScale);

            if (targetConcept instanceof CompoundConcept)
                linkTemplates(targetConcept, subScale, depth);

//            activate(targetConcept, subScale);
//            targetConcept = nar.activate(target,
//                    activation);
            //if (targetConcept == null)
            //throw new RuntimeException("termlink to null concept: " + target);
        }

        Term sourceTerm = source.term();

        if (!targetTerm.equals(sourceTerm)) {

            //        /* insert termlink target to source */
            boolean alsoReverse = true;
            if (targetConcept != null && alsoReverse) {
                //subScale /= 2; //divide among both directions?

                targetConcept.termlinks().put(sourceTerm, in, subScale, linkOverflow);


            }

        /* insert termlink source to target */
            source.termlinks().put(targetTerm, in, subScale, linkOverflow);
        }


        return targetConcept;
    }

    public void link(Concept src, Term target, float scale, int depth) {

        Concept targetConcept = linkSubterm(src, target, scale, depth);


        if (targetConcept != null && in instanceof Task && depth <= TASKLINK_DEPTH_LIMIT) {
            //System.out.println(in + " <- " + targetConcept + " " + depth);

            linkTask(scale, targetConcept);
        }


    }

    protected void linkTemplates(Concept src, float subScale, int depth) {
        linkTerms(src, ((CompoundConcept)src).templates.terms(), subScale, Param.BUDGET_EPSILON, depth);
    }

    public void linkTask(float subScale, Concept target) {

        target.tasklinks().put((Task)in, in, subScale, null);
    }


    public void commit(float scale) {
        if (!concepts.isEmpty()) {
            nar.activate(concepts, in, scale / (float)concepts.sum(), conceptOverflow);
        }
    }

    public void activateConcept(Concept targetConcept, float scale) {
        //System.out.println("+" + scale + " x " + targetConcept);
        concepts.addToValue(targetConcept, scale);
    }

}

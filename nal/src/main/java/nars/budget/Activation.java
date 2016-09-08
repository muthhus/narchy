package nars.budget;

import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.concept.TruthDelta;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.var.Variable;
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
    public final Budgeted in;

    public Concept src;
    //final ObjectFloatHashMap<BLink<Task>> tasks = new ObjectFloatHashMap<>();
    //public final ObjectFloatHashMap<BLink<Term>> termlinks = new ObjectFloatHashMap<>();
    public final ObjectFloatHashMap<Concept> concepts = new ObjectFloatHashMap<>();
    public final MutableFloat linkOverflow = new MutableFloat(0);
    public final MutableFloat conceptOverflow = new MutableFloat(0);

    public Activation(@NotNull Task in, @NotNull NAR n, float scale) {
        this(in, in.concept(n));
        link(n, scale);
    }

    public Activation(Budgeted in, Concept src) {

        this.in = in;
        this.src = src;

    }

    /**
     * runs the task activation procedure
     */
    public Activation(Budgeted in, Concept c, NAR nar, float scale) {
        this(in, c);

        link(nar, scale);

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
            feedback(input, delta, concept, nar);
        }
    }


    /**
     * apply derivation feedback and update NAR emotion state
     */
    protected void feedback(Task input, TruthDelta delta, CompoundConcept concept, NAR nar) {


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

    protected final void link(NAR nar, float scale) {
        src.link(1f, null/* linkActivation */, Param.BUDGET_EPSILON, nar, this);

        activate(nar, scale); //values will already be scaled
    }

    public void linkTermLinks(Concept src, float scale, NAR nar) {
        src.termlinks().forEach(n -> {
            linkTerm(src, nar, scale, n.get());
        });
    }

    public void linkTerms(Concept src, Term[] tgt, float scale, float minScale, @NotNull NAR nar) {


        Concept asrc = src;
        if (asrc != null) {
            //link the src to this
            linkSub(src, asrc, scale, nar);
        } else {
            activate(src, scale); //activate self
        }

        int n = tgt.length;
        float tStrength = 1f / n;
        float subScale = scale * tStrength;

        if (subScale > minScale) { //TODO use a min bound to prevent the iteration ahead of time

            //then link this to terms
            for (int i = 0; i < n; i++) {
                Term tt = tgt[i];


                //Link the peer termlink bidirectionally
                linkTerm(src, nar, subScale, tt);
            }
        }


    }

    /**
     * crosslinks termlinks
     */
    @Nullable
    Concept linkSub(@NotNull Concept source, @NotNull Termed target,
                    float subScale,
                    @NotNull NAR nar) {

    /* activate concept */
        Concept targetConcept;

        Term targetTerm = target.term();

        if (!linkable(targetTerm)) {
            targetConcept = null;
        } else {
            targetConcept = nar.concept(target, true);
            if (targetConcept == null)
                throw new NullPointerException(target + " did not resolve to a concept");
            //if (targetConcept!=null)


            activate(targetConcept, subScale);
//            targetConcept = nar.activate(target,
//                    activation);
            //if (targetConcept == null)
            //throw new RuntimeException("termlink to null concept: " + target);
        }

        if (!target.term().equals( source.term() )) {
            //            throw new RuntimeException("termlink self-loop");


            //        /* insert termlink target to source */
            boolean alsoReverse = true;
            if (targetConcept != null && alsoReverse) {
                subScale /= 2; //divide among both directions

                targetConcept.termlinks().put(source.term(), in, subScale, linkOverflow);
            }

        /* insert termlink source to target */
            source.termlinks().put(targetTerm, in, subScale, linkOverflow);
        }

        return targetConcept;
    }

    public static boolean linkable(@NotNull Term x) {
//        return !(target instanceof Variable);
        if (x instanceof Variable) {
            return false;
        }
        if (x instanceof Compound) {

            if (x.op() == Op.NEG) {
                if (((Compound) x).term(0) instanceof Variable)
                    return false;
            }
            if (!x.isNormalized())
                return false;
        }
        return true;
    }

    public void linkTerm(Concept src, @NotNull NAR nar, float subScale, Term tt) {
        Concept target = linkSub(src, tt, subScale, nar);

        if (target != null && in instanceof Task) {
            //insert recursive tasklink
            target.linkTask((Task) in, subScale);
        }
    }

    public void run(@NotNull NAR nar) {
        activate(nar, 1f);
    }

    public void activate(@NotNull NAR nar, float activation) {
        if (!concepts.isEmpty()) {
            float total =
                    //1 / (float) concepts.sum(); //normalized
                    1f; //unnormalized

            nar.activate(concepts, in, activation / total, conceptOverflow);
        }
    }

    public void activate(Concept targetConcept, float scale) {
        concepts.addToValue(targetConcept, scale);
    }

}

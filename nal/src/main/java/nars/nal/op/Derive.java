package nars.nal.op;

import com.google.common.base.Joiner;
import nars.*;
import nars.budget.Budget;
import nars.nal.ConceptProcess;
import nars.nal.TimeFunctions;
import nars.nal.meta.PremiseEval;
import nars.nal.meta.ProcTerm;
import nars.nal.meta.TruthOperator;
import nars.nal.rule.PremiseRule;
import nars.task.Revision;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.AtomicStringConstant;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static nars.Op.ATOM;
import static nars.Op.NEG;
import static nars.nal.Tense.DTERNAL;
import static nars.nal.Tense.ETERNAL;

/**
 * Handles matched derivation results
 * < (&&, postMatch1, postMatch2) ==> derive(term) >
 */
public final class Derive extends AtomicStringConstant implements ProcTerm {

    @NotNull
    public final String id;

    public final boolean eternalize;

    @NotNull
    public final PremiseRule rule;
    private final @NotNull TimeFunctions temporalizer;

    /**
     * result pattern
     */
    @NotNull
    public final Term conclusionPattern;


    /**
     * whether this a single or double premise derivation; necessary in case premise
     * does have a belief but it was not involved in determining Truth
     */
    private final TruthOperator belief;
    private final TruthOperator goal;
    //private final ImmutableSet<Term> uniquePatternVar;


    public Derive(@NotNull PremiseRule rule, @NotNull Term term,
                  TruthOperator belief, TruthOperator goal, boolean eternalize, @NotNull TimeFunctions temporalizer) {
        this.rule = rule;

        this.belief = belief;
        this.goal = goal;


        this.conclusionPattern = term;
        //this.uniquePatternVar = Terms.unique(term, (Term x) -> x.op() == VAR_PATTERN);
        this.temporalizer = temporalizer;

        this.eternalize = eternalize;

        this.id = "Derive(" +
                Joiner.on(',').join(
                    term,
                    "temporal" + Integer.toHexString(temporalizer.hashCode()), //HACK todo until names are given to unique classes
                    belief!=null ? belief : "_",
                    goal !=null ? goal : "_",
                    eternalize ? "Et" : "_") +
                ')';


    }


    @NotNull
    @Override
    public Op op() {
        return ATOM; //product?
    }

    @NotNull
    @Override
    public String toString() {
        return id;
    }


    /**
     * main entry point for derivation result handler.
     *
     * @return true to allow the matcher to continue matching,
     * false to stop it
     */
    @Override
    public final void accept(@NotNull PremiseEval m) {

        Term cp = this.conclusionPattern;

        Truth taskTruth = m.taskTruth, beliefTruth = m.beliefTruth;

        char punct = m.punct.get();


        if (!cp.op().atomic && (punct!=Symbols.QUEST && punct!=Symbols.QUESTION)) {

            if (taskTruth != null && taskTruth.isNegative()) {
                Map<Term, Term> cc = new HashMap();
                Term taskPattern = this.rule.getTask();
                cc.put(taskPattern, $.neg(taskPattern));
                Term ccp = $.terms.remap(cp, cc);
                if (!ccp.equals(cp)) {
                    taskTruth = taskTruth.negated();
                    cp = ccp;
                }
            }

            if (beliefTruth != null && beliefTruth.isNegative()) {

                Map<Term, Term> cc = new HashMap();
                @NotNull Term beliefPattern = this.rule.getBelief();
                cc.put(beliefPattern, $.neg(beliefPattern));
                Term ccp = $.terms.remap(cp, cc);
                if (!ccp.equals(cp)) {
                    beliefTruth = beliefTruth.negated();
                    cp = ccp;
                }

            }


        }

        Term r = m.index.resolve(cp, m);
        if (r instanceof Compound) { //includes null test

            char c = punct;
            TruthOperator f;
            if (c == Symbols.BELIEF)
                f = belief;
            else if (c == Symbols.GOAL)
                f = goal;
            else
                f = null;

            Truth t = (f == null) ?
                null :
                f.apply(
                     taskTruth,
                     beliefTruth,
                     m.nar,
                     m.confMin
                    );


            if (f==null || t!=null)
                derive(m, (Compound)r, t);
        }

    }

    final void derive(@NotNull PremiseEval m, @NotNull Compound raw, Truth truth) {
        ConceptProcess premise = m.premise;
        NAR nar = m.nar;

        if (raw.op() == NEG) {
            //negations cant term concepts or tasks, so we unwrap and invert the truth (fi
            Term raw2 = raw.term(0);
            if (!(raw2 instanceof Compound))
                return; //unwrapped to a variable (negations of atomics are not allowed)
            raw = (Compound)raw2;
            if (truth!=null)
                truth = truth.negated();
        }



        //pre-filter invalid statements: insufficient NAL level, etc
        if (!Task.preNormalize(raw, nar))
            return;

        Budget budget = m.budget(truth, raw);
        if (budget == null)
            return;

        //get the normalized term to determine the budget (via it's complexity)
        //this way we can determine if the budget is insufficient
        //before conceptualizating in mem.taskConcept
        Termed<Compound> content = nar.index.normalize(raw, true);

        if (content == null)
            return; //HACK why would this happen?



        long occ;

        if ((nar.nal() >= 7) && (premise.hasTemporality())) {

            long[] occReturn = new long[]{ETERNAL};
            float[] confScale = new float[] { 1f };

            Term temporalized = this.temporalizer.compute(content.term(),
                    m, this, occReturn, confScale
            );

            if (Global.DEBUG && occReturn[0] == DTERNAL) {
                //temporalizer.compute(content.term(), m, this, occReturn, confScale);
                throw new RuntimeException("temporalization resulted in suspicious occurrence time");
            }

            if (temporalized == null) {
                /*throw new RuntimeException("temporal leak:\n" + premise +
                        "\n\trule: " + rule.source +
                        "\n\ttask: " + premise.task() +
                        "\n\tbelief: " + premise.belief + "\t" + premise.beliefTerm() +
                        "\n\tderived: " + content);*/

                return; //aborted by temporalization
            }

            //apply the confidence scale
            if (truth!=null) {
                float projection;
                if (Global.REDUCE_TRUTH_BY_TEMPORAL_DISTANCE && premise.isEvent()) {
                    projection = Revision.truthProjection(premise.task().occurrence(), premise.belief().occurrence(), nar.time());
                } else {
                    projection = 1f;
                }
                truth = truth.confMultViaWeightMaxEternal(confScale[0] * projection);
                if (truth == null) {
                    throw new RuntimeException("temporal leak: " + premise);
                    //return;
                }
            }

            //NOTE: if temporalized, the content term will be the unique Term (NOT Termed)
            //else it will stay as the Concept itself (Termed<> already stored before)
            if (temporalized.hasTemporal())
                content = temporalized;

            occ = occReturn[0];
        } else {
            occ = ETERNAL;
        }

        nar.process(
            premise.derive(content, truth, budget, nar.time(), occ, m, this)
        );



    }


}

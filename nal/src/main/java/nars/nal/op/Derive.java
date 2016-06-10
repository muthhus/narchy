package nars.nal.op;

import com.google.common.base.Joiner;
import nars.NAR;
import nars.Op;
import nars.budget.Budget;
import nars.nal.ConceptProcess;
import nars.nal.TimeFunction;
import nars.nal.meta.PremiseEval;
import nars.nal.meta.PremiseRule;
import nars.nal.meta.ProcTerm;
import nars.task.Revision;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.AtomicStringConstant;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

import static nars.Op.*;
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
    @NotNull
    private final TimeFunction temporalizer;

    /**
     * result pattern
     */
    @NotNull
    public final Term conclusionPattern;


    /**
     * whether this a single or double premise derivation; necessary in case premise
     * does have a belief but it was not involved in determining Truth
     */
    public final boolean beliefSingle, goalSingle;
    //private final ImmutableSet<Term> uniquePatternVar;


    public Derive(@NotNull PremiseRule rule, @NotNull Term term,
                  boolean beliefSingle, boolean goalSingle, boolean eternalize, @NotNull TimeFunction temporalizer) {
        this.rule = rule;


        this.conclusionPattern = term;
        //this.uniquePatternVar = Terms.unique(term, (Term x) -> x.op() == VAR_PATTERN);
        this.temporalizer = temporalizer;
        this.beliefSingle = beliefSingle;
        this.goalSingle = goalSingle;
        this.eternalize = eternalize;

        this.id = "Derive(" +
                Joiner.on(',').join(
                    term,
                    "temporal" + Integer.toHexString(temporalizer.hashCode()), //HACK todo until names are given to unique classes
                    beliefSingle ? "Bs" : "Bd",
                    goalSingle ? "Gs" : "Gd",
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

//        if (!uniquePatternVar.allSatisfy(m.xy::containsKey))
//            return;

        Term r = m.resolve(conclusionPattern);
        if (r instanceof Compound) { //includes null test

            // if (r.varPattern() != 0) return; //EXACTLY WHY DO WE TAKE THIS FAR TO DISCOVER THIS, CAN WE ELIMINATE USELESS WORK BY DISCOVERING ITS REASON

            derive(m, r);
        }

    }

    final void derive(@NotNull PremiseEval m, @NotNull Term raw) {
        ConceptProcess premise = m.premise;
        NAR nar = premise.nar();

        Truth truth = m.truth.get();

        if (raw.op() == NEG) {
            //negations cant term concepts or tasks, so we unwrap and invert the truth (fi
            raw = ((Compound)raw).term(0);
            if (!(raw instanceof Compound))
                return; //unwrapped to a variable
            if (truth!=null)
                truth = truth.negated();
        }

        //pre-filter invalid statements
        if (!Task.preNormalize(raw, nar))
            return;

        //get the normalized term to determine the budget (via it's complexity)
        //this way we can determine if the budget is insufficient
        //before conceptualizating in mem.taskConcept
        Termed<Compound> content = nar.index.normalized(raw);

        if (content == null)
            return; //HACK why would this happen?


        Budget budget = m.budget(truth, content);
        if (budget == null)
            return;

        long occ;

        if ((nar.nal() >= 7) && (premise.hasTemporality())) {

            long[] occReturn = new long[]{ETERNAL};
            float[] confScale = new float[] { 1f };

            Term temporalized = this.temporalizer.compute(content.term(),
                    m, this, occReturn, confScale
            );

            if (temporalized == null)
                return; //aborted by temporalization

            //apply the confidence scale
            if (truth!=null) {
                float projection;
                if (premise.isEvent()) {
                    projection = Revision.truthProjection(premise.task().occurrence(), premise.belief().occurrence(), nar.time());
                } else {
                    projection = 1f;
                }
                truth = truth.confMultViaWeightMaxEternal(confScale[0] * projection);
                if (truth == null)
                    return;
            }

            //NOTE: if temporalized, the content term will be the unique Term (NOT Termed)
            //else it will stay as the Concept itself (Termed<> already stored before)
            if (temporalized.hasTemporal())
                content = temporalized;

            occ = occReturn[0];
        } else {
            occ = ETERNAL;
        }

        premise.derive(content, truth, budget, nar.time(), occ, m, this);
    }


}

package nars.nal.op;

import com.google.common.base.Joiner;
import nars.*;
import nars.budget.Budget;
import nars.nal.Premise;
import nars.nal.TimeFunctions;
import nars.nal.meta.PremiseEval;
import nars.nal.meta.ProcTerm;
import nars.nal.rule.PremiseRule;
import nars.task.DerivedTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.InvalidTermException;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.AtomicStringConstant;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import nars.truth.func.TruthOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static nars.Op.ATOM;
import static nars.Op.NEG;
import static nars.nal.Tense.DTERNAL;
import static nars.nal.Tense.ETERNAL;
import static nars.nal.Tense.XTERNAL;

/**
 * Handles matched derivation results
 * < (&&, postMatch1, postMatch2) ==> derive(term) >
 */
public final class Derive extends AtomicStringConstant implements ProcTerm {


    final static Logger logger = LoggerFactory.getLogger(Derive.class);

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
    @Nullable
    private final TruthOperator belief;
    @Nullable
    private final TruthOperator goal;
    //private final ImmutableSet<Term> uniquePatternVar;


    public Derive(@NotNull PremiseRule rule, @NotNull Term term,
                  @Nullable TruthOperator belief, @Nullable TruthOperator goal, boolean eternalize, @NotNull TimeFunctions temporalizer) {
        super("Derive(" +
                Joiner.on(',').join(
                        term,
                        "temporal" + Integer.toHexString(temporalizer.hashCode()), //HACK todo until names are given to unique classes
                        belief != null ? belief : "_",
                        goal != null ? goal : "_",
                        eternalize ? "Et" : "_") +
                ')');
        this.rule = rule;

        this.belief = belief;
        this.goal = goal;

        this.conclusionPattern = term;


        //this.uniquePatternVar = Terms.unique(term, (Term x) -> x.op() == VAR_PATTERN);
        this.temporalizer = temporalizer;

        this.eternalize = eternalize;

    }



    @NotNull
    @Override
    public Op op() {
        return ATOM; //product?
    }


    /**
     * main entry point for derivation result handler.
     *
     * @return true to allow the matcher to continue matching,
     * false to stop it
     */
    @Override
    public final void accept(@NotNull PremiseEval m) {


        char c = m.punct.get();

        TruthOperator f;
        if (c == Symbols.BELIEF)
            f = belief;
        else if (c == Symbols.GOAL)
            f = goal;
        else
            f = null;

        Truth taskTruth, beliefTruth;

        //task truth is not involved in the outcome of this; set task truth to be null to prevent any negations below:
        taskTruth = (f == null) ? null : m.taskTruth;

        //truth function is single premise so set belief truth to be null to prevent any negations below:
        boolean single = f == null || f.single();
        beliefTruth = ((f == null) || single) ? null : m.beliefTruth;

        Term cp = this.conclusionPattern;


        Term r;
        try {
            r = m.index.resolve(cp, m);
        } catch (InvalidTermException e) {
            logger.warn("{}\n\tderiving rule {}", e.toString(), rule.source);
            return;
        }

        /*if (r == Imdex)
            System.err.println(r + " " + this.rule.source);*/ //<- finds rules which may help to add a neq(x,y) constraint


        if (r instanceof Compound) { //includes null test

            Truth t = (f == null) ?
                    null :
                    f.apply(
                            taskTruth,
                            beliefTruth,
                            m.nar,
                            m.confMin
                    );

            if (f == null || t != null)
                derive(m, (Compound) r, t, single);
        }

    }


    final void derive(@NotNull PremiseEval m, @NotNull Compound raw, @Nullable Truth truth, boolean single) {

        if (raw.op() == NEG) {
            //negations cant term concepts or tasks, so we unwrap and invert the truth (fi
            Term raw2 = raw.term(0);
            if (!(raw2 instanceof Compound))
                return; //unwrapped to a variable (negations of atomics are not allowed)
            raw = (Compound) raw2;
            if (truth != null)
                truth = truth.negated();
        }

        Budget budget = m.budget(truth, raw);
        if (budget == null)
            return; //INSUFFICIENT BUDGET

        NAR nar = m.nar;
        Compound content = Task.normalizeTaskTerm(raw, m.punct.get(), nar, true);
        if (content == null)
            return; //INVALID TERM FOR TASK

        long occ;
        Premise premise = m.premise;

        if (m.temporal) {
            if (nar.nal() < 7)
                throw new RuntimeException("invalid NAL level");

            long[] occReturn = new long[]{ETERNAL};
            float[] confScale = new float[]{1f};

            Compound temporalized;
            try {
                temporalized = this.temporalizer.compute(content,
                    m, this, occReturn, confScale
                );
            } catch (InvalidTermException e) {
                logger.warn("{}\n\ttemporalizing from {}\n\tderiving rule {}", e.toString(), content.term(), rule.source);
                return;
            }

            if (Param.DEBUG && occReturn[0] == DTERNAL) {
                //temporalizer.compute(content.term(), m, this, occReturn, confScale);
                throw new RuntimeException("temporalization resulted in suspicious occurrence time");
            }

            content = temporalized;

            if (content.dt() == XTERNAL) {
                //throw new InvalidTermException(content.op(), content.dt(), content.terms(), "XTERNAL leak");
                return;
            }

            //apply the confidence scale
            if (truth != null) {
                float projection;
                if (Param.REDUCE_TRUTH_BY_TEMPORAL_DISTANCE && premise.isEvent()) {
                    projection = TruthFunctions.projection(m.task.occurrence(), m.belief.occurrence(), nar.time());
                } else {
                    projection = 1f;
                }
                truth = truth.confMultViaWeightMaxEternal(confScale[0] * projection);
                if (truth == null) {
                    throw new RuntimeException("temporal leak: " + premise);
                    //return;
                }
            }



            occ = occReturn[0];
        } else {
            //the derived compound has a dt but the premise was entirely atemporal;
            // this (probably!) indicates a temporal placeholder in the rules that needs to be set to DTERNAL
            Op o = content.op();
            if (content.dt()==XTERNAL /*&& !o.isImage()*/) {
                Term ete = m.index.builder().build(o, DTERNAL, content.terms());
                if (!(ete instanceof Compound)) {
                    //throw new InvalidTermException(o, content.dt(), content.terms(), "untemporalization failed");
                    return;
                }
                content = (Compound) ete;
            }

            occ = ETERNAL;
        }



        m.conclusion.derive.add( //TODO we should not need to normalize the task, so process directly is preferred
                derive(content, truth, budget, nar.time(), occ, m, this, single)
        );


    }

    /**
     * part 2
     */
    @NotNull
    public final Task derive(@NotNull Termed<Compound> c, @Nullable Truth truth, @NotNull Budget budget, long now, long occ, @NotNull PremiseEval p, @NotNull Derive d, boolean single) {


        return newDerivedTask(c, p.punct.get(), truth, p, single)
                .time(now, occ)
                .budget(budget) // copied in, not shared
                //.anticipate(derivedTemporal && d.anticipate)
                .log(Param.DEBUG ? d.rule : null);


        //ETERNALIZE: (CURRENTLY DISABLED)

//        if ((occ != ETERNAL) && (truth != null) && d.eternalize  ) {


//            if (!derived.isDeleted()) {
//
//
//                nar.process(newDerivedTask(c, punct, new DefaultTruth(truth.freq(), eternalize(truth.conf())), parents)
//                        .time(now, ETERNAL)
//                        .budgetCompoundForward(budget, this)
//                        /*
//                TaskBudgeting.compoundForward(
//                        budget, truth(),
//                        term(), premise);*/
//                        .log("Immediaternalized") //Immediate Eternalization
//
//                );
//            }

//        }

    }


    public @NotNull DerivedTask newDerivedTask(@NotNull Termed<Compound> c, char punct, @Nullable Truth truth, PremiseEval p, boolean single) {
        return new DerivedTask.DefaultDerivedTask(c, punct, truth, p, p.evidence(single));
    }


}

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
import nars.task.DerivedTask;
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


    public final boolean eternalize;

    @NotNull
    public final PremiseRule rule;
    private final @NotNull TimeFunctions temporalizer;

    /**
     * result pattern
     */
    @NotNull
    public final Term conclusionPattern, conclusionPatternNP, conclusionPatternPN, conclusionPatternNN;


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
                        belief!=null ? belief : "_",
                        goal !=null ? goal : "_",
                        eternalize ? "Et" : "_") +
                ')');
        this.rule = rule;

        this.belief = belief;
        this.goal = goal;


        this.conclusionPattern = term;
        this.conclusionPatternNP = negateConclusion(true, false);
        this.conclusionPatternPN = negateConclusion(false, true);
        this.conclusionPatternNN = negateConclusion(true, true);


        //this.uniquePatternVar = Terms.unique(term, (Term x) -> x.op() == VAR_PATTERN);
        this.temporalizer = temporalizer;

        this.eternalize = eternalize;

    }

    private Term negateConclusion(boolean task, boolean belief) {

        @NotNull Term cp = this.conclusionPattern;

        if (cp.op().atomic)
            return null;
        if (cp.vars() > 0) //exclude vars for now, but this may be allowed if unification on the variable-containing superterm matches the task/belief pattern being negated, etc.
            return null;

        if (task) {
            Map<Term, Term> cc = new HashMap();
            Term taskPattern = this.rule.getTask();
            cc.put(taskPattern, $.neg(taskPattern));
            Term ccp = $.terms.remap(cp, cc);
            if (ccp.equals(cp)) {
                return null; //unaffects the conclusion, so the negation can't be captured by a negated subterm
            }
            cp = ccp;
        }
        if (belief) {
            Map<Term, Term> cc = new HashMap();
            @NotNull Term beliefPattern = this.rule.getBelief();
            cc.put(beliefPattern, $.neg(beliefPattern));
            Term ccp = $.terms.remap(cp, cc);
            if (ccp.equals(cp)) {
                return null; //unaffects the conclusion, so the negation can't be captured by a negated subterm
            }
            cp = ccp;
        }
        return cp;
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


        Truth taskTruth = m.taskTruth, beliefTruth = m.beliefTruth;

        char punct = m.punct.get();

        Term cp = this.conclusionPattern;

        if (!cp.op().atomic && (punct!=Symbols.QUEST && punct!=Symbols.QUESTION)) {

            boolean tn = taskTruth != null && taskTruth.isNegative();
            boolean bn = beliefTruth != null && beliefTruth.isNegative();
            if (bn && tn) {
                if (conclusionPatternNN!=null) {
                    cp = conclusionPatternNN;
                    taskTruth = taskTruth.negated();
                    beliefTruth = beliefTruth.negated();
                }
            } else if (bn) {
                if (conclusionPatternPN!=null) {
                    cp = conclusionPatternPN;
                    beliefTruth = beliefTruth.negated();
                }
            } else if (tn) {
                if (conclusionPatternNP!=null) {
                    cp = conclusionPatternNP;
                    taskTruth = taskTruth.negated();
                }
            }
        }


        Term r = m.index.resolve(cp, m);
        /*if (r == Imdex)
            System.err.println(r + " " + this.rule.source);*/ //<- finds rules which may help to add a neq(x,y) constraint


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

    final void derive(@NotNull PremiseEval m, @NotNull Compound raw, @Nullable Truth truth) {
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

        if ((nar.nal() >= 7) && (m.temporal)) {

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
                    projection = Revision.truthProjection(m.task.occurrence(), m.belief.occurrence(), nar.time());
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
            derive(content, truth, budget, nar.time(), occ, m, this)
        );


    }

    /** part 2 */
    @NotNull public final Task derive(@NotNull Termed<Compound> c, @Nullable Truth truth, @NotNull Budget budget, long now, long occ, @NotNull PremiseEval p, @NotNull Derive d) {


        return newDerivedTask(c, p.punct.get(), truth, p)
                .time(now, occ)
                .budget(budget) // copied in, not shared
                //.anticipate(derivedTemporal && d.anticipate)
                .log(Global.DEBUG ? d.rule : "Derived");


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


    public @NotNull DerivedTask newDerivedTask(@NotNull Termed<Compound> c, char punct, @Nullable Truth truth, PremiseEval p) {
        return new DerivedTask.DefaultDerivedTask(c, punct, truth, p);
        //return new DerivedTask.CompetingDerivedTask(c, punct, truth, p);
    }




}

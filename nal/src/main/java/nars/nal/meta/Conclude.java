package nars.nal.meta;

import com.google.common.base.Joiner;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.budget.Budget;
import nars.concept.TruthDelta;
import nars.nal.Premise;
import nars.nal.rule.PremiseRule;
import nars.task.DerivedTask;
import nars.term.Compound;
import nars.term.InvalidTermException;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.AtomicStringConstant;
import nars.time.TimeFunctions;
import nars.truth.Truth;
import nars.util.Texts;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static nars.Op.ATOM;
import static nars.Op.NEG;
import static nars.time.Tense.*;

/**
 * Final conclusion step of the derivation process that produces a derived task
 *
 * Each rule corresponds to a unique instance of this
 */
public final class Conclude extends AtomicStringConstant implements BoolCondition {


    public final static Logger logger = LoggerFactory.getLogger(Conclude.class);

    @Deprecated public final boolean eternalize;

    @NotNull
    public final PremiseRule rule;
    private final @NotNull TimeFunctions temporalizer;

    /**
     * result pattern
     */
    @NotNull
    public final Term conclusionPattern;


    //private final ImmutableSet<Term> uniquePatternVar;


    public Conclude(@NotNull PremiseRule rule, @NotNull Term term,
                    @Deprecated boolean eternalize, @NotNull TimeFunctions temporalizer) {
        super("Derive(" +
                Joiner.on(',').join(
                        term,
                        "temporal" + Integer.toHexString(temporalizer.hashCode()) //HACK todo until names are given to unique classes
                        //belief != null ? belief : "_",
                        //goal != null ? goal : "_",
                        //eternalize ? "Et" : "_") +
                ) +
                ')');
        this.rule = rule;

//        this.belief = belief;
//        this.goal = goal;

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
    public final boolean booleanValueOf(@NotNull PremiseEval m, int now) {

        Term r = m.index.resolve(this.conclusionPattern, m);

        if (r instanceof Compound) { //includes null test
            PremiseEval.TruthPuncEvidence ct = m.punct.get();
            derive(m, (Compound) r, ct);
        }

        return true;
    }


    final void derive(@NotNull PremiseEval m, @NotNull Compound content, @NotNull PremiseEval.TruthPuncEvidence ct) {

        Truth truth = ct.truth;
        if (content.op() == NEG) {
            //negations cant term concepts or tasks, so we unwrap and invert the truth (fi
            Term raw2 = content.term(0);
            if (!(raw2 instanceof Compound))
                return; //unwrapped to a variable (negations of atomics are not allowed)
            content = (Compound) raw2;
            if (truth != null)
                truth = truth.negated();
        }


        Budget budget = m.budget(truth, content);
        if (budget == null)
            return; //INSUFFICIENT BUDGET

        NAR nar = m.nar;


        if (!Task.taskContentPreTest(content, ct.punc, nar, true /* !Param.DEBUG*/))
            return; //INVALID TERM FOR TASK


        long occ;
        Premise premise = m.premise;

        if (m.temporal) {
            if (nar.nal() < 7)
                throw new RuntimeException("invalid NAL level");

            long[] occReturn = {ETERNAL};
            float[] confScale = {1f};

            Compound temporalized = this.temporalizer.compute(content,
                    m, this, occReturn, confScale
            );

            if (Param.DEBUG && occReturn[0] != ETERNAL && Math.abs(occReturn[0] - DTERNAL) < 1000) {
                //temporalizer.compute(content.term(), m, this, occReturn, confScale); //leave this commented for debugging
                throw new RuntimeException("temporalization resulted in suspicious occurrence time");
            }

            content = temporalized;

            if (content.dt() == XTERNAL || content.dt() == -XTERNAL) {
                throw new InvalidTermException(content.op(), content.dt(), content.terms(), "XTERNAL leak");
                //return;
            }

            //apply the confidence scale
            if (truth != null) {
                //float projection;
                //projection =
                        //Param.REDUCE_TRUTH_BY_TEMPORAL_DISTANCE && premise.isEvent() ? TruthFunctions.projection(m.task.occurrence(), m.belief.occurrence(), nar.time()) : 1f;
                float cf = confScale[0];
                if (cf != 1) {
                    truth = truth.confMultViaWeightMaxEternal(cf);
                    if (truth == null) {
                        throw new RuntimeException("temporal leak: " + premise);
                        //return;
                    }
                }
            }


            occ = occReturn[0];
        } else {
            //the derived compound indicated a potential dt, but the premise was actually atemporal;
            // this indicates a temporal placeholder (XTERNAL) in the rules which needs to be set to DTERNAL

            Op o = content.op();
            if (content.dt() == XTERNAL /*&& !o.isImage()*/) {
                Term ete = m.index.the(o, DTERNAL, content.terms());
                if (!(ete instanceof Compound)) {
                    //throw new InvalidTermException(o, content.dt(), content.terms(), "untemporalization failed");
                    return;
                }
                content = (Compound) ete;
            }

            occ = ETERNAL;
        }


        DerivedTask d = derive(content, budget, nar.time(), occ, m, truth, ct.punc, ct.evidence);
        if (d != null)
            m.conclusion.derive.accept(d);

    }

    /**
     * part 2
     */
    @Nullable
    public final DerivedTask derive(@NotNull Termed<Compound> c, @NotNull Budget budget, long now, long occ, @NotNull PremiseEval p, Truth truth, char punc, long[] evidence) {


        try {
            DerivedTask dt =
                    new DerivedTask.DefaultDerivedTask(c, truth, punc, evidence, p);
                    //new RuleFeedbackDerivedTask(c, truth, punc, evidence, p, rule);

            dt.time(now, occ)
                    .budget(budget) // copied in, not shared
                    //.anticipate(derivedTemporal && d.anticipate)
                    .log(Param.DEBUG ? rule : null);

            return dt;
        } catch (RuntimeException e) {
            if (Param.DEBUG) {
                logger.error("{}", e.toString());
            }
            return null;
        }


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

    public static class RuleFeedbackDerivedTask extends DerivedTask.DefaultDerivedTask {

        private final @NotNull PremiseRule rule;

        public RuleFeedbackDerivedTask(@NotNull Termed<Compound> tc, @Nullable Truth truth, char punct, long[] evidence, @NotNull PremiseEval premise, @NotNull PremiseRule rule) {
            super(tc, truth, punct, evidence, premise);
            this.rule = rule;
        }

        @Override
        public void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar) {
            if (!isDeleted())
                Conclude.feedback(premise, rule, this, delta, deltaConfidence, deltaSatisfaction, nar);
            super.feedback(delta, deltaConfidence, deltaSatisfaction, nar);

        }
    }

    static class RuleStats {
        final SummaryStatistics pri = new SummaryStatistics();
        final SummaryStatistics dSat = new SummaryStatistics();
        final SummaryStatistics dConf = new SummaryStatistics();

        public long count() {
            return dSat.getN();
        }

    }

    static final Map<NAR, Map<PremiseRule,RuleStats>> stats = new ConcurrentHashMap();

    private static void feedback(Premise premise, @NotNull PremiseRule rule, RuleFeedbackDerivedTask t, TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar) {
        Map<PremiseRule, RuleStats> x = stats.computeIfAbsent(nar, n -> new ConcurrentHashMap<>());

        RuleStats s = x.computeIfAbsent(rule, d -> new RuleStats());

        s.pri.addValue( t.pri() );

        if (delta!=null) {
            s.dSat.addValue( Math.abs(deltaSatisfaction) );
            s.dConf.addValue( Math.abs(deltaConfidence) );
        }

    }

    static public void printStats(NAR nar) {
        stats.get(nar).forEach((r, s) -> {
            long n = s.count();

            System.out.println(
                    r + "\t" +
                    Texts.n4(s.pri.getSum()) + "\t" +
                    Texts.n4(s.dConf.getSum()) + "\t" +
                    Texts.n4(s.dSat.getSum()) + "\t" +
                    n
                    //" \t " + mean +
                    );
        });
    }
}

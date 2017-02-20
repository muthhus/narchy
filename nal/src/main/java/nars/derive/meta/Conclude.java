package nars.derive.meta;

import com.google.common.base.Joiner;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.budget.Budget;
import nars.derive.rule.PremiseRule;
import nars.index.term.TermIndex;
import nars.premise.Derivation;
import nars.task.DerivedTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.AtomicStringConstant;
import nars.term.util.InvalidTermException;
import nars.time.TimeFunctions;
import nars.truth.Truth;
import nars.util.task.InvalidTaskException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nars.Op.ATOM;
import static nars.Op.NEG;
import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.*;

/**
 * Final conclusion step of the derivation process that produces a derived task
 * <p>
 * Each rule corresponds to a unique instance of this
 */
public final class Conclude extends AtomicStringConstant implements BoolCondition {


    public final static Logger logger = LoggerFactory.getLogger(Conclude.class);


    @NotNull
    public final PremiseRule rule;
    private final @NotNull TimeFunctions time;

    /**
     * result pattern
     */
    @NotNull
    public final Term conclusionPattern;


    //private final ImmutableSet<Term> uniquePatternVar;


    public Conclude(@NotNull PremiseRule rule, @NotNull Term term,
                    @NotNull TimeFunctions time) {
        super("Derive(" +
                Joiner.on(',').join(
                        term,
                        "time" + Integer.toHexString(time.hashCode()) //HACK todo until names are given to unique classes
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
        this.time = time;


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
    public final boolean run(@NotNull Derivation m) {

        NAR nar = m.nar;

        if (rule.minNAL <= nar.level()) { //HACK

            try {
                //TODO make a variation of transform which can terminate early if exceeds a minimum budget threshold
                //  which is already determined bythe constructed term's growing complexity) in m.budget()

                Term r = term(m);

                if (r instanceof Compound) {

                    Compound term = (Compound) r;

                    //note: the budget function used here should not depend on the truth's frequency. btw, it may be inverted below
                    Compound crr = compoundOrNull(nar.concepts.eval(term));
                    if (crr == null) {
                        throw new InvalidTermException(r.op(), DTERNAL, "normalization failed", (term).terms());
                    }

                    Derivation.TruthPuncEvidence ct = m.punct.get();
                    Truth truth = ct.truth;
                    Budget budget = m.premise.budget(crr, truth, m);
                    if (budget != null) {


                        Op o = term.op();
                        if (o == NEG) {
                            term = compoundOrNull(term.unneg());
                            if (term == null)
                                throw new RuntimeException("unneg resulted in null compound");

                            if (truth != null)
                                truth = truth.negated();
                        }

                        m.premise.accept(new Conclusion(term, ct.punc, truth, budget, ct.evidence, rule));
                    }
                }
            } catch (@NotNull InvalidTermException | InvalidTaskException e) {
                if (Param.DEBUG_EXTRA)
                    logger.warn("{} {}", m, e.getMessage());
            }
        }

        return true;
    }

    public @Nullable Term term(@NotNull Derivation m) {
        if (conclusionPattern instanceof Compound) {
            return TermIndex.transform((Compound)this.conclusionPattern, m);
        } else {
            return m.xy.get(conclusionPattern); //single variable, just lookup
        }
    }



//    public static class RuleFeedbackDerivedTask extends DerivedTask.DefaultDerivedTask {
//
//        private final @NotNull PremiseRule rule;
//
//        public RuleFeedbackDerivedTask(@NotNull Termed<Compound> tc, @Nullable Truth truth, char punct, long[] evidence, @NotNull Derivation premise, @NotNull PremiseRule rule, long now, long[] occ) {
//            super(tc, truth, punct, evidence, premise, now, occ);
//            this.rule = rule;
//        }
//
//        @Override
//        public void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar) {
//            if (!isDeleted())
//                Conclude.feedback(premise, rule, this, delta, deltaConfidence, deltaSatisfaction, nar);
//            super.feedback(delta, deltaConfidence, deltaSatisfaction, nar);
//
//        }
//    }
//
//    static class RuleStats {
//        final SummaryStatistics pri = new SummaryStatistics();
//        final SummaryStatistics dSat = new SummaryStatistics();
//        final SummaryStatistics dConf = new SummaryStatistics();
//
//        public long count() {
//            return dSat.getN();
//        }
//
//    }
//
//    static final Map<NAR, Map<PremiseRule, RuleStats>> stats = new ConcurrentHashMap();
//
//    private static void feedback(Premise premise, @NotNull PremiseRule rule, @NotNull RuleFeedbackDerivedTask t, @Nullable TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar) {
//        Map<PremiseRule, RuleStats> x = stats.computeIfAbsent(nar, n -> new ConcurrentHashMap<>());
//
//        RuleStats s = x.computeIfAbsent(rule, d -> new RuleStats());
//
//        s.pri.addValue(t.pri());
//
//        if (delta != null) {
//            s.dSat.addValue(Math.abs(deltaSatisfaction));
//            s.dConf.addValue(Math.abs(deltaConfidence));
//        }
//
//    }
//
//    static public void printStats(NAR nar) {
//        stats.get(nar).forEach((r, s) -> {
//            long n = s.count();
//
//            System.out.println(
//                    r + "\t" +
//                            Texts.n4(s.pri.getSum()) + '\t' +
//                            Texts.n4(s.dConf.getSum()) + '\t' +
//                            Texts.n4(s.dSat.getSum()) + '\t' +
//                            n
//                    //" \t " + mean +
//            );
//        });
//    }


//    final static HashBag<PremiseRule> posGoal = new HashBag();
//    final static HashBag<PremiseRule> negGoal = new HashBag();
//    static {
//
//        Runtime.getRuntime().addShutdownHook(new Thread(()-> {
//            System.out.println("POS GOAL:\n" + print(posGoal));
//            System.out.println("NEG GOAL:\n" + print(negGoal));
//        }));
//    }
//
//    private static String print(HashBag<PremiseRule> h) {
//        return Joiner.on("\n").join(h.topOccurrences(h.size())) + "\n" + h.size() + " total";
//    }
}

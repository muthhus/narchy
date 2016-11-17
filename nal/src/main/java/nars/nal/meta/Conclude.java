package nars.nal.meta;

import com.google.common.base.Joiner;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.budget.Budget;
import nars.nal.Premise;
import nars.nal.rule.PremiseRule;
import nars.task.DerivedTask;
import nars.task.util.InvalidTaskException;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.AtomicStringConstant;
import nars.term.compound.GenericCompound;
import nars.term.util.InvalidTermException;
import nars.time.TimeFunctions;
import nars.truth.Truth;
import nars.truth.TruthDelta;
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
import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.*;

/**
 * Final conclusion step of the derivation process that produces a derived task
 * <p>
 * Each rule corresponds to a unique instance of this
 */
public final class Conclude extends AtomicStringConstant implements BoolCondition {


    public final static Logger logger = LoggerFactory.getLogger(Conclude.class);

    @Deprecated
    public final boolean eternalize;

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
                    @Deprecated boolean eternalize, @NotNull TimeFunctions time) {
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
    public final boolean run(@NotNull PremiseEval m, int now) {

        NAR nar = m.nar;

        if (rule.minNAL <= nar.level()) { //HACK

            try {
                Term r = m.index.transform(this.conclusionPattern, m);

                if (r instanceof Compound) { //includes null test

                    if (r.volume() < nar.compoundVolumeMax.intValue())

                        derive(m, (Compound) r, m.punct.get()); //Term exceeds maximum volume

                }
            } catch (@NotNull InvalidTermException | InvalidTaskException e) {
                if (Param.DEBUG_EXTRA)
                    logger.warn("{} {}", m, e.toString());
            }
        }

        return true;
    }


    final void derive(@NotNull PremiseEval m, @NotNull Compound content, @NotNull PremiseEval.TruthPuncEvidence ct) {

        Truth truth = ct.truth;


        //note: the budget function used here should not depend on the truth's frequency. btw, it may be inverted below
        Budget budget = m.budget(truth, content);
        if (budget == null)
            return; //INSUFFICIENT BUDGET

        NAR nar = m.nar;

        content = nar.normalize(content); //TODO why isnt this sometimes normalized by here
        if (content == null)
            return; //somehow became null

        if (!Task.taskContentValid(content, ct.punc, nar, !Param.DEBUG))
            return;

        Op o = content.op();
        if (o == NEG) {
            content = compoundOrNull(content.unneg());
            if (content == null)
                return; //??

            if (truth!=null)
                truth = truth.negated();
            o = content.op();
        }

//this is performed on input also
//        if (!Task.taskContentValid(content, ct.punc, nar, false/* !Param.DEBUG*/))
//            return; //INVALID TERM FOR TASK

        long occ;

        if (m.temporal) {
//            if (nar.level() < 7)
//                throw new NAR.InvalidTaskException(content, "invalid NAL level");

            long[] occReturn = {ETERNAL};
            float[] confScale = {1f};

            Compound temporalized = this.time.compute(content,
                    m, this, occReturn, confScale
            );

            //temporalization failure, could not determine temporal attributes. seems this can happen normally
            if ((temporalized == null) || ((long)temporalized.dt()) == -((long)DTERNAL) /* long cast here due to integer wraparound */ ) {
//                if (temporalized!=null) {
//                    //FOR DEBUGGING, re-run it
//                    Compound temporalized2 = this.time.compute(content,
//                            m, this, occReturn, confScale
//                    );
//                }

                throw new InvalidTermException(content.op(), content.dt(), content.terms(),
                        "temporalization failure" + (Param.DEBUG ? rule : ""));
            }

            int tdt = temporalized.dt();
            if (tdt == XTERNAL || tdt == -XTERNAL) {
                throw new InvalidTermException(content.op(), content.dt(), content.terms(), "XTERNAL/DTERNAL leak");
            }

//            if (Param.DEBUG && occReturn[0] != ETERNAL && Math.abs(occReturn[0] - DTERNAL) < 1000) {
//                //temporalizer.compute(content.term(), m, this, occReturn, confScale); //leave this commented for debugging
//                throw new NAR.InvalidTaskException(content, "temporalization resulted in suspicious occurrence time");
//            }

            if (temporalized != content) {
                ((GenericCompound) (content = temporalized)).setNormalized();
            }


            //apply any non 1.0 the confidence scale
            if (truth != null) {

                float cf = confScale[0];
                if (cf != 1) {
                    throw new UnsupportedOperationException("yet");
//                    truth = truth.confMultViaWeightMaxEternal(cf);
//                    if (truth == null) {
//                        throw new InvalidTaskException(content, "temporal leak");
//                    }
                }
            }

            occ = occReturn[0];

        } else {

            occ = ETERNAL;
        }


        //the derived compound indicated a potential dt, but the premise was actually atemporal;
        // this indicates a temporal placeholder (XTERNAL) in the rules which needs to be set to DTERNAL
        if (content.dt() == XTERNAL /*&& !o.isImage()*/) {
            Term ete = m.index.the(o, DTERNAL, content.terms());
            if (!(ete instanceof Compound)) {
                throw new InvalidTermException(o, DTERNAL, content.terms(), "untemporalization failed");
            }
            content = nar.normalize((Compound) ete);
        }


        DerivedTask d = derive(content, budget, nar.time(), occ, m, truth, ct.punc, ct.evidence);
        if (d != null)
            m.target.accept(d);
    }


    /**
     * part 2
     */
    @Nullable
    public final DerivedTask derive(@NotNull Termed<Compound> c, @NotNull Budget budget, long now, long occ, @NotNull PremiseEval p, Truth truth, char punc, long[] evidence) {

        DerivedTask dt =
                new DerivedTask.DefaultDerivedTask(c, truth, punc, evidence, p);
        //new RuleFeedbackDerivedTask(c, truth, punc, evidence, p, rule);

        dt.time(now, occ)
                .budget(budget) // copied in, not shared
                //.anticipate(derivedTemporal && d.anticipate)
                .log(Param.DEBUG ? rule : null);


//            //TEMPORARY MEASUREMENT
//            if (dt.isGoal()) {
//               synchronized (posGoal) {
//                   ((dt.freq() >= 0.5f) ? posGoal : negGoal).addOccurrences(rule, (int)(Math.abs(dt.freq()-0.5f)*100));
//               }
//            }
//            //</TEMPORARY MEASUREMENT

        return dt;


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

    static final Map<NAR, Map<PremiseRule, RuleStats>> stats = new ConcurrentHashMap();

    private static void feedback(Premise premise, @NotNull PremiseRule rule, @NotNull RuleFeedbackDerivedTask t, @Nullable TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar) {
        Map<PremiseRule, RuleStats> x = stats.computeIfAbsent(nar, n -> new ConcurrentHashMap<>());

        RuleStats s = x.computeIfAbsent(rule, d -> new RuleStats());

        s.pri.addValue(t.pri());

        if (delta != null) {
            s.dSat.addValue(Math.abs(deltaSatisfaction));
            s.dConf.addValue(Math.abs(deltaConfidence));
        }

    }

    static public void printStats(NAR nar) {
        stats.get(nar).forEach((r, s) -> {
            long n = s.count();

            System.out.println(
                    r + "\t" +
                            Texts.n4(s.pri.getSum()) + '\t' +
                            Texts.n4(s.dConf.getSum()) + '\t' +
                            Texts.n4(s.dSat.getSum()) + '\t' +
                            n
                    //" \t " + mean +
            );
        });
    }


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

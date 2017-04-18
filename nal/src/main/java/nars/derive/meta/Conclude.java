package nars.derive.meta;

import com.google.common.base.Joiner;
import jcog.pri.Priority;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.derive.rule.PremiseRule;
import nars.premise.Derivation;
import nars.premise.TruthPuncEvidence;
import nars.task.DerivedTask;
import nars.task.util.InvalidTaskException;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.AtomicStringConstant;
import nars.term.util.InvalidTermException;
import nars.time.TimeFunctions;
import nars.truth.Truth;
import nars.truth.func.TruthOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nars.Op.ATOM;
import static nars.Op.NEG;
import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * Final conclusion step of the derivation process that produces a derived task
 * <p>
 * Each rule corresponds to a unique instance of this
 */
public final class Conclude extends AtomicStringConstant implements BoolPredicate<Derivation> {


    public final static Logger logger = LoggerFactory.getLogger(Conclude.class);


    @NotNull
    public final PremiseRule rule;
    private final @NotNull TimeFunctions time;

    /**
     * result pattern
     */
    @NotNull
    public final Term conclusionPattern;

    public final TruthOperator belief, goal;


    //private final ImmutableSet<Term> uniquePatternVar;


    public Conclude(@NotNull PremiseRule rule, PostCondition p,
                    TruthOperator belief, TruthOperator goal,
                    @NotNull TimeFunctions time) {
        super("Derive(" +
                Joiner.on(',').join(
                        p.pattern,
                        "time" + Integer.toHexString(time.hashCode()) //HACK todo until names are given to unique classes

                        //belief != null ? belief : "_",
                        //goal != null ? goal : "_",
                        //eternalize ? "Et" : "_") +
                ) +
                ')');

        this.rule = rule;

        this.belief = belief;
        this.goal = goal;

        this.conclusionPattern = p.pattern;


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
    public final boolean test(@NotNull Derivation m) {

        NAR nar = m.nar;

        if (rule.minNAL > nar.level())  //HACK
            return true;

        try {
            //TODO make a variation of transform which can terminate early if exceeds a minimum budget threshold
            //  which is already determined bythe constructed term's growing complexity) in m.budget()

            Compound cr = Task.content(m.index.transform(this.conclusionPattern, m), nar);
            if (cr == null)
                return true;

            //note: the budget function used here should not depend on the truth's frequency. btw, it may be inverted below
            cr = nar.concepts.eval(cr);
            if (cr == null)
                return true;

            TruthPuncEvidence ct = m.punct.get();
            Truth truth = ct.truth;
            byte punc = ct.punc;

            Priority priority = m.budgeting.budget(m, cr, truth, punc);
            if (priority == null)
                return true;

            derive(m, cr, truth, priority, punc, ct.evidence); //continue to stage 2


        } catch (@NotNull InvalidTermException | InvalidTaskException e) {
            if (Param.DEBUG_EXTRA)
                logger.warn("{} {}", m, e.getMessage());
        }

        return true;
    }


    /**
     * 2nd-stage
     */
    final void derive(@NotNull Derivation m, @NotNull final Compound _content, Truth truth, Priority priority, byte punc, long[] evidence) {

        Compound content = _content;

        NAR nar = m.nar;


        long[] occ;

        if (m.temporal) {
//            if (nar.level() < 7)
//                throw new NAR.InvalidTaskException(content, "invalid NAL level");


            //process time with the unnegated term
            Op o = content.op();
            Compound temporalized;
            boolean negated;
            if (o == NEG) {
                temporalized = Task.content(content.unneg(), nar);
                if (temporalized == null) {
                    temporalized = content; //use as-is since it cant be decompressed unnegated
                    negated = false;
                } else
                    negated = true;
            } else {
                negated = false;
                temporalized = content;
            }

            long[] occReturn = {ETERNAL, ETERNAL};
            float[] confScale = {1f};

            temporalized = this.time.compute(temporalized,
                    m, this, occReturn, confScale
            );

            //temporalization failure, could not determine temporal attributes. seems this can happen normally
            if ((temporalized == null) /*|| (Math.abs(occReturn[0]) > 2047483628)*/ /* long cast here due to integer wraparound */) {
//                 {
//                    //FOR DEBUGGING, re-run it
//                    Compound temporalized2 = this.time.compute(content,
//                            m, this, occReturn, confScale
//                    );
//                }

                throw new InvalidTermException(content.op(), content.dt(), "temporalization failure" + (Param.DEBUG ? rule : ""), content.terms()
                );
            }

            int tdt = temporalized.dt();
            if (tdt == XTERNAL || tdt == -XTERNAL) {
                throw new InvalidTermException(content.op(), content.dt(), "XTERNAL/DTERNAL leak", content.terms());
            }

//            if (Param.DEBUG && occReturn[0] != ETERNAL && Math.abs(occReturn[0] - DTERNAL) < 1000) {
//                //temporalizer.compute(content.term(), m, this, occReturn, confScale); //leave this commented for debugging
//                throw new NAR.InvalidTaskException(content, "temporalization resulted in suspicious occurrence time");
//            }

            //apply any non 1.0 the confidence scale
            if (truth != null) {

                if (negated)
                    truth = truth.negated();

                float cf = confScale[0];
                if (cf != 1) {
                    throw new UnsupportedOperationException("yet");

//                    truth = truth.confMultViaWeightMaxEternal(cf);
//                    if (truth == null) {
//                        throw new InvalidTaskException(content, "temporal leak");
//                    }
                }
            }

            content = temporalized;

            occ = occReturn;

        } else {

            occ = null;
        }


        content = Task.content(content, nar);
        if (content == null)
            return;

        //the derived compound indicated a potential dt, but the premise was actually atemporal;
        // this indicates a temporal placeholder (XTERNAL) in the rules which needs to be set to DTERNAL
        if (content.hasTemporal()) {
            content = m.index.retemporalize(content);
            if (content == null)
                return;
        } else if (!content.isNormalized()) {
            content = compoundOrNull(m.index.normalize(content));
            if (content == null)
                return;
        }

        DerivedTask d = derive(content, priority, nar.time(), occ, m, truth, punc, evidence);

        if (d != null)
            m.target.accept(d);
    }


    /**
     * part 2
     */
    @Nullable
    public final DerivedTask derive(@NotNull Compound cc, @NotNull Priority priority, long now, long[] occ, @NotNull Derivation p, Truth truth, byte punc, long[] evidence) {

        long start, end;
        if (occ != null) {
            start = occ[0];
            end = occ[1];
            if (end == ETERNAL)
                end = start;
        } else
            start = end = ETERNAL;


        DerivedTask d =
                new DerivedTask.DefaultDerivedTask(cc, truth, punc, evidence, p, now, start, end);


        //new RuleFeedbackDerivedTask(c, truth, punc, evidence, p, rule);
        d.setPriority(priority);
        //        if (srcCopy == null) {
//            delete();
//        } else {
//            float p = srcCopy.priSafe(-1);
//            if (p < 0) {
//                delete();
//            } else {
//                setPriority(p);
//            }
//        }
//
//        return this;
        //.anticipate(derivedTemporal && d.anticipate)

        if (Param.DEBUG)
            d.log(rule);

//            //TEMPORARY MEASUREMENT
//            if (dt.isGoal()) {
//               synchronized (posGoal) {
//                   ((dt.freq() >= 0.5f) ? posGoal : negGoal).addOccurrences(rule, (int)(Math.abs(dt.freq()-0.5f)*100));
//               }
//            }
//            //</TEMPORARY MEASUREMENT

        return d;


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

//    public static class RuleFeedbackDerivedTask extends DerivedTask.DefaultDerivedTask {
//
//        private final @NotNull PremiseRule rule;
//
//        public RuleFeedbackDerivedTask(@NotNull Termed<Compound> tc, @Nullable Truth truth, byte punct, long[] evidence, @NotNull Derivation premise, @NotNull PremiseRule rule, long now, long[] occ) {
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

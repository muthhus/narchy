package nars.derive.meta;

import jcog.pri.Priority;
import nars.*;
import nars.derive.rule.PremiseRule;
import nars.index.term.TermIndex;
import nars.premise.Derivation;
import nars.premise.TruthPuncEvidence;
import nars.task.DerivedTask;
import nars.task.util.InvalidTaskException;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.InvalidTermException;
import nars.time.Tense;
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
public final class Conclude extends AbstractPred<Derivation> {


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
        super($.func("derive", p.pattern, $.the("time" + time.toString() )));

        this.rule = rule;

        this.belief = belief;
        this.goal = goal;

        this.conclusionPattern = p.pattern;


        //this.uniquePatternVar = Terms.unique(term, (Term x) -> x.op() == VAR_PATTERN);
        this.time = time;


    }


    /**
     * @return true to allow the matcher to continue matching,
     * false to stop it
     */
    @Override
    public final boolean test(@NotNull Derivation m) {

        //int start = m.now();
        accept(m);
        //m.revert(start);

        return true;
    }

    /**
     * start for derivation concluder
     */
    private void accept(@NotNull Derivation m) {
        NAR nar = m.nar;

        if (rule.minNAL > nar.level())  //HACK
            return;

        try {
            //TODO make a variation of transform which can terminate early if exceeds a minimum budget threshold
            //  which is already determined bythe constructed term's growing complexity) in m.budget()

            TermIndex index = m.index;

            TruthPuncEvidence ct = m.punct;
            if (ct == null)
                return;

            Term b0 = this.conclusionPattern;

            for (int i = 0; i < 2; i++) { //repeat necessary for second-layer unification
                Term bp = b0;
                b0 = compoundOrNull(m.transform(b0, index));
                if (b0 == null)
                    return;
                if (b0 == conclusionPattern)
                    return;
                if (b0 == bp)
                    break; //no change
            }

            Compound c0 = (Compound) b0;

            final Compound c1 = compoundOrNull(c0.eval(index));
            if (c1 != null) {


                Truth truth = ct.truth;

                @NotNull final long[] occ;

                final Compound c2;
                if (m.temporal) {
                    //            if (nar.level() < 7)
                    //                throw new NAR.InvalidTaskException(content, "invalid NAL level");


                    //process time with the unnegated term
                    Op o = c1.op();
                    Compound temporalized;
                    boolean negated;
                    if (o == NEG) {
                        temporalized = Task.content(c1.unneg(), nar);
                        if (temporalized == null) {
                            temporalized = c1; //use as-is since it cant be decompressed unnegated
                            negated = false;
                        } else
                            negated = true;
                    } else {
                        negated = false;
                        temporalized = c1;
                    }

                    long[] occReturn = {ETERNAL, ETERNAL};
                    float[] confScale = {1f};

                    @Nullable Compound temporalized2 = this.time.compute(temporalized,
                            m, this, occReturn, confScale
                    );

                    //temporalization failure, could not determine temporal attributes. seems this can happen normally
                    if ((temporalized2 == null) /*|| (Math.abs(occReturn[0]) > 2047483628)*/ /* long cast here due to integer wraparound */) {
                        //                 {
                        //                    //FOR DEBUGGING, re-run it
                        //                    Compound temporalized2 = this.time.compute(content,
                        //                            m, this, occReturn, confScale
                        //                    );
                        //                }

//                            throw new InvalidTermException(c1.op(), c1.dt(), "temporalization failure"
//                                    //+ (Param.DEBUG ? rule : ""), c1.toArray()
//                            );

                        return;
                    } else {
                        temporalized = temporalized2;
                    }

                    int tdt = temporalized.dt();
                    if (tdt == XTERNAL || tdt == -XTERNAL) {
                        throw new InvalidTermException(c1.op(), c1.dt(), "XTERNAL/DTERNAL leak");
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


                    if (occReturn[1] == ETERNAL)
                        occReturn[1] = occReturn[0];

                    occ = occReturn;
                    c2 = temporalized;

                } else {
                    occ = Tense.ETERNAL_RANGE;
                    c2 = c1;
                }


                Compound c3 = Task.content(c2, nar);
                if (c3 != null) {

                    //the derived compound indicated a potential dt, but the premise was actually atemporal;
                    // this indicates a temporal placeholder (XTERNAL) in the rules which needs to be set to DTERNAL
                    if (c3.isTemporal()) {
                        c3 = m.index.retemporalize(c3); //retemporalize does normalize at the end
                    } else if (!c3.isNormalized()) {
                        c3 = m.index.normalize(c3);
                    }


                    if (c3 != null) {

                        if (c3.op() == NEG) {
                            c3 = compoundOrNull(c3.unneg());
                            if (c3 == null)
                                return;

                            if (truth!=null)
                                truth = truth.negated();
                        }

//                            c3 = index.normalize(c3);
//                            if (c3 == null)  return;

                        if (!Task.taskContentValid(c3, ct.punc, nar, true)) {
                            return;
                        }

                        long start = occ[0];
                        long end = occ[1];
                        if (start!=ETERNAL && start > end) {
                            //TODO why does this happen
                            long t = start; start = end; end = t; //swap
                        }


                        //note: the budget function used here should not depend on the truth's frequency. btw, it may be inverted below
                        // also confidence should not be changed after this budgeting
                        byte punc = ct.punc;
                        Priority priority = m.budgeting.budget(m, c3, truth, punc, start, end);

                        if (priority != null) {

                            DerivedTask d =
                                    new DerivedTask.DefaultDerivedTask(
                                            c3, truth, punc,
                                            ct.evidence, m, nar.time(), start, end);

                            d.setPriority(priority);

                            if (Param.DEBUG)
                                d.log(rule);

                            m.derive(d);

                        }
                    }
                }
            }

        } catch (InvalidTermException | InvalidTaskException e) {
            if (Param.DEBUG_EXTRA)
                logger.warn("{} {}", m, e.getMessage());
        }

    }


    @NotNull
    @Override
    public Op op() {
        return ATOM; //product?
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

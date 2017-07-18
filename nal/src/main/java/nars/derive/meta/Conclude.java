package nars.derive.meta;

import jcog.pri.Priority;
import nars.*;
import nars.control.premise.Derivation;
import nars.derive.rule.PremiseRule;
import nars.op.DepIndepVarIntroduction;
import nars.task.DebugDerivedTask;
import nars.task.DerivedTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.time.Tense;
import nars.time.TimeFunctions;
import nars.truth.Truth;
import nars.truth.func.TruthOperator;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nars.Op.ATOM;
import static nars.Op.NEG;
import static nars.term.Terms.compoundOrNull;
import static nars.term.Terms.normalizedOrNull;
import static nars.time.Tense.ETERNAL;

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

    private final boolean varIntro;


    @NotNull
    public final Term conclusionPattern;

    public final TruthOperator belief, goal;


    public Conclude(@NotNull PremiseRule rule, @NotNull Term conclusionPattern,
                    @Nullable TruthOperator belief, @Nullable TruthOperator goal,
                    @NotNull TimeFunctions time) {
        super($.func((Atomic) $.the("conc"),
                conclusionPattern, $.the(/*"time" + */time.toString())));

        this.rule = rule;
        this.time = time;

        this.belief = belief;
        this.goal = goal;

        Term pp = conclusionPattern;

        //HACK unwrap varIntro so we can apply it at the end of the derivation process, not before like other functors
        Pair<Atomic, Compound> outerFunctor = Op.functor(pp, $.terms, false);
        if (outerFunctor != null && outerFunctor.getOne().toString().equals("varIntro")) {
            varIntro = true;
            pp = outerFunctor.getTwo().sub(0);
        } else {
            varIntro = false;
        }

        this.conclusionPattern = pp;
    }


    /**
     * @return true to allow the matcher to continue matching,
     * false to stop it
     */
    @Override
    public final boolean test(@NotNull Derivation d) {

        NAR nar = d.nar;

        nar.emotion.derivation1.increment();

        if (rule.minNAL > nar.level())  //HACK
            return true;

        //TODO make a variation of transform which can terminate early if exceeds a minimum budget threshold
        //  which is already determined bythe constructed term's growing complexity) in m.budget()

        Term b0 = this.conclusionPattern;
        Term b1 = compoundOrNull(d.transform(b0));
        if (b1 == null)
            return true;

        assert(b1.varPattern() == 0);

        Compound c1 = compoundOrNull(b1.eval(d));
        if (c1 == null)
            return true;

        boolean polarity = true;
        if (c1.op()==NEG) {
            c1 = compoundOrNull( c1.unneg() );
            if (c1==null)
                return true;
            polarity = false;
        }


        nar.emotion.derivation2.increment();

        Truth truth = d.concTruth;


        @NotNull final long[] occ;

        Compound c2;
        if (d.temporal) {

            occ = Tense.ETERNAL_RANGE.clone();

            //process time with the unnegated term
            Op o = c1.op();


            float[] confScale = {1f};

            @Nullable Compound t1 = this.time.compute(c1,
                    d, this, occ, confScale
            );

            //temporalization failure, could not determine temporal attributes. seems this can happen normally
            if ((t1 == null) /*|| (Math.abs(occReturn[0]) > 2047483628)*/ /* long cast here due to integer wraparound */) {
                //                 {
                //                    //FOR DEBUGGING, re-run it
                //                    Compound temporalized2 = this.time.compute(content,
                //                            m, this, occReturn, confScale
                //                    );
                //                }

//                            throw new InvalidTermException(c1.op(), c1.dt(), "temporalization failure"
//                                    //+ (Param.DEBUG ? rule : ""), c1.toArray()
//                            );

                return true;
            }

//            int tdt = t2.dt();
//            if (tdt == XTERNAL || tdt == -XTERNAL) {
//                //throw new InvalidTermException(c1.op(), c1.dt(), "XTERNAL/DTERNAL leak");
//                return true;
//            }

            //            if (Param.DEBUG && occReturn[0] != ETERNAL && Math.abs(occReturn[0] - DTERNAL) < 1000) {
            //                //temporalizer.compute(content.term(), m, this, occReturn, confScale); //leave this commented for debugging
            //                throw new NAR.InvalidTaskException(content, "temporalization resulted in suspicious occurrence time");
            //            }

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


            if (occ[1] == ETERNAL) occ[1] = occ[0];

            c2 = t1;

        } else {
            occ = Tense.ETERNAL_RANGE;
            c2 = c1;
        }

        if (varIntro) {
            Compound Cv = normalizedOrNull(DepIndepVarIntroduction.varIntro(c2, nar), d.terms,
                    d.temporal ? d.terms.retemporalizationZero : d.terms.retemporalizationDTERNAL //select between eternal and parallel depending on the premises's temporality
            );

            if (Cv == null || Cv.equals(c2) /* keep only if it differs */)
                return true;

            c2 = Cv;
        }

        byte punc = d.concPunc;
        @Nullable ObjectBooleanPair<Compound> c3n = Task.tryContent(c2, punc, d.terms, true);
        if (c3n != null) {

            nar.emotion.derivation3.increment();

            boolean negating = c3n.getTwo();

            final Compound C = c3n.getOne();


            long start = occ[0];
            long end = occ[1];
            if (start != ETERNAL && end < start) { //why?
                long s = start;
                start = end;
                end = s; //swap
            }

            float priority = d.parentPri; //d.budgeting.budget(d, C, truth, punc, start, end);
            if (priority == priority) {

                if (priority < Priority.EPSILON) {
                    return true; //wasted
                }


                if (truth!=null) {

                    if (negating)
                        polarity = !polarity;

                    if (!polarity)
                        truth = truth.negated();
                }

                DerivedTask t =
                        Param.DEBUG ?
                                new DebugDerivedTask(C, punc, truth, d, start, end, d.cause.id) :
                                new DerivedTask(C, punc, truth, d, start, end, d.cause.id);

                if (t.equals(d.task) || t.equals(d.belief)) {
                    return true; //created a duplicate of the task
                }

                t.setPri(priority);

                if (Param.DEBUG)
                    t.log(rule);

                d.accept(t);
                d.use(Param.TTL_DERIVE_TASK);
            }
        }

        //        } catch (InvalidTermException | InvalidTaskException e) {
//            if (Param.DEBUG_EXTRA)
//                logger.warn("{} {}", m, e.getMessage());
//        }

        d.use(Param.TTL_DERIVE_TASK_FAIL);
        return true;
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

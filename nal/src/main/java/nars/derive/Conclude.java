package nars.derive;

import nars.$;
import nars.NAR;
import nars.Op;
import nars.control.Derivation;
import nars.derive.rule.PremiseRule;
import nars.term.Term;
import nars.term.Termed;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.NotNull;

/**
 * Conclusion builder
 */
public final class Conclude {

    private static final Term VAR_INTRO = $.the("varIntro");
    private static final Term GOAL_URGENT = $.the("urgent");

    static public PrediTerm<Derivation> the(@NotNull PremiseRule rule, @NotNull Term pattern, boolean goalUrgent, NAR nar) {

        //substitute occurrences of the exact task and belief terms with the short-cut
        pattern = pattern.replace(rule.getTask(), Derivation._taskTerm).replace(rule.getBelief(), Derivation._beliefTerm);

        //HACK unwrap varIntro so we can apply it at the end of the derivation process, not before like other functors
        boolean introVars;
        Pair<Termed, Term> outerFunctor = Op.functor(pattern, (i)->i.equals(VAR_INTRO) ? VAR_INTRO : null);
        if (outerFunctor != null) {
            introVars = true;
            pattern = outerFunctor.getTwo().sub(0);
        } else {
            introVars = false;
        }

        Term id = !goalUrgent ? $.func("derive", pattern) : $.func("derive", pattern, $.the("urgent"));

             //input.privaluate = false; //disable priority affect, since feedback is applied in other more direct ways (ex: deriver backpressure)
        //input.valueBias = 0;//-0.1f;
        Taskify taskify = new Taskify(rule, nar.newChannel(rule));

        Term concID =
                !goalUrgent ?
                    $.func("derive", /*$.the(cid), */pattern/* prod args */) :
                    $.func("derive", /*$.the(cid), */pattern/* prod args */, GOAL_URGENT);
        return AndCondition.the(
                new Conclusion(concID,pattern, goalUrgent),
                introVars ? //Fork.fork(
                        AndCondition.the(new IntroVars(), taskify)
                        //makeTask)
                        : taskify
        );

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

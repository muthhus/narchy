package nars.derive;

import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.control.CauseChannel;
import nars.derive.rule.PremiseRule;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.container.TermContainer;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Conclusion builder
 */
public final class Conclude extends ProxyTerm implements Function<NAR,Conclusion> {

    @NotNull public final PremiseRule rule;

    public final boolean varIntro;
    public final boolean goalUrgent;

    @NotNull public final Term pattern;


    public Conclude(@NotNull PremiseRule rule, @NotNull Term pattern, boolean goalUrgent) {

        super(!goalUrgent ? $.func("derive", pattern) : $.func("derive", pattern, $.the("urgent")));

        this.rule = rule;

//        this.belief = belief;
//        this.goal = goal;

        Term pp = pattern;

        //HACK unwrap varIntro so we can apply it at the end of the derivation process, not before like other functors
        Pair<Atom, TermContainer> outerFunctor = Op.functor(pp, $.terms, false);
        if (outerFunctor != null && outerFunctor.getOne().toString().equals("varIntro")) {
            varIntro = true;
            pp = outerFunctor.getTwo().sub(0);
        } else {
            varIntro = false;
        }

        this.goalUrgent = goalUrgent;
        this.pattern = pp;
    }


    @Override
    public Conclusion apply(@NotNull NAR nar) {
        CauseChannel<Task> input = nar.newChannel(rule);
        return new Conclusion(this, input);
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

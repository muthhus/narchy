//package nars.derive;
//
//import jcog.meter.event.PeriodMeter;
//import jcog.version.VersionMap;
//import nars.$;
//import AndCondition;
//import nars.derive.meta.BoolPred;
//import Fork;
//import nars.premise.Derivation;
//import nars.term.Term;
//import nars.term.var.Variable;
//import org.eclipse.collections.api.tuple.Twin;
//import org.eclipse.collections.impl.tuple.Tuples;
//import org.jetbrains.annotations.NotNull;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.function.Function;
//
//import static java.util.stream.Collectors.toList;
//
//
//public class DeriverTransform implements Function<TrieDeriver,TrieDeriver> {
//
//    private final Function<BoolPred, BoolPred> transform;
//
//    public DeriverTransform(Function<BoolPred,BoolPred> xy) {
//        this.transform = xy;
//    }
//
//    public TrieDeriver apply(TrieDeriver x) {
//        BoolPred[] y = instrument(x.roots().clone());
//        return new TrieDeriver(y);
//    }
//
//    private BoolPred[] instrument(BoolPred[] roots) {
//        for (int i = 0, rootsLength = roots.length; i < rootsLength; i++) {
//            roots[i] = instrument(roots[i]);
//        }
//        return roots;
//    }
//
//    private BoolPred instrument(@NotNull BoolPred b) {
//
//        if (b instanceof Fork) {
//            Fork f = (Fork)b;
//            return Fork.compile(instrument(f.cached));
//        } else if (b instanceof AndCondition) {
//            AndCondition f = (AndCondition)b;
//            return Fork.compile(instrument(f.termCache));
//        } else {
//            return transform.apply(b);
//        }
//    }
//
//
//    public static class TracedBoolPred extends InstrumentedBoolPred<Derivation> {
//
//        private final Logger logger;
//
//        public TracedBoolPred(BoolPred b) {
//            super(b);
//            logger = LoggerFactory.getLogger(ref.getClass());
//        }
//
//        @Override
//        public boolean test(Derivation x) {
//            boolean y;
//            try {
//                y = ref.test(x);
//            } catch (Throwable t) {
//                logger.error("{}", t);
//                throw t;
//            }
//
//            if (!y) {
//                logger.info("{} FALSE {} {} xy={}", x.now(), x.task, x.beliefTerm, bindings(x.xy));
//            }
//
//            return y;
//        }
//
//        private List<Twin<Term>> bindings(@NotNull VersionMap<Term, Term> xy) {
//            return xy.entrySet().stream()
//                    .filter(p -> (p.getKey() instanceof Variable))
//                    .map(p -> Tuples.twin(p.getKey(), p.getValue()))
//                    .collect(toList());
//        }
//    }
//
//    public static class TimedBoolPred extends InstrumentedBoolPred<Derivation> {
//
//        private final PeriodMeter hits;
//
//        public TimedBoolPred(BoolPred<Derivation> b) {
//            super(b);
//
//            synchronized (predicates) {
//                hits = predicates.computeIfAbsent(b, (B) -> new PeriodMeter(b.toString(), -1));
//            }
//        }
//
//        @Override
//        public boolean test(Derivation p) {
//            long start = System.nanoTime();
//            boolean result = ref.test(p);
//            long end = System.nanoTime();
//            long time = end - start;
//
//            synchronized (hits) {
//                hits.hitNano(time);
//            }
//            return result;
//        }
//        static final Map<BoolPred, PeriodMeter> predicates = new ConcurrentHashMap();
//        static {
//            Runtime.getRuntime().addShutdownHook(new Thread(()->{
//                print();
//            }));
//        }
//
//        public static void print() {
//
//            int os = predicates.size();
//            List<BoolPred> bySum = $.newArrayList(os);
//
//            predicates.forEach((k,m) -> {
//                bySum.add(k);
//            });
//
//            List<BoolPred> byAvg = $.newArrayList();
//            byAvg.addAll(bySum);
//
//            byAvg.sort((BoolPred a, BoolPred b) -> {
//                return Double.compare(predicates.get(b).mean(), predicates.get(a).mean());
//            });
//            bySum.sort((BoolPred a, BoolPred b) -> {
//                return Double.compare(predicates.get(b).sum(), predicates.get(a).sum());
//            });
//
//            System.out.println("DERIVER AVERAGES");
//            byAvg.subList(0, 10).forEach(x -> System.out.println(predicates.get(x)));
//            System.out.println("DERIVER SUMS");
//            bySum.subList(0, 10).forEach(x -> System.out.println(predicates.get(x)));
//
//        }
//
//    }
//}

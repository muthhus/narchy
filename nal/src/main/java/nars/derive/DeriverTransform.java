package nars.derive;

import jcog.meter.event.PeriodMeter;
import jcog.version.VersionMap;
import nars.$;
import nars.derive.meta.AndCondition;
import nars.derive.meta.BoolPredicate;
import nars.derive.meta.Fork;
import nars.premise.Derivation;
import nars.term.Term;
import nars.term.var.Variable;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;


public class DeriverTransform implements Function<TrieDeriver,TrieDeriver> {

    private final Function<BoolPredicate, BoolPredicate> transform;

    public DeriverTransform(Function<BoolPredicate,BoolPredicate> xy) {
        this.transform = xy;
    }

    public TrieDeriver apply(TrieDeriver x) {
        BoolPredicate[] y = instrument(x.roots.clone());
        return new TrieDeriver(y);
    }

    private BoolPredicate[] instrument(BoolPredicate[] roots) {
        for (int i = 0, rootsLength = roots.length; i < rootsLength; i++) {
            roots[i] = instrument(roots[i]);
        }
        return roots;
    }

    private BoolPredicate instrument(@NotNull BoolPredicate b) {

        if (b instanceof Fork) {
            Fork f = (Fork)b;
            return Fork.compile(instrument(f.termCache));
        } else if (b instanceof AndCondition) {
            AndCondition f = (AndCondition)b;
            return Fork.compile(instrument(f.termCache));
        } else {
            return transform.apply(b);
        }
    }


    public static class TracedBoolPredicate extends InstrumentedBoolPredicate<Derivation> {

        private final Logger logger;

        public TracedBoolPredicate(BoolPredicate b) {
            super(b);
            logger = LoggerFactory.getLogger(ref.getClass());
        }

        @Override
        public boolean test(Derivation x) {
            boolean y;
            try {
                y = ref.test(x);
            } catch (Throwable t) {
                logger.error("{}", t);
                throw t;
            }

            if (!y) {
                logger.info("{} FALSE {} {} xy={}", x.now(), x.task, x.beliefTerm, bindings(x.xy));
            }

            return y;
        }

        private List<Twin<Term>> bindings(@NotNull VersionMap<Term, Term> xy) {
            return xy.entrySet().stream()
                    .filter(p -> (p.getKey() instanceof Variable))
                    .map(p -> Tuples.twin(p.getKey(), p.getValue()))
                    .collect(toList());
        }
    }

    public static class TimedBoolPredicate extends InstrumentedBoolPredicate<Derivation> {

        private final PeriodMeter hits;

        public TimedBoolPredicate(BoolPredicate<Derivation> b) {
            super(b);

            synchronized (predicates) {
                hits = predicates.computeIfAbsent(b, (B) -> new PeriodMeter(b.toString(), -1));
            }
        }

        @Override
        public boolean test(Derivation p) {
            long start = System.nanoTime();
            boolean result = ref.test(p);
            long end = System.nanoTime();
            long time = end - start;

            synchronized (hits) {
                hits.hitNano(time);
            }
            return result;
        }
        static final Map<BoolPredicate, PeriodMeter> predicates = new ConcurrentHashMap();
        static {
            Runtime.getRuntime().addShutdownHook(new Thread(()->{
                print();
            }));
        }

        public static void print() {

            int os = predicates.size();
            List<BoolPredicate> bySum = $.newArrayList(os);

            predicates.forEach((k,m) -> {
                bySum.add(k);
            });

            List<BoolPredicate> byAvg = $.newArrayList();
            byAvg.addAll(bySum);

            byAvg.sort((BoolPredicate a, BoolPredicate b) -> {
                return Double.compare(predicates.get(b).mean(), predicates.get(a).mean());
            });
            bySum.sort((BoolPredicate a, BoolPredicate b) -> {
                return Double.compare(predicates.get(b).sum(), predicates.get(a).sum());
            });

            System.out.println("DERIVER AVERAGES");
            byAvg.subList(0, 10).forEach(x -> System.out.println(predicates.get(x)));
            System.out.println("DERIVER SUMS");
            bySum.subList(0, 10).forEach(x -> System.out.println(predicates.get(x)));

        }

    }
}

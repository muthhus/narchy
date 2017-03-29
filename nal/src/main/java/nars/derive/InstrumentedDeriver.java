package nars.derive;

import jcog.meter.event.HitMeter;
import jcog.meter.event.PeriodMeter;
import nars.$;
import nars.derive.meta.AndCondition;
import nars.derive.meta.BoolPredicate;
import nars.derive.meta.Fork;
import nars.premise.Derivation;
import nars.term.ProxyTerm;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by me on 3/28/17.
 */
public class InstrumentedDeriver extends TrieDeriver {

    public InstrumentedDeriver(TrieDeriver t) {
        super(instrument(t.roots.clone()));
    }

    private static BoolPredicate[] instrument(BoolPredicate[] roots) {
        for (int i = 0, rootsLength = roots.length; i < rootsLength; i++) {
            roots[i] = instrument(roots[i]);
        }
        return roots;
    }

    private static BoolPredicate instrument(BoolPredicate b) {

        if (b instanceof Fork) {
            Fork f = (Fork)b;
            return Fork.compile(instrument(f.termCache));
        } else if (b instanceof AndCondition) {
            AndCondition f = (AndCondition)b;
            return Fork.compile(instrument(f.termCache));
        } else {
            return new InstrumentedBoolPredicate(b);
        }
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

    private static class InstrumentedBoolPredicate extends ProxyTerm implements BoolPredicate {

        private final BoolPredicate b;
        private final PeriodMeter hits;


        public InstrumentedBoolPredicate(BoolPredicate b) {
            super(b);
            this.b = b;

            synchronized (predicates) {
                hits = predicates.computeIfAbsent(b, (B) -> new PeriodMeter(b.toString(), -1));
            }
        }

        @Override
        public boolean test(Object p) {
            long start = System.nanoTime();
            boolean result = b.test(p);
            long end = System.nanoTime();
            long time = end - start;

            synchronized (hits) {
                hits.hitNano(time);
            }
            return result;
        }
    }
}

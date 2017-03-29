package nars.derive;

import jcog.meter.event.HitMeter;
import jcog.meter.event.PeriodMeter;
import nars.derive.meta.AndCondition;
import nars.derive.meta.BoolPredicate;
import nars.derive.meta.Fork;
import nars.premise.Derivation;
import nars.term.ProxyTerm;
import org.jetbrains.annotations.NotNull;

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
            predicates.forEach((k,m)->{
               System.out.println(m.sum() + "\t" + m.mean() + "\t" + k);
            });
        }));
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

package nars.util.data;

import jcog.data.FloatParam;
import jcog.meter.event.PeriodMeter;
import nars.budget.Budget;
import nars.budget.Budgeted;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * mixes inputs from different identified sources in different amounts
 * @K source identifier
 * @P type of mixable content
 */
public class Mix<K, P extends Budgeted>  {


    public class MixStream extends FloatParam implements Consumer<P> {

        public final PeriodMeter priMeterIn, priMeterOut;
        public final PeriodMeter quaMeter;
        public final K source;

        MixStream(K source, int window) {
            super(1f, 0f, 2f);
            this.source = source;
            priMeterIn = new PeriodMeter("priIn", window);
            priMeterOut = new PeriodMeter("priOut", window);
            quaMeter = new PeriodMeter( "qua", window);
        }

        public Stream<P> input(Stream<P> x) {
            return x.peek(this);
        }

        public P input(P x) {
            accept(x);
            return x;
        }

        @Override
        public void accept(P xx) {
            if (xx == null)
                return;

            float g = floatValue();

            Budget budget = xx.budget();
            priMeterIn.hit(budget.priSafe(0));
            priMeterOut.hit(budget.priSafe(0) * g);
            quaMeter.hit(budget.qua());

            if (g <= 0) {

            } else {
                if (g != 1f) { //TODO epsilon?
                    budget.priMult(g);
                }
            }

        }

        /** reset gathered statistics */
        public void commit() {
            priMeterIn.clear();
            priMeterOut.clear();
            //quaMeter.clear();
        }
    }

    public final Map<K, MixStream> streams = new ConcurrentHashMap();
    //TODO use a WeakValue map?

    final static int WINDOW = 64; //changing this affects the temporal precision

    /** gets or creates a mix stream for the given key */
    public MixStream stream(K x) {
        return streams.computeIfAbsent(x, xx -> new MixStream(xx, WINDOW));
    }

    /** reset gathered statistics */
    public void commit() {
        streams.forEach((k,s)->s.commit());
    }

}

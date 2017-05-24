package nars.util.data;

import jcog.data.FloatParam;
import jcog.meter.event.PeriodMeter;
import jcog.pri.Prioritized;
import jcog.pri.Priority;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * mixes inputs from different identified sources in different amounts
 * @K source identifier
 * @P type of mixable content
 */
public class Mix<K, P extends Prioritized>  {


    public static class MixStream<K,P extends Prioritized> extends FloatParam implements Consumer<P>, Function<P,P> {

        public final PeriodMeter priMeterIn, priMeterOut;
        public final K source;

        MixStream(K source, int window) {
            super(1f, 0f, 2f);
            this.source = source;
            priMeterIn = new PeriodMeter("priIn", window);
            priMeterOut = new PeriodMeter("priOut", window);
        }

        public Stream<P> apply(Stream<P> x) {
            return x.peek(this);
        }

        @Override public P apply(P x) {
            accept(x);
            return x;
        }

        public P[] apply(P[] x) {
            for (P y : x)
                accept(y);
            return x;
        }


        public final void input(Stream<P> x, Consumer<Stream<P>> target) {
            target.accept(apply(x));
        }
        public final void input(P x, Consumer<P> target) {
            target.accept(apply(x));
        }
        public final void input(P[] x, Consumer<P[]> target) {
            target.accept(apply(x));
        }

        @Override
        public void accept(P xx) {
            if (xx == null)
                return;

            float g = floatValue();

            Priority priority = xx.priority();
            float p = priority.priSafe(0);
            priMeterIn.hit(p);
            priMeterOut.hit(p * g);

            if (p <= 0 || g <= 0) {

            } else {
                priority.priMult(g);
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

    final static int WINDOW = 8; //changing this affects the temporal precision

    /** gets or creates a mix stream for the given key */
    public MixStream stream(K x) {
        return streams.computeIfAbsent(x, xx -> new MixStream(xx, WINDOW));
    }

    /** reset gathered statistics */
    public void commit() {
        streams.forEach((k,s)->s.commit());
    }

}

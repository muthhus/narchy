package nars.bag.leak;

import jcog.bag.Bag;
import jcog.data.FloatParam;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

import static nars.time.Tense.ETERNAL;

/**
 * drains items from a Bag at
 */
public class Leak<X, Y> {

    //private static final Logger logger = LoggerFactory.getLogger(MutaTaskBag.class);

    @NotNull
    public final Bag<X, Y> bag;


    /**
     * asynchronously controlled implementation of Leak which
     * decides demand according to time elapsed (stored as some 'long' value)
     * since a previous call, and a given rate parameter.
     * if the rate * elapsed dt will not exceed the provided maxCost
     * value, which can be POSITIVE_INFINITY (by default).
     * <p>
     * draining the input bag
     */
    public static abstract class DtLeak<X, Y> extends Leak<X, Y> {


        @NotNull
        public final FloatParam rate /* items per dt */;
        @NotNull
        public final FloatParam minBudget;

        protected long lastLeak = ETERNAL;

        public DtLeak(@NotNull Bag<X, Y> bag, @NotNull FloatParam rate) {
            this(bag, rate, new FloatParam(1));
        }

        /**
         * rate = max successful leaks per duration
         *
         * @param bag
         * @param rate
         * @param minBudget
         */
        public DtLeak(@NotNull Bag<X, Y> bag, @NotNull FloatParam rate, FloatParam minBudget) {
            super(bag);
            this.rate = rate;
            this.minBudget = minBudget;
        }

        private final AtomicBoolean busy = new AtomicBoolean(false);

        public void commit(long now, int dur) {

            if (!busy.compareAndSet(false, true))
                return;

            try {
                bag.commit();

                if (bag.size() >= min()) {

                    long last = this.lastLeak;
                    if (last == ETERNAL) {
                        this.lastLeak = last = now;
                    }

                    float dt = Math.max(0, (now - last) / ((float) dur));

                    float budget = rate.floatValue() * dt;
                    float minBudget = this.minBudget.floatValue();
                    if (budget >= minBudget) {

                        final float[] spent = {0};
                        bag.sample((v) -> {
                            float cost = onOut(v);
                            assert(cost <= minBudget);
                            float spe = spent[0] + cost;
                            if (spe < budget) {
                                spent[0] = spe;
                                return Bag.BagCursorAction.Remove; //continue
                            } else {
                                return Bag.BagCursorAction.RemoveAndStop;
                            }
                        });

                        if (spent[0] > 0) {
                            this.lastLeak = now; //only set time if some cost was spent
                        }
                    }

                }
            } finally {
                busy.set(false);
            }
        }


        /**
         * returns a cost value, in relation to the bag sampling parameters, which is subtracted
         * from the rate each iteration. this can allow proportional consumption of
         * a finitely allocated resource.
         */
        abstract protected float onOut(@NotNull Y b);

    }

    /**
     * rate = max successful leaks per duration
     */
    public Leak(@NotNull Bag<X, Y> bag) {
        this.bag = bag;
    }

    /**
     * minimum bag size allowed before leak
     */
    public int min() {
        return 1;
    }

    public void setCapacity(int capacity) {
        bag.setCapacity(capacity);
    }

    public void clear() {
        bag.clear();
    }
}

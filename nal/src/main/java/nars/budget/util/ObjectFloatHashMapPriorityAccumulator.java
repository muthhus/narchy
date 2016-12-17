package nars.budget.util;

import nars.$;
import nars.Param;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by me on 12/16/16.
 */
public class ObjectFloatHashMapPriorityAccumulator<X> extends AtomicReference<ObjectFloatHashMap<X>> implements PriorityAccumulator<X> {


    public ObjectFloatHashMapPriorityAccumulator() {
        super(null);
        commit();
    }

    static final int INITIAL_SIZE = 16;

    @Override
    @Nullable
    public Iterable<ObjectFloatPair<X>> commit() {
        ObjectFloatHashMap<X> prevMap = this.getAndSet(new ObjectFloatHashMap<>(INITIAL_SIZE));
        return prevMap != null && !prevMap.isEmpty() ? postprocess(prevMap) : null;
    }

    static final class LightObjectFloatPair<Z> implements ObjectFloatPair<Z> {

        private final float val;
        private final Z the;

        LightObjectFloatPair(Z the, float val) {
            this.val = val;
            this.the = the;
        }

        @Override
        public Z getOne() {
            return the;
        }

        @Override
        public float getTwo() {
            return val;
        }

        @Override
        public int compareTo(ObjectFloatPair<Z> zObjectFloatPair) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return the + "=" + val;
        }
    }

    protected Iterable<ObjectFloatPair<X>> postprocess(ObjectFloatHashMap<X> m) {

        //final float idealAvgPri = 0.5f;
        final float thresh = Param.BUDGET_EPSILON;

        //MutableFloatCollection values = m.values();
        int n = m.size();
        float normFactor =
                //(float) ( (n * idealAvgPri )/ values.sum());
                (float) (1f / m.sum());

        List<ObjectFloatPair<X>> l = $.newArrayList(n);
        m.forEachKeyValue((k, v) -> {
            float vn = v * normFactor;
            if (vn >= thresh)
                l.add(new LightObjectFloatPair<>(k, vn));
        });

        //m.clear(); //?<- helpful?

        return l; //m.keyValuesView();

    }

    @Override
    public void add(X x, float v) {
        get().addToValue(x, v);
    }
}

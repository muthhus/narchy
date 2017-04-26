package jcog.bag.impl.hijack;

import jcog.pri.PLink;
import jcog.pri.RawPLink;
import org.eclipse.collections.api.block.procedure.primitive.ObjectLongProcedure;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * TODO add an instrumentation wrapper to collect statistics
 * about cache efficiency and also processing time of the calculations
 */
public class HijackMemoize<K,V> extends PriorityHijackBag<K,PLink<Pair<K,V>>> implements Function<K,V> {

    float CACHE_HIT_BOOST;
    float CACHE_DENY_DAMAGE; //damage taken by a cell in rejecting an attempted hijack

    final Function<K,V> func;

    public final LongAdder
            hit = new LongAdder(),  //existing item retrieved
            miss = new LongAdder(),  //a new item inserted that has not existed
            reject = new LongAdder(), //item prevented from insertion by existing items
            evict = new LongAdder(); //removal of existing item on insertion of new item

                //hit + miss + reject = total insertions


    public HijackMemoize(int initialCapacity, int reprobes, @NotNull Function<K, V> f) {
        super(reprobes);
        setCapacity(initialCapacity);
        this.func = f;
    }

    public void statReset(ObjectLongProcedure<String> eachStat) {
        //eachStat.accept("S" /* size */, size() );
        eachStat.accept("H" /* hit */, hit.sumThenReset() );
        eachStat.accept("M" /* miss */, miss.sumThenReset() );
        eachStat.accept("R" /* reject */, reject.sumThenReset() );
        eachStat.accept("E" /* evict */, evict.sumThenReset() );
    }

    @Override
    public boolean setCapacity(int i) {
        if (super.setCapacity(i)) {
            this.CACHE_HIT_BOOST = i > 0 ? reprobes / (float)Math.sqrt(i) : 0;
            this.CACHE_DENY_DAMAGE = CACHE_HIT_BOOST;
            return true;
        }
        return false;
    }

    @Override
    @Nullable public V apply(@NotNull K k) {
        PLink<Pair<K, V>> exists = get(k);
        if (exists!=null) {
            exists.priAdd(CACHE_HIT_BOOST);
            hit.increment();
            return exists.get().getTwo();
        } else {
            V v = func.apply(k);
            if (put(new RawPLink<>(Tuples.pair(k, v), value(k)))!=null) {
                miss.increment();
            } else {
                reject.increment();
            }
            return v;
        }
    }

    /** quickly estimates the value of computing the input.
     * easier items will introduce lower priority, allowing
     * harder items to sustain longer
     * */
    public float value(@NotNull K k) {
        return reprobes * 2 * CACHE_HIT_BOOST;
    }

    @Override
    protected boolean replace(PLink<Pair<K, V>> incoming, PLink<Pair<K, V>> existing, float scale) {
        if (!super.replace(incoming, existing, scale)) {
            existing.priSub(CACHE_DENY_DAMAGE);
            return false;
        }
        return true;
    }


    @NotNull
    @Override
    public K key(PLink<Pair<K,V>> value) {
        return value.get().getOne();
    }


    @Override
    protected Consumer<PLink<Pair<K, V>>> forget(float rate) {
        return null;
    }

    @Override
    public void onRemoved(@NotNull PLink<Pair<K, V>> value) {
        evict.increment();
    }

    /** clears the statistics */
    public String summary() {
        StringBuilder sb = new StringBuilder(32);
        statReset((k,v) -> {
            sb.append(k).append('=').append(v).append(' ');
        });
        sb.setLength(sb.length()-1); //remove last ' '
        return sb.toString();
    }
}

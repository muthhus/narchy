package nars.bag;

import jcog.Util;
import jcog.bag.impl.ArrayBag;
import jcog.pri.Priority;
import jcog.pri.op.PriMerge;
import jcog.util.QueueLock;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Random;

abstract public class ConcurrentArrayBag<K,X extends Priority> extends ArrayBag<K,X> {

    private final QueueLock<X> toPut;

    public ConcurrentArrayBag(@NotNull PriMerge mergeFunction, @NotNull Map<K, X> map, Random rng, int cap) {
        super(mergeFunction, map);
        setCapacity(cap);

        this.toPut = new QueueLock<X>(Util.blockingQueue(cap), super::putAsync, (batchSize) -> {
            if (mustSort) {
                synchronized (items) {
                    super.ensureSorted();
                }
            }
        });
    }

    @Override
    protected void ensureSorted() {
        //sort elides until after batchFinished
    }

    @Override
    public void putAsync(@NotNull X b) {
        toPut.accept(b);
    }



}


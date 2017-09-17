package jcog.bag.impl;

import jcog.Util;
import jcog.pri.Priority;
import jcog.pri.op.PriMerge;
import jcog.util.QueueLock;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Random;

/**
 * adds a QueueLock wrapping the putAsync methods
 */
public class ConcurrentCurveBag<X extends Priority> extends CurveBag<X> {

    private final QueueLock<X> toPut;

    public ConcurrentCurveBag(@NotNull PriMerge mergeFunction, @NotNull Map<X, X> map, Random rng, int cap) {
        super(mergeFunction, map, rng, cap);

        this.toPut = new QueueLock<X>(Util.blockingQueue(cap), super::putAsync, mustSort ? (batchSize) -> {
            synchronized (items) {
                super.ensureSorted();
            }
        } : null);
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

package jcog.cache;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 *  Future completes when any of child futures completed. All others are cancelled
 *  upon completion.
 */
public class AnyFuture<V> extends AbstractFuture<V> {
    private final Executor exe;
    private List<ListenableFuture<V>> futures = new ArrayList<>();

    public AnyFuture(Executor exe) {
        this.exe = exe;
    }

    /**
     * Add a Future delegate
     */
    public synchronized void add(final ListenableFuture<V> f) {
        if (isCancelled() || isDone()) return;

        f.addListener(() ->
                futureCompleted(f),  exe);
        futures.add(f);
    }

    private synchronized void futureCompleted(ListenableFuture<V> f) {
        if (isCancelled() || isDone()) return;
        if (f.isCancelled()) return;

        try {
            cancelOthers(f);
            
            V v = f.get();
            postProcess(v);
            set(v);
        } catch (Exception e) {
            setException(e);
        }
    }

    /**
     * Subclasses my override to perform some task on the calculated
     * value before returning it via Future
     */
    protected void postProcess(V v) {}

    private void cancelOthers(ListenableFuture besidesThis) {
        for (ListenableFuture future : futures) {
            if (future != besidesThis) {
                try {
                    future.cancel(true);
                } catch (Exception e) {
                }
            }
        }
    }

    @Override
    protected void interruptTask() {
        cancelOthers(null);
    }
}

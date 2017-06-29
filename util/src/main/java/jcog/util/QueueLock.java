package jcog.util;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * ex:
 *      toAdd = new QueueLock<T>(writeLock, 4, this::add);
 */
public class QueueLock<X> implements Consumer<X> {

    //public final Lock lock;
    public final AtomicBoolean busy;
    public final BlockingQueue<X> queue;
    private final Consumer<X> proc;

    public QueueLock(Consumer<X> procedure) {
        this(Integer.MAX_VALUE, procedure);
    }

    public QueueLock(int capacity, Consumer<X> procedure) {
        queue = //new DisruptorBlockingQueue<X>(capacity);
                new LinkedBlockingQueue<>(capacity);

        //this.lock = lock;
        this.busy = new AtomicBoolean(false);
        this.proc = procedure;
    }

    @Override public void accept(@NotNull X x) {
        queue.add(x);
        boolean responsible = busy.compareAndSet(false, true);
        if (responsible) {
            try {
                X next;
                while ((next = queue.poll()) != null) {
                    proc.accept(next);
                }
            } finally {
                busy.set(false);
            }
        }
    }
}

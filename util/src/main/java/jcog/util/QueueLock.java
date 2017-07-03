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

    final static int concurrency = Runtime.getRuntime().availableProcessors();

    public QueueLock(Consumer<X> procedure) {
        this(concurrency*2, procedure);
    }

    public QueueLock(int capacity, Consumer<X> procedure) {
        queue = //new DisruptorBlockingQueue<X>(capacity);
                new LinkedBlockingQueue();
                //new ArrayBlockingQueue<X>(capacity);

        //this.lock = lock;
        this.busy = new AtomicBoolean(false);
        this.proc = procedure;
    }

    @Override public void accept(@NotNull X x) {
//        try {
            if (!queue.offer(x))
                return;
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }

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

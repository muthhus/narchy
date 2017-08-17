package jcog.util;

import org.jetbrains.annotations.NotNull;

import java.util.Queue;
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
    public final Queue<X> queue;
    private final Consumer<X> proc;

    //final static int concurrency = Runtime.getRuntime().availableProcessors();

    public QueueLock(Consumer<X> procedure) {
        this(new LinkedBlockingQueue(), procedure);
    }

    /*
    //new EvictingQueue<>(capacity);
    //new DisruptorBlockingQueue<>(capacity);
    //new ArrayBlockingQueue<X>(capacity);
    //new LinkedBlockingQueue
     */
    public QueueLock(Queue<X> queue, Consumer<X> procedure) {
        this.queue = queue;

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
            int count = 0;
            try {
                X next;
                while ((next = queue.poll()) != null) {
                    proc.accept(next);
                    count++;
                }
            } finally {
                busy.set(false);
                batchFinished(count);
            }
        }
    }

    protected void batchFinished(int batchSize) {

    }
}

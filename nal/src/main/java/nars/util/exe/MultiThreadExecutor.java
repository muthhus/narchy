package nars.util.exe;

import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import jcog.AffinityExecutor;
import jcog.event.On;
import jcog.pri.Priority;
import nars.NAR;
import nars.Task;
import nars.concept.SensorConcept;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/**
 * Created by me on 8/16/16.
 */
public class MultiThreadExecutor extends Executioner {

    private static final Logger logger = LoggerFactory.getLogger(MultiThreadExecutor.class);


    private final DisruptorBlockingQueue passive;
    private final AffinityExecutor active;
    private final int concurrency;


    //private CPUThrottle throttle;

    //
//    private final DisruptorBlockingQueue queue;
//    private final AffinityExecutor exe;
//    private final Runnable worker;
//    private Consumer<NAR> cycle;
//    private int cap;
    private On onReset;
    //private Loop loop;

    final int maxPending = 1024;
    final AtomicInteger numPending = new AtomicInteger(); //because skiplist size() is slow
    final LongAdder added = new LongAdder(), forgot = new LongAdder(), executed = new LongAdder();
    final ConcurrentSkipListSet<Task> pending = new ConcurrentSkipListSet(Priority.COMPARATOR);

    @Override
    public void run(@NotNull Task t) {
        if (t.isCommand()) {
            run(() -> t.run(nar));
        } else {
            if (pending.add(t)) {
                int p = numPending.incrementAndGet();
                if (p > maxPending) {

                    do {
                        if (numPending.compareAndSet(p, p - 1)) {
                            Task forgotten = pending.pollFirst(); //remove lowest
                            if (forgotten != null) {
                                forgot.increment();
                                forget(forgotten);
                            }
                        } else {
                            break;
                        }
                    } while ((p = numPending.get()) > maxPending);
                }
                added.increment();
            }
        }
    }

    protected void forget(Task task) {
        if (!(task instanceof SensorConcept))
            task.delete();

    }

    @Override
    public void run(Runnable cmd) {
        //exe.execute(cmd);
        execute(cmd);
    }

    @Override
    public void run(@NotNull Consumer<NAR> r) {
        execute(() -> r.accept(nar));
    }

    //    @Override
//    public void handleEventException(Throwable ex, long sequence, TaskEvent event) {
//
//    }
//
//    @Override
//    public void handleOnStartException(Throwable ex) {
//        logger.error("start: {}", ex);
//    }
//
//    @Override
//    public void handleOnShutdownException(Throwable ex) {
//        logger.error("stop: {}", ex);
//    }


//    public boolean tryRun(@NotNull Consumer<NAR> r) {
//        return queue.offer(r);
//    }

//    @Override
//    public final void run(@NotNull Runnable r) {
//        if (!queue.offer(r)) {
//            //logger.error("unable to queue {}", r);
//            r.run(); //execute in callee's thread
//        }
//    }
//
//    @Override
//    public final void run(@NotNull Consumer<NAR> r) {
////        try {
////            queue.put(r);
////        } catch (InterruptedException e) {
////            logger.error("{}", r);
////        }
//        if (!queue.offer(r)) {
//            //logger.error("unable to queue {}", r);
//            r.accept(nar); //execute in callee's thread
//        }
//    }

    public MultiThreadExecutor(int activeThreads, int passiveThreads) {

        this.concurrency = activeThreads + passiveThreads;

        this.passive = new DisruptorBlockingQueue(512);

        exe = //new ForkJoinPool(threads, defaultForkJoinWorkerThreadFactory, null, true);
                //Executors.newFixedThreadPool(threads);
                new ThreadPoolExecutor(passiveThreads, passiveThreads,
                        1, TimeUnit.MINUTES,
                        passive
                );
        exe.prestartAllCoreThreads();

        this.active = new AffinityExecutor("exe");
        active.work(() -> { //worker thread

            while (true) {
                try {

                    Task next = pending.pollLast(); //highest, because the comparator is descending
                    if (next == null) {
                        Thread.yield();
                        continue;
                    }
                    numPending.decrementAndGet();

                    next.run(nar);

                    executed.increment();

                } catch (Throwable e) {
                    logger.error("{}", e);
                    continue;
                }
            }
        }, activeThreads);


//        this.queue = new DisruptorBlockingQueue<>(ringSize);


    }


    @Override
    public void execute(Runnable pooled) {
        //exe.execute(pooled);

        if (!passive.offer(pooled))
            pooled.run();

    }

    @Override
    public void start(@NotNull NAR nar) {
        synchronized (exe) {
//            cap = queue.capacity();
//
//            exe.work(worker, threads);

            onReset = nar.eventReset.on((n) -> {
                if (isRunning()) {
                    synchronized (exe) {
                        stop();
                        start(n);
                    }
                }
            });

            super.start(nar);
//            loop = new Loop(50) {
//
//                @Override
//                public boolean next() {
//                    //sync
//                    //queue.
//                    nar.cycle();
//
//                    return true;
//                }
//            };

        }

    }

    final ThreadPoolExecutor exe;


    @Override
    public void stop() {

        synchronized (exe) {

            if (onReset != null) {
                onReset.off();
                onReset = null;
            }

            //exe.shutdown();

            super.stop();
        }


    }

    @Override
    public final float load() {
        //ForkJoinPool.commonPool().getActiveThreadCount()
        //return 0f;
        return 1f - ((float) passive.remainingCapacity()) / passive.capacity();

//        int remaining = (int) ring.remainingCapacity();
//        if (remaining < safetyLimit)
//            return 1f;
//
//        return Math.max(0f, 1f - ((float)ring.remainingCapacity() / (cap - safetyLimit)));
    }

    @Override
    public int concurrency() {
        return concurrency;
    }

    @Override
    public void cycle(@NotNull NAR nar) {


        System.out.println(
                added.sumThenReset() + " added, " +
                        executed.sum() + " executed, " +
                        forgot.sum() + " forgot, " +
                        numPending.longValue() + " pending");


        //throw new UnsupportedOperationException("Real-time mode only");


//        if (load() > 0.5f) {
//            Util.sleep(10);
//            return;
//        }


//        try {
//            exe.awaitTermination(1000, TimeUnit.MILLISECONDS);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        while (queue.remainingCapacity()<queue.capacity()/2) {
//            Util.sleep(100);
//        }

        Consumer[] vv = nar.eventCycleStart.getCachedNullTerminatedArray();
        if (vv != null) {
            for (int i = 0; ; ) {
                Consumer c = vv[i++];
                if (c == null) break; //null terminator hit

                //nar.exe.run(c); //use nar.exe because this executor may be wrapped in something else that we want this to pass through, ie. instrumentor
                run(c);
            }
        }
    }


//
//
//    private final class TaskEventWorkHandler implements WorkHandler<TaskEvent> {
//
//        @Override
//        public final void onEvent(@NotNull TaskEvent te) {
//
//            Object val = te.val;
//            te.val = null;
//
//            //try {
//                if (val instanceof Runnable) {
//                    ((Runnable) val).run();
//                } else {
//                    ((Consumer) val).accept(nar);
//                }
////            } catch (Throwable t) {
////                if (Param.DEBUG)
////                    logger.error("{} {} ", val, t);
////            }
//        }
//
//
//    }


}

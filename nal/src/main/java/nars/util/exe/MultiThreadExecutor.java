package nars.util.exe;

import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import jcog.event.On;
import nars.NAR;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by me on 8/16/16.
 */
public class MultiThreadExecutor extends Executioner {

    private static final Logger logger = LoggerFactory.getLogger(MultiThreadExecutor.class);


    private final int threads;
    private final DisruptorBlockingQueue queue;


    //private CPUThrottle throttle;

    //
//    private final DisruptorBlockingQueue queue;
//    private final AffinityExecutor exe;
//    private final Runnable worker;
//    private Consumer<NAR> cycle;
//    private int cap;
    private On onReset;
    //private Loop loop;

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

    public MultiThreadExecutor(int threads) {

        this.queue = new DisruptorBlockingQueue(64);
        this.threads = threads;

        exe = //new ForkJoinPool(threads, defaultForkJoinWorkerThreadFactory, null, true);
                //Executors.newFixedThreadPool(threads);
                new ThreadPoolExecutor(threads, threads,
                        1, TimeUnit.MINUTES,
                        queue
                );
        exe.prestartAllCoreThreads();


//        this.queue = new DisruptorBlockingQueue<>(ringSize);
//        this.exe = new AffinityExecutor("exe");


//        worker = () -> { //worker thread
//
//            while (true) {
//                try {
//                    Object r = queue.take();
//                    if (r instanceof Consumer)
//                        ((Consumer) r).accept(nar);
//                    else
//                        ((Runnable) r).run();
//                } catch (Throwable e) {
//                    logger.error("{}", e);
//                    continue;
//                }
//            }
//        };


    }


    @Override
    public void execute(Runnable pooled) {
        //exe.execute(pooled);

        if (!queue.offer(pooled))
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
        return 1f - ((float) queue.remainingCapacity()) / queue.capacity();

//        int remaining = (int) ring.remainingCapacity();
//        if (remaining < safetyLimit)
//            return 1f;
//
//        return Math.max(0f, 1f - ((float)ring.remainingCapacity() / (cap - safetyLimit)));
    }

    @Override
    public int concurrency() {
        return threads;
    }

    @Override
    public void cycle(@NotNull NAR nar) {
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

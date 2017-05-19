package nars.util.exe;

import jcog.AffinityExecutor;
import jcog.Texts;
import jcog.Util;
import jcog.bag.Bag;
import jcog.bag.impl.ArrayBag;
import jcog.bag.impl.hijack.DefaultHijackBag;
import jcog.bag.impl.hijack.PriorityHijackBag;
import jcog.event.On;
import jcog.pri.PForget;
import jcog.pri.PLink;
import jcog.pri.PriMerge;
import jcog.pri.RawPLink;
import nars.NAR;
import nars.Param;
import nars.task.ITask;
import nars.task.NALTask;
import org.apache.commons.collections4.bag.HashBag;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

import static nars.Op.COMMAND;

/**
 * Created by me on 8/16/16.
 */
public class MultiThreadExecutor extends Executioner {

    private static final Logger logger = LoggerFactory.getLogger(MultiThreadExecutor.class);


    //private final ArrayBlockingQueue<Runnable> passive;
    private final ForkJoinPool passive;

    final int maxActive = 4096;
    final PriorityHijackBag<ITask,ITask> active = new PriorityHijackBag<ITask, ITask>(maxActive, 3) {
        @Override
        protected ITask merge(@NotNull ITask existing, @NotNull ITask incoming, float scale) {
//            existing.priMax(incoming.priSafe(0)*scale);
//            return existing;
            return super.merge(existing, incoming, scale);
        }

        @Override
        protected Consumer<ITask> forget(float rate) {
            return new PForget(rate);
        }

        @NotNull
        @Override
        public ITask key(ITask value) {
            return value;
        }
    };
    //final ArrayBag<ITask> active = new ArrayBag(maxActive, PriMerge.avg, new ConcurrentHashMap<>(maxActive));
    //final ConcurrentSkipListSet<ITask> active = new ConcurrentSkipListSet(Priority.COMPARATOR);

    private final AffinityExecutor activeExec;

    private final int concurrency;


    private On onReset;

    //    final AtomicInteger numActive = new AtomicInteger(); //because skiplist size() is slow
    final LongAdder input = new LongAdder(), forgot = new LongAdder(), executed = new LongAdder();


    @Override
    public void run(@NotNull ITask t) {
        float p = t.pri();
        if (p!=p) //deleted
            return;

        if (t.punc() == COMMAND) {
            runLater(() -> t.run(nar));
        } else {
            input.increment();
            if (active.put(t, 1f, null) != null) {
                forgot.increment();

//                int p = numActive.incrementAndGet();
//                if (p > maxActive) {
//
//                    do {
//                        if (numActive.compareAndSet(p, p - 1)) {
//                            PLink<ITask> forgotten = active.remove(false); // pollFirst(); //remove lowest
//                            if (forgotten != null) {
//                                forgot.increment();
//                                forget(forgotten);
//                            }
//                        } else {
//                            break;
//                        }
//                    } while ((p = numActive.get()) > maxActive);
//                }
            }
        }
    }

    protected void forget(PLink<ITask> task) {

    }


    @Override
    public void runLater(Runnable cmd) {
        passive.execute(cmd);
    }

    @Override
    public void run(@NotNull Consumer<NAR> r) {
        this.passive.execute(new RunNAR(r));
    }

    final class RunNAR implements Runnable {

        private final Consumer<NAR> run;

        public RunNAR(@NotNull Consumer<NAR> r) {
            this.run = r;
        }

        @Override
        public void run() {
            run.accept(nar);
        }
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

        this.passive =
                new ForkJoinPool(passiveThreads, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
//                new ThreadPoolExecutor(passiveThreads, passiveThreads,
//                        1, TimeUnit.MINUTES, passive);
        //passiveExec.prestartAllCoreThreads();

        this.activeExec = new AffinityExecutor("exe");
        activeExec.work(() -> { //worker thread

            int pauses = 0;

            while (true) {
                try {

                    active.sample((next)-> {
                        executed.increment();
                        next.run(nar);
                        return Bag.BagCursorAction.Next;
                    }, true);

                    if (active.isEmpty()) {
                        pause(pauses++);
                        continue;
                    } else {
                        pauses = 0;
                    }

                } catch (Throwable e) {
                    logger.error("{}", e);
                }
            }
        }, activeThreads);

//        this.queue = new DisruptorBlockingQueue<>(ringSize);
    }


    /**
     * adaptive spinlock behavior
     */
    protected void pause(int previousContiguousPauses) {
        if (previousContiguousPauses < 2) {
            //nothing
        } else if (previousContiguousPauses < 4) {
            Thread.yield();
        } else if (previousContiguousPauses < 10) {
            Util.sleep(0);
        } else if (previousContiguousPauses < 20) {
            Util.sleep(1);
        } else {
            Util.sleep(100);
        }
    }


    @Override
    public void start(@NotNull NAR nar) {
        synchronized (nar) {
//            cap = queue.capacity();
//
//            exe.work(worker, threads);

            onReset = nar.eventReset.on((n) -> {
                if (isRunning()) {
                    synchronized (nar) {
                        stop();
                        start(n);
                    }
                }
            });

            super.start(nar);
        }

    }

    @Override
    public void stop() {

        synchronized (nar) {

            if (onReset != null) {
                onReset.off();
                onReset = null;
            }

            active.clear();
            activeExec.stop();
            passive.shutdownNow();

            super.stop();
        }

    }

    @Override
    public final float load() {
        //ForkJoinPool.commonPool().getActiveThreadCount()
        return ((float)active.size())/maxActive;
        //return 1f - ((float) passive.remainingCapacity()) / passive.capacity();

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


//        int waitCycles = 0;
//        while (!passive.isQuiescent()) {
//            pause(waitCycles++);
//        }

        active.commit();


        Consumer[] vv = nar.eventCycleStart.getCachedNullTerminatedArray();
        if (vv != null) {
            for (int i = 0; ; ) {
                Consumer c = vv[i++];
                if (c == null) break; //null terminator hit

                //nar.exe.run(c); //use nar.exe because this executor may be wrapped in something else that we want this to pass through, ie. instrumentor
                run(c);
            }
        }

        if (Param.DEBUG) {
            System.out.println(
                    Texts.n4(load()) + " load, " +
                            input.sumThenReset() + " input, " +
                            executed.sumThenReset() + " executed, " +
                            forgot.sumThenReset() + " forgot, " +
                            active.size() + " active, "

                    //+ passive
            );
            System.out.println(activeProfile());
        }
    }

    private HashBag<String> activeProfile() {
        HashBag<String> h = new HashBag();
        active.forEachKey(x -> {
            String key;
            if (x instanceof NALTask) {
                key = Character.toString((char) ((NALTask) x).punc);
            } else {
                key = x.getClass().getSimpleName();
            }
            h.add(key);
        });
        return h;
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

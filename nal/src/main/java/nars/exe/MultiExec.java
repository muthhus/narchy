package nars.exe;

import jcog.Loop;
import jcog.Util;
import jcog.event.On;
import nars.NAR;
import nars.task.ITask;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntFunction;
import java.util.stream.Stream;

/**
 * multithreaded execution system
 */
public class MultiExec extends Exec {

    private static final long LAG_REPORT_THRESH_ms = 0;

    private final Passive passive;
    private final Worker[] active;

    //private final ForkJoinPool passive;

    /**
     * https://github.com/conversant/disruptor/wiki
     */
    static class Passive implements Executor {

        public static final Logger logger = LoggerFactory.getLogger(Passive.class);

        private final BlockingQueue<Runnable> q;
        private final ExecutorService calcThreads;

        public Passive(int nThreads, int queueSize) {BlockingQueue<Runnable> q1;
            q = Util.blockingQueue(queueSize);

            calcThreads = Executors.newFixedThreadPool(nThreads);
            for (int i = 0; i < nThreads; i++) {
                calcThreads.execute(() -> {
                    int idle = 0;
                    while (true) {
                        //try {
                            Runnable r = q.poll(); //100L, TimeUnit.MILLISECONDS);
                            if (r != null) {
                                idle = 0;
                                work(r);
                            } else {
                                Util.pauseNext(idle++);
                            }
//                        } catch (InterruptedException e) {
//                            Thread.yield();
//                        }
                    }
                });
            }
        }

        @Override
        public void execute(@NotNull Runnable command) {
            try {
                q.put(command);
            } catch (InterruptedException e) {
                logger.error("{} {}", command, e);
            }
        }

        static void work(Runnable r) {
            try {
                r.run();
            } catch (Throwable t) {
                logger.error("{} {}", r, t);
            }
        }
    }

    //private final Worker[] workers;
    private final int num;
    private On onCycle;


    public MultiExec(IntFunction<Worker> active, int numWorkers, int passive) {
        this(
                Util.map(0, numWorkers, active, Worker[]::new),
                passive
                //new ForkJoinPool(passive, defaultForkJoinWorkerThreadFactory,
                       // null, true /* async */)
        );
    }

    public MultiExec(Worker[] active, int passiveThreads) {
        this.num = active.length;
        assert (num > 0);
        this.active = active;
        this.passive = new Passive(passiveThreads, 512);
        //this.working = Executors.newFixedThreadPool(num);

        for (Worker s : active) {
            s.passive = passive;
        }
    }

    @Override
    public synchronized void start(NAR nar) {
        super.start(nar);

        for (Worker w : active) {
            w.start(nar, 0);
        }

        onCycle = nar.onCycle(this::cycle);
    }

    @Override
    public void execute(Runnable r) {
        passive.execute(r);
    }


    @Override
    public void add(@NotNull ITask x) {
        int sub = worker(x);
        active[sub].add(x);
    }

    public int worker(@NotNull ITask x) {
        return Math.abs(Util.hashWangJenkins(x.hashCode())) % num;
    }


//        public void apply(CLink<? extends ITask> x) {
//            if (x!=null && !x.isDeleted()) {
//                x.priMult(((MixContRL) (((NARS) nar).in)).gain(x));
//            }
//        }

    @Override
    public synchronized void stop() {

        for (Worker w : active)
            w.stop();

        super.stop();

//        onCycle.off();
//        onCycle = null;
//        lastCycle = null;
    }


    public void cycle() {
        Runnable r;
        while ((r = passive.q.poll()) != null) {
            Passive.work(r);
        }
//        while (!passive.awaitQuiescence(10, TimeUnit.MILLISECONDS)) {
//            Thread.yield();
//        }
    }

//            int waitCycles = 0;
//            while (!passive.isQuiescent()) {
//                Util.pauseNext(waitCycles++);
//            }
//
//        if (!busy.compareAndSet(false, true))
//            return; //already in the cycle
//
//
//        try {
//
//            if (!passive.isQuiescent()) {
//                long start = System.currentTimeMillis();
//                int pauseCount = 0;
//
//                do {
//                    Util.pauseNext(pauseCount++);
//                } while (!passive.isQuiescent());
//
//                long lagMS = System.currentTimeMillis() - start;
//                if (lagMS > LAG_REPORT_THRESH_ms)
//                    NAR.logger.info("cycle lag {} ms", lagMS);
//            }
//
//        } finally {
//            busy.set(false);
//        }
//    }

//    /**
//     * dont call directly
//     */
//    void run() {
//        nar.eventCycle.emitAsync(nar, passive); //TODO make a variation of this for ForkJoin specifically
//    }

    @Override
    public int concurrency() {
        return 1 + num; //TODO calculate based on # of sub-NAR's but definitely is concurrent so we add 1 here in case passive=1
    }

    @Override
    public boolean concurrent() {
        return true;
    }

    @Override
    public Stream<ITask> stream() {
        return Stream.of(active).flatMap(Worker::stream);
    }

    public static class Worker extends Exec {

        private final Exec model;
        private Executor passive;
        private Loop loop;

        public Worker(Exec delegate) {
            super();
            this.model = delegate;
        }

        Loop start(NAR nar, int periodMS) {
            this.start(nar);
            model.start(nar);

//            new CPUThrottle()
            return this.loop = new Loop(periodMS) {
                @Override
                public boolean next() {
                    //System.out.println(Thread.currentThread() + " " + model);
                    ((Runnable) model).run(); //HACK
                    return true;
                }
            };
        }


        //        @Override
//        protected void actuallyRun(CLink<? extends ITask> x) {
//
//            super.actuallyRun(x);
//
//            ((RootExecutioner) exe).apply(x); //apply gain after running
//
//        }
        @Override
        public void execute(Runnable r) {
            passive.execute(r);
        }

        @Override
        public synchronized void stop() {
            if (loop != null) {
                loop.stop();
                model.stop();
                loop = null;
            }
        }

        @Override
        public Stream<ITask> stream() {
            return model.stream();
        }

        @Override
        public int concurrency() {
            return model.concurrency();
        }


        @Override
        public void add(@NotNull ITask input) {
            model.add(input);
        }
    }

}

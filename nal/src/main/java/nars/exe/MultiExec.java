package nars.exe;

import jcog.Loop;
import jcog.Util;
import jcog.event.On;
import nars.NAR;
import nars.task.ITask;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import static java.util.concurrent.ForkJoinPool.defaultForkJoinWorkerThreadFactory;

/**
 * multithreaded execution system
 */
public class MultiExec extends Exec {

    private static final long LAG_REPORT_THRESH_ms = 0;

    //private ForkJoinTask lastCycle;
    private final ForkJoinPool passive;
    private final Worker[] workers;
    private final int num;
    //private On onCycle;


    public MultiExec(IntFunction<Worker> workers, int numWorkers, int passive) {
        this(
                Util.map(0, numWorkers, workers, Worker[]::new),
                new ForkJoinPool(passive, defaultForkJoinWorkerThreadFactory,
                        null, true /* async */)
        );
    }

    public MultiExec(Worker[] workers, ForkJoinPool passive) {
        this.num = workers.length;
        assert (num > 0);
        this.workers = workers;
        this.passive = passive;
        //this.working = Executors.newFixedThreadPool(num);

        for (Worker s : workers) {
            s.passive = passive;
        }
    }

    @Override
    public synchronized void start(NAR nar) {
        super.start(nar);

        for (Worker w : workers) {
            w.start(nar, 0);
        }

        //onCycle = nar.onCycle(this::cycle);
    }

    @Override
    public void execute(Runnable r) {
        passive.execute(r);
    }


    @Override
    public void add(@NotNull ITask x) {
        int sub = worker(x);
        workers[sub].add(x);
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

        for (Worker w : workers)
            w.stop();

        super.stop();

//        onCycle.off();
//        onCycle = null;
//        lastCycle = null;
    }

    final AtomicBoolean busy = new AtomicBoolean(false);

//    public void cycle() {
//
//
////            int waitCycles = 0;
////            while (!passive.isQuiescent()) {
////                Util.pauseNext(waitCycles++);
////            }
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
        return Stream.of(workers).flatMap(Worker::stream);
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

            return this.loop = new Loop(periodMS) {
                @Override
                public boolean next() {
                    //System.out.println(Thread.currentThread() + " " + model);
                    ((Runnable)model).run(); //HACK
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

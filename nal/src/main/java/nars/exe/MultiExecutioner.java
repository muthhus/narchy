package nars.exe;

import jcog.Loop;
import jcog.Util;
import nars.NAR;
import nars.task.ITask;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import static java.util.concurrent.ForkJoinPool.defaultForkJoinWorkerThreadFactory;

/**
 * multithreaded execution system
 */
public class MultiExecutioner extends Executioner {

    public ForkJoinTask lastCycle;
    private final ForkJoinPool passive;
    private final Worker[] workers;
    private final int num;

    /**
     * foreground: the independent, preallocated, high frequency worker threads ; a fixed threadpool
     */
    private final ExecutorService working;


    public MultiExecutioner(IntFunction<Worker> workers, int numWorkers, int passive) {
        this(
                Util.map(0, numWorkers, workers, Worker[]::new),
                new ForkJoinPool(passive, defaultForkJoinWorkerThreadFactory,
                        null, true /* async */)
        );
    }

    public MultiExecutioner(Worker[] workers, ForkJoinPool passive) {
        this.num = workers.length;
        assert (num > 0);
        this.workers = workers;
        this.passive = passive;
        this.working = Executors.newFixedThreadPool(num);

        for (Worker s : workers) {
            s.passive = passive;
        }
    }

    @Override
    public void start(NAR nar) {
        super.start(nar);

        for (Worker w : workers) {
            w.start(nar);
            working.execute(w.start(0 ));
        }
    }

    @Override
    public void runLater(Runnable cmd) {
        passive.execute(cmd);
    }

    @Override
    public void runLaterAndWait(Runnable cmd) {
        passive.submit(cmd).join();
    }

    @Override
    public void run(@NotNull ITask x) {
        int sub = worker(x);
        workers[sub].run(x);
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
    public void stop() {

        for (Worker w : workers) w.stop();
        this.working.shutdownNow();

        super.stop();

        lastCycle = null;
    }

    final AtomicBoolean busy = new AtomicBoolean(false);

    @Override
    public void cycle() {


//            int waitCycles = 0;
//            while (!passive.isQuiescent()) {
//                Util.pauseNext(waitCycles++);
//            }

        if (!busy.compareAndSet(false, true))
            return; //already in the cycle


        try {

            if (lastCycle != null) {
                //System.out.println(lastCycle + " " + lastCycle.isDone());
                if (!lastCycle.isDone()) {
                    long start = System.currentTimeMillis();
                    lastCycle.join(); //wait for lastCycle's to finish
                    NAR.logger.info("cycle lag {}", (System.currentTimeMillis() - start) + "ms");
                }

                lastCycle.reinitialize();
                passive.execute(lastCycle);


            } else {
                lastCycle = passive.submit((Runnable) this::run);
            }
        } finally {
            busy.set(false);
        }
    }

    /**
     * dont call directly
     */
    void run() {
        nar.eventCycleStart.emitAsync(nar, passive); //TODO make a variation of this for ForkJoin specifically
    }

    @Override
    public int concurrency() {
        return 1 + num; //TODO calculate based on # of sub-NAR's but definitely is concurrent so we add 1 here in case passive=1
    }

    @Override
    public boolean concurrent() {
        return true;
    }

    @Override
    public void forEach(Consumer<ITask> each) {
        for (Worker w : workers) {
            w.forEach(each);
        }
    }


    public static class Worker extends Executioner {

        private final Executioner model;
        private Executor passive;
        private Loop loop;

        public Worker(Executioner delegate) {
            super();
            this.model = delegate;
        }

        Loop start(int periodMS) {
            model.start(nar);

            return this.loop = new Loop(periodMS) {
                @Override
                public boolean next() {
                    model.cycle();
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
        public void stop() {
            loop.stop();
            model.stop();
            loop = null;
        }

        @Override
        public void cycle() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int concurrency() {
            return model.concurrency();
        }

        @Override
        public void forEach(Consumer<ITask> each) {
            model.forEach(each);
        }

        @Override
        public void runLater(@NotNull Runnable r) {
            passive.execute(r); //use the common threadpool
        }

        @Override
        public void runLaterAndWait(Runnable cmd) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void run(@NotNull ITask input) {
            model.run(input);
        }
    }

}

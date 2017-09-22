package nars.exe;

import jcog.exe.Loop;
import jcog.Util;
import jcog.event.ListTopic;
import jcog.event.On;
import jcog.event.Topic;
import jcog.sort.TopN;
import nars.NAR;
import nars.control.Activate;
import nars.control.Cause;
import nars.control.Causable;
import nars.task.ITask;
import nars.task.RunTask;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * multithreaded execution system
 */
public class MultiExec extends Exec {


    /**
     * https://github.com/conversant/disruptor/wiki
     */

    public static final Logger logger = LoggerFactory.getLogger(MultiExec.class);

    private final BlockingQueue<ITask> q;
    private ExecutorService exe;

    final Topic<Activate> onActivate = new ListTopic();

    //private final Worker[] workers;
    private final int num;
    private On onCycle;

    @Override
    public void execute(@NotNull Runnable runnable) {
        add(new RunTask(runnable));
    }


    public Stream<ITask> stream() {
        return q.stream();
    }


    public MultiExec(int threads, int qSize) {
        num = threads;
        q = Util.blockingQueue(qSize);
    }

    @Override
    public synchronized void start(NAR nar) {
        super.start(nar);
        exe = Executors.newFixedThreadPool(num);
        for (int i = 0; i < num; i++) {
            exe.execute(() -> {
                int idle = 0;
                while (true) {
                    ITask r = q.poll();
                    if (r != null) {
                        execute(r);
                        idle = 0;
                    } else {
                        Util.pauseNext(++idle);
                    }
                }
            });
        }
    }

    public void execute(ITask r) {
        try {

            r.run(nar);

            if (r instanceof Activate)
                onActivate.emit((Activate)r);

        } catch (Throwable t) {
            logger.error("{} {}", r, t);
        }
    }

    @Override
    public synchronized void stop() {
        exe.shutdownNow();
        if (!exe.isShutdown()) {
            logger.info("awaiting termination..");
            try {
                exe.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.error("awaiting termination: {}", e);
            }
        }
        super.stop();
    }

    @Override
    public void add(/*@NotNull*/ ITask t) {
        if (!q.offer(t)) {

            //1. reduce system power parameter
            //TODO

            Util.pauseNext(0);

            //2. attempt to evict any weaker tasks consuming space
            int sample = Math.max(4, q.size() / (num+1) / 4);

            int survive = Math.round(sample * 0.5f);

            TopN<ITask> tmpEvict = new TopN<>(new ITask[survive], (x) -> x.priElseNeg1() ) {
                @Override
                protected void reject(ITask iTask) {
                    logger.info("ignored: {}", iTask);
                }
            };
            tmpEvict.add(t);

            q.drainTo(tmpEvict, sample);
//            if (tmpEvict.isEmpty()) {
//                logger.warn("ignored {}", t);
//                return;
//            }

            //reinsert the temporarily evicted
            tmpEvict.forEach(q::offer);
        }
    }

    public void add(FocusExec reasoner, Predicate<Activate> filter) {
        execute((Runnable)()->new Reasoner(reasoner, filter));
    }

    class Reasoner extends Causable /* TODO use Throttled */ implements Consumer<Activate> {
        private final FocusExec sub;

        /** customizable filter, or striping so that every reasoner doesnt get the same inputs */
        private final Predicate<Activate> filter;
        private final Cause out;

        public Reasoner(FocusExec focusExec, Predicate<Activate> filter) {
            super(nar);
            this.sub = focusExec;
            this.filter = filter;
            this.out = nar.newCauseChannel(this);
            sub.cause = this.out.id;
            sub.start(nar);
        }

        @Override
        protected void start(NAR nar) {
            super.start(nar);
            ons.add(onActivate.on(this));
        }

        @Override
        public float value() {
            return out.amp();
        }

        @Override
        protected int next(NAR n, int work) {
            FocusExec f = (FocusExec) this.sub;
            f.subCycles = work;
            f.run();
            return work; //TODO better estimate than this, even to the precision of the TTL spent
        }

        public void stop() {
            sub.stop();
        }

        @Override
        public void accept(Activate a) {
            if (filter.test(a))
                sub.add(a);
        }
    }


    @Override
    public int concurrency() {
        return num; //TODO calculate based on # of sub-NAR's but definitely is concurrent so we add 1 here in case passive=1
    }

    @Override
    public boolean concurrent() {
        return true;
    }


    static class Sub extends Exec {

        private final Exec model;
        private Executor passive;
        private Loop loop;

        public Sub(Exec delegate) {
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

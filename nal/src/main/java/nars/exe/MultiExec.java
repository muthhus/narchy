package nars.exe;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import jcog.Util;
import jcog.exe.AffinityExecutor;
import nars.$;
import nars.Task;
import nars.task.ITask;
import nars.task.NativeTask;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongPredicate;
import java.util.stream.Stream;

abstract public class MultiExec extends UniExec {

    public static final Logger logger = LoggerFactory.getLogger(MultiExec.class);
    protected final Disruptor<ITask[]> disruptor;

    protected Executor exe;

    protected RingBuffer<ITask[]> buffer;
    protected int threads;


    public MultiExec(int concepts, int threads, int qSize) {
        super(concepts);

        this.threads = threads;

        exe = initExe();

        disruptor = new Disruptor(
                () -> new ITask[1],
                qSize,
                exe,
                ProducerType.MULTI,
                waitStrategy()
        );

        initWorkers();

        buffer = disruptor.getRingBuffer();

        disruptor.start();
    }


    abstract protected WaitStrategy waitStrategy();

    protected boolean isWorker(Thread t) {
        return isActiveThreadId.test(t.getId());
    }


    protected abstract Executor initExe();

    abstract protected void initWorkers();

    final WorkHandler<ITask[]> wh = event -> {
                ITask e = event[0];
                event[0] = null;
                execute(e);
            };

    public static class Intense extends MultiExec {

        private AffinityExecutor aexe;

        public Intense(int concepts, int threads, int qSize) {
            super(concepts, threads, qSize);
        }

        @Override
        protected WaitStrategy waitStrategy() {
            return new BusySpinWaitStrategy();
        }

        @Override
        protected Executor initExe() {
            return aexe = new AffinityExecutor() {
                @Override
                protected void add(AffinityExecutor.AffinityThread at) {
                    super.add(at);
                    register(at);
                }
            };
        }

        @Override
        protected void initWorkers() {
            WorkHandler[] w = new WorkHandler[threads];

            for (int i = 0; i < threads; i++)
                w[i] = wh;
            disruptor.handleEventsWithWorkerPool(w);
            aexe.threads.forEach(this::register);
        }
    }

    final List<Thread> activeThreads = $.newArrayList();
    LongSet activeThreadIds = new LongHashSet();
    LongPredicate isActiveThreadId = (x)->false;

    /** to be called in initWorkers() impl for each thread constructed */
    protected synchronized void register(Thread t) {
        activeThreads.add(t);
        activeThreadIds = LongSets.mutable.ofAll(activeThreadIds).with(t.getId()).toImmutable();
        long max = activeThreadIds.max();
        long min = activeThreadIds.min();
        if (max - min == activeThreadIds.size()-1) {
            //contiguous id's, use fast id tester
            isActiveThreadId = (x) -> x >= min && x <= max;
        } else {
            isActiveThreadId = activeThreadIds::contains;
        }
    }

    public static class CoolNQuiet extends MultiExec {

        private ThreadPoolExecutor texe;

        public CoolNQuiet(int concepts, int threads, int qSize) {
            super(concepts, threads, qSize);
        }

        @Override
        protected WaitStrategy waitStrategy() {
            //return new SleepingWaitStrategy();
            return new LiteBlockingWaitStrategy();
        }

        @Override
        protected Executor initExe() {
            //return Executors.newCachedThreadPool();
            //return texe = ((ThreadPoolExecutor) Executors.newFixedThreadPool(threads));


            return texe = new ThreadPoolExecutor(threads, threads,
                     60L, TimeUnit.SECONDS,
                     //new LinkedBlockingQueue<Runnable>(),
                     new ArrayBlockingQueue(8),
                     r -> {
                         Thread t = new Thread(r);
                         t.setDaemon(false);
                         t.setName("worker" + activeThreads.size());
                         register(t);
                         return t;
                     }, new ThreadPoolExecutor.AbortPolicy()
             );
        }

        @Override
        protected void initWorkers() {
            WorkHandler<ITask[]>[] w = new WorkHandler[threads];
            for (int i = 0; i < threads; i++)
                w[i] = wh;
            disruptor.handleEventsWithWorkerPool(w);
        }
    }


    @Override
    public boolean concurrent() {
        return true;
    }

    @Override
    public synchronized void stop() {
        disruptor.halt();
        disruptor.shutdown();
        super.stop();
    }

    static final EventTranslatorOneArg<ITask[], ITask> ein =
            (event, sequence, arg0) ->
                    event[0] = arg0;
//    final EventTranslatorOneArg<ITask[], ITask> einCritical =
//            (event, sequence, y) -> {
//                ITask x = event[0];
//                if (x!=null) {
//                    execute(x);
//                }
//                event[0] = y;
//            };

    @Override
    public float load() {
        float bufferUsed = ((float) buffer.remainingCapacity()) / buffer.getBufferSize();
        return Util.sqr(Util.sqr(Util.sqr(1f - bufferUsed)));
    }

    @Override
    public void execute(Runnable r) {
        add(new NativeTask.RunTask(r));
    }

    @Override
    protected synchronized void clear() {
        super.clear();
        activeThreads.forEach(Thread::interrupt);
        activeThreads.clear();
        activeThreadIds = new LongHashSet();
        isActiveThreadId = (x)->false;
    }

    //    class Sub extends UniExec implements Runnable {
//
//        public final Logger logger = LoggerFactory.getLogger(Sub.class);
//
//        private final SharedCan can;
//
//        public Sub(int capacity, SharedCan deriver) {
//            super(capacity);
//            this.can = deriver;
//            start(MultiExec.this.nar);
//        }
//
//        int premiseRemaining, premiseDone;
//
//        @Override
//        public void add(ITask t) {
//            MultiExec.this.add(t); //to master
//        }
//
//        @Override
//        public void run() {
//
//            BatchActivate ba = BatchActivate.get();
//
//            int conc = MultiExec.this.concurrency();
//            int idle = 0;
//            while (true) {
//                try {
//
//                    int s = work(conc);
//
//                    int p = think(conc);
//
//                    if ((s == 0) && (p== 0)) {
//                        Util.pauseNext(idle++);
//                    } else {
//                        ba.commit(nar);
//                        idle = 0;
//                    }
//
//                } catch (Throwable t) {
//                    logger.error("{} {}", this, t);
//                }
//            }
//        }
//
//        public int think(int conc) {
//            int p;
//            if ((p = premiseRemaining = Math.max(1,can.share(1f / conc))) > 0) {
//                premiseDone = 0;
//
//                int loops = 0;
//
//
//                //spread the sampling over N batches for fairness
//                int batches = 4;
//                int batchSize = Math.max(8, active.size() / batches);
//
//                long start = System.nanoTime();
//
//                while (premiseRemaining > 0) {
//
//                    int premiseDoneAtStart = premiseDone;
//
//                    workRemaining = batchSize;
//                    active.commit().sample(super::exeSample);
//
//                    loops++;
//
//                    if (premiseDone == premiseDoneAtStart)
//                        break; //encountered no premises in this batch that could have been processed
//                }
//
//                long end = System.nanoTime();
//
//                //System.err.println(premiseDone + "/" + work + " in " + loops + " loops\tvalue=" + can.value() + " " + n4((end - start) / 1.0E9) + "sec");
//
//                can.update(premiseDone, (end - start) / 1.0E9);
//            }
//            return p;
//        }
//
//        public int work(int conc) {
//            int s;
//            float qs = q.size();
//            if (qs > 0) {
//                s = (int) Math.ceil(qs / Math.max(1, (conc - 1)));
//                for (int i = 0; i < s; i++) {
//                    ITask k = q.poll();
//                    if (k != null)
//                        execute(k);
//                    else
//                        break;
//                }
//            } else {
//                s = 0;
//            }
//            return s;
//        }
//
//        @Override
//        protected boolean done(ITask x) {
//            if (x instanceof Premise) {
//                premiseDone++;
//                if (--premiseRemaining <= 0)
//                    return true;
//            }
//
//            return super.done(x);
//        }
//
//
//    }


    final Consumer<ITask> immediate = this::execute;

    final Consumer<ITask> deferred = x->{
        if (x instanceof Task)
            execute(x);
        else
            buffer.publishEvent(ein, x);
    };

    @Override
    public void add(Iterator<? extends ITask> input) {
        input.forEachRemaining(add());
    }

    /** the input procedure according to the current thread */
    protected Consumer<ITask> add() {
        return isWorker(Thread.currentThread()) ? immediate : deferred;
    }

    @Override
    public void add(Stream<? extends ITask> input) {
        input.forEach(add());
    }

    @Override
    public void add(ITask t) {
        if ((t instanceof Task) || (isWorker(Thread.currentThread()))) {
            execute(t);
        } else {
            buffer.publishEvent(ein, t);
        }
    }

//
//    protected int which(ITask t) {
//        return Math.abs(t.hashCode() % sub.length);
//    }

    @Override
    public int concurrency() {
        return threads;
    }


//    private class SharedCan extends Can {
//
//        final AtomicInteger workDone = new AtomicInteger(0);
//        final AtomicInteger workRemain = new AtomicInteger(0);
//        final AtomicDouble time = new AtomicDouble(0);
//
//        final AtomicLong valueCachedAt = new AtomicLong(ETERNAL);
//        float valueCached = 0;
//
//        @Override
//        public void commit() {
//            workRemain.set(iterations());
//        }
//
//        public int share(float prop) {
//            return (int) Math.ceil(workRemain.get() * prop);
//        }
//
//        @Override
//        public float value() {
//            long now = nar.time();
//            if (valueCachedAt.getAndSet(now) != now) {
//
//                //HACK
//                float valueSum = 0;
//                for (Cause c : nar.causes) {
//                    if (c instanceof Conclude.RuleCause) {
//                        valueSum += c.value();
//                    }
//                }
//
//                this.valueCached = valueSum;
//
//                int w = workDone.getAndSet(0);
//                double t = time.getAndSet(0);
//
//                this.update(w, valueCached, t);
//            }
//
//            return valueCached;
//        }
//
//        public void update(int work, double timeSec) {
//            this.workDone.addAndGet(work);
//            this.time.addAndGet(timeSec);
//        }
//
//    }
}

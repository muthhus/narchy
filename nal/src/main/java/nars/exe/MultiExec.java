package nars.exe;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import jcog.exe.AffinityExecutor;
import nars.NAR;
import nars.Task;
import nars.task.ITask;
import nars.task.NativeTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

abstract public class MultiExec extends UniExec {

    public static final Logger logger = LoggerFactory.getLogger(MultiExec.class);
    protected final Disruptor<ITask[]> disruptor;

    protected  Executor exe;

    protected RingBuffer<ITask[]> buffer;
    protected int threads;


    public MultiExec(int concepts, int threads, int qSize) {
        super(concepts);

        this.threads = threads;

        exe = initExe();

        disruptor = new Disruptor<ITask[]>(
                () -> new ITask[1],
                qSize,
                exe
        );

        initWorkers();

        buffer = disruptor.getRingBuffer();

        disruptor.start();
    }

    protected abstract Executor initExe();

    abstract protected void initWorkers();

    public static class Intense extends MultiExec {

        public Intense(int concepts, int threads, int qSize) {
            super(concepts, threads, qSize);
        }

        @Override
        protected Executor initExe() {
            return new AffinityExecutor();
        }

        @Override
        protected void initWorkers() {
            WorkHandler[] w = new WorkHandler[threads];
            WorkHandler<ITask[]> wh = event -> {
                ITask e = event[0];
                event[0] = null;
                execute(e);
            };
            for (int i = 0; i < threads; i++)
                w[i] = wh;
            disruptor.handleEventsWithWorkerPool(w);
        }
    }

    public static class CoolNQuiet extends MultiExec {

        public CoolNQuiet(int concepts, int threads, int qSize) {
            super(concepts, threads, qSize);
        }

        @Override
        protected Executor initExe() {
            return Executors.newCachedThreadPool();
        }

        @Override
        protected void initWorkers() {
            disruptor.handleEventsWith((event, seq, endOfBatch)->{
                ITask e = event[0];
                event[0] = null;
                execute(e);
            });
        }
    }

    @Override
    public synchronized void start(NAR nar) {
        super.start(nar);
    }

    @Override
    public synchronized void stop() {
        disruptor.halt();
        disruptor.shutdown();
        super.stop();
    }

    static final EventTranslatorOneArg<ITask[], ITask> ein = (event, sequence, arg0) -> {
        event[0] = arg0;
    };

    public void queue(ITask i) {
        if (!buffer.tryPublishEvent(ein, i)) {
            i.run(nar); //queue full, in-thread
        }
    }

    @Override
    public void execute(Runnable r) {
        add(new NativeTask.RunTask(r));
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

    @Override
    public void add(ITask t) {
        if (t instanceof Task) {

            execute(t);

        } else {
            queue(t);
//            if (!buffer.offer(t)) {
//                execute(t); //in same thread, dangerous could deadlock
//            }
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

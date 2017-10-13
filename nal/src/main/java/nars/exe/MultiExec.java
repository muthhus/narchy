package nars.exe;

import com.conversantmedia.util.concurrent.ConcurrentQueue;
import com.google.common.util.concurrent.AtomicDouble;
import jcog.Util;
import jcog.exe.Can;
import nars.NAR;
import nars.Task;
import nars.control.Activate;
import nars.control.Cause;
import nars.control.Premise;
import nars.derive.Conclude;
import nars.task.ITask;
import nars.task.NativeTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static nars.time.Tense.ETERNAL;

public class MultiExec extends Exec {

    public static final Logger logger = LoggerFactory.getLogger(MultiExec.class);

    private final BlockingQueue<ITask> q;
    private ExecutorService exe;

    final Sub[] sub;
    private final int num;

    final static int SUB_CAPACITY = 512;


    @Deprecated
    final SharedCan deriver = new SharedCan();

    public MultiExec(int threads) {
        this(threads, threads * 32);
    }

    public MultiExec(int threads, int qSize) {
        num = threads;
        sub = new Sub[num];
        q = Util.blockingQueue(qSize);
    }

    @Override
    public synchronized void start(NAR nar) {
        super.start(nar);
        exe = Executors.newFixedThreadPool(num);

        for (int i = 0; i < num; i++) {
            exe.execute(sub[i] = new Sub(SUB_CAPACITY, deriver));
        }

        nar.can.add(deriver);
    }

    class Sub extends UniExec implements Runnable {


        private final SharedCan can;

        public Sub(int capacity, SharedCan deriver) {
            super(capacity);
            this.can = deriver;
            start(MultiExec.this.nar);
        }

        int premiseRemaining, premiseDone;

        @Override
        public void run() {

            Activate.BatchActivate.enable();

            while (true) {
                try {
                    int conc = MultiExec.this.concurrency();

                    //share task work, slightly more per each than if fairly distributed to ensure it is done ASAP
                    final float s = ((ConcurrentQueue) q).size();
                    int maxToPoll = (int) (s / Math.max(1, (conc - 1)));
                    for (int i = 0; i < maxToPoll; i++) {
                        ITask k = q.poll();
                        if (k != null)
                            execute(k);
                        else
                            break;
                    }


                    premiseRemaining = can.share(1f/conc);

                    premiseDone = 0;

                    long start = System.nanoTime();
                    int loops = 0;

                    plan.commit();

                    //spread the sampling over N batches for fairness
                    int batches = 8;
                    int batchSize = plan.capacity()/batches;

                    while (premiseRemaining > 0) {

                        int premiseDoneAtStart = premiseDone;

                        for (int i = 0; i < batches; i++) {
                            workRemaining = batchSize;
                            plan.sample(super::exeSample);
                        }

                        loops++;

                        if (premiseDone == premiseDoneAtStart)
                            break; //bag contained no premises that could have been processed
                    }

                    Activate.BatchActivate.get().commit(nar);

                    long end = System.nanoTime();

                    //System.err.println(premiseDone + "/" + work + " in " + loops + " loops\tvalue=" + can.value() + " " + n4((end - start) / 1.0E9) + "sec");

                    can.update(premiseDone, (end - start) / 1.0E9);

                } catch (Throwable t) {
                    logger.error("{} {}", this, t);
                }
            }
        }

        @Override
        protected boolean done(ITask x) {
            if (x instanceof Premise) {
                premiseDone++;
                if (--premiseRemaining <= 0)
                    return true;
            }

            return super.done(x);
        }

        protected void execute(ITask x) {
            Iterable<? extends ITask> y;
            try {
                y = x.run(nar);
                if (y != null)
                    y.forEach(MultiExec.this::add);
            } catch (Throwable t) {
                logger.error("{} {}", x, t);
                return;
            }

        }
    }

    @Override
    public void add(ITask t) {
        if (t instanceof Task) {

            Iterable<? extends ITask> y = t.run(nar);
            if (y != null)
                y.forEach(q::add);

        } else if (t instanceof NativeTask) {
            q.add(t);
        } else {
            sub[which(t)].plan.putAsync((t));
        }
    }


    protected int which(ITask t) {
        return Math.abs(t.hashCode() % sub.length);
    }

    @Override
    public int concurrency() {
        return sub.length;
    }

    @Override
    public Stream<ITask> stream() {
        return Stream.of(sub).flatMap(UniExec::stream);
    }

    private class SharedCan extends Can {

        final AtomicInteger workDone = new AtomicInteger(0);
        final AtomicInteger workRemain = new AtomicInteger(0);
        final AtomicDouble time = new AtomicDouble(0);

        final AtomicLong valueCachedAt = new AtomicLong(ETERNAL);
        float valueCached = 0;

        @Override
        public void commit() {
            workRemain.set(iterations());
        }

        public int share(float prop) {
            return (int) Math.ceil(workRemain.get() * prop);
        }

        @Override
        public float value() {
            long now = nar.time();
            if (valueCachedAt.getAndSet(now) != now) {

                //HACK
                float valueSum = 0;
                for (Cause c : nar.causes) {
                    if (c instanceof Conclude.RuleCause) {
                        valueSum += c.value();
                    }
                }

                this.valueCached = valueSum;

                int w = workDone.getAndSet(0);
                double t = time.getAndSet(0);

                this.update(w, valueCached, t);
            }

            return valueCached;
        }

        public void update(int work, double time) {
            this.workDone.addAndGet(work);
            this.time.addAndGet(time);
        }

    }
}

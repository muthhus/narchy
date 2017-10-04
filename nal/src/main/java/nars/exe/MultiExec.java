package nars.exe;

import com.conversantmedia.util.concurrent.ConcurrentQueue;
import jcog.Util;
import jcog.event.On;
import jcog.exe.Schedulearn;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static jcog.Texts.n4;
import static nars.time.Tense.ETERNAL;

public class MultiExec extends Exec {

    public static final Logger logger = LoggerFactory.getLogger(MultiExec.class);

    private final BlockingQueue<ITask> q;
    private ExecutorService exe;

    final Sub[] sub;
    private final int num;
    private On onCycle;

    final static int SUB_CAPACITY = 256;
    private On on;

    @Deprecated final Schedulearn.Can deriver = new Schedulearn.Can() {

        final AtomicLong valueCachedAt = new AtomicLong(ETERNAL);
        float valueCached = 0;

        @Override
        public float value() {
            long now = nar.time();
            if (valueCachedAt.getAndSet(now)!=now) {
                //HACK
                float valueSum = 0;
                for (Cause c : nar.causes) {
                    if (c instanceof Conclude.RuleCause) {
                        valueSum += c.value();
                    }
                }

                this.valueCached = (Util.tanhFast(valueSum) + 1f) / 2f;
            }

            return valueCached;
        }
    };

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

        on = nar.onCycle(this::cycle);
    }

    class Sub extends UniExec implements Runnable {


        private final Schedulearn.Can can;

        public Sub(int capacity, Schedulearn.Can deriver) {
            super(capacity);
            this.can = deriver;
            start(MultiExec.this.nar);
        }

        int premiseRemaining, premiseDone;

        @Override
        public void run() {

            while (true) {
                int conc = MultiExec.this.concurrency();

                //share task work
                final float s = ((ConcurrentQueue) q).size();
                int maxToPoll = (int) (s / Math.max(1, (conc -1)) );
                for (int i = 0; i < maxToPoll; i++) {
                    ITask k = q.poll();
                    if (k != null)
                        execute(k);
                    else
                        break;
                }



                int work = premiseRemaining = Math.max(1, (int) Math.ceil(can.iterations.value()));
                premiseDone = 0;

                long start = System.nanoTime();
                int loops = 0;
                while (premiseRemaining > 0) {

                    int premiseDoneAtStart = premiseDone;

                    workRemaining = plan.size();

                    plan.commit()
                            .sample(super::exeSample);

                    loops++;

                    if (premiseDone == premiseDoneAtStart)
                        break; //bag contained no premises that could have been processed
                }

                Activate.BatchActivate.get().commit(nar);

                long end = System.nanoTime();

                //System.err.println(premiseDone + "/" + work + " in " + loops + " loops\tvalue=" + can.value() + " " + n4((end - start) / 1.0E9) + "sec");

                //? multiply the work done by concurrency to be consistent with how it is calculated
                synchronized(can) { //synch to be safe
                    can.update(premiseDone, 0 /* ignored, uses per-cycle cached value */, ((end - start) / 1.0E9));
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
    public synchronized void stop() {
        on.off();
        super.stop();
    }

    public void cycle() {

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
        return Math.abs(t.hashCode()) % sub.length;
    }

    @Override
    public int concurrency() {
        return sub.length;
    }

    @Override
    public Stream<ITask> stream() {
        return Stream.of(sub).flatMap(UniExec::stream);
    }

}

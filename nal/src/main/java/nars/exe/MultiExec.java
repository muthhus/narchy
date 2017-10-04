package nars.exe;

import com.conversantmedia.util.concurrent.ConcurrentQueue;
import jcog.Util;
import jcog.event.On;
import jcog.exe.Schedulearn;
import nars.NAR;
import nars.Task;
import nars.control.Activate;
import nars.control.Cause;
import nars.derive.Conclude;
import nars.task.ITask;
import nars.task.NativeTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static jcog.Texts.n2;
import static jcog.Texts.n4;

public class MultiExec extends Exec {

    public static final Logger logger = LoggerFactory.getLogger(MultiExec.class);

    private final BlockingQueue<ITask> q;
    private ExecutorService exe;

    final Sub[] sub;
    private final int num;
    private On onCycle;

    final static int SUB_CAPACITY = 512;
    private On on;

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
            exe.execute(sub[i] = new Sub(SUB_CAPACITY));
            nar.can.add(sub[i].can);
        }

        on = nar.onCycle(this::cycle);
    }

    class Sub extends UniExec implements Runnable {

        final Schedulearn.Can can = new Schedulearn.Can();

        public Sub(int capacity) {
            super(capacity);
            start(MultiExec.this.nar);
        }

        public void queue(ITask t) {
            plan.putAsync((t));
        }

        @Override
        public void run() {
            while (true) {

                final float s = ((ConcurrentQueue) q).size();
                int maxToPoll = (int) (s / Math.max(1, (MultiExec.this.concurrency()-1)) );
                for (int i = 0; i < maxToPoll; i++) {
                    ITask k = q.poll();
                    if (k != null)
                        execute(k);
                    else
                        break;
                }

                int iter = (int) Math.ceil(can.iterations.value());


                long start = System.nanoTime();
                for (int i = 0; i < iter; i++) {
                    workRemaining = plan.size();

                    plan.commit()
                            .sample(super::exeSample);

                    Activate.BatchActivate.get().commit(nar);
                }
                long end = System.nanoTime();

                //HACK
                float valueSum = 0;
                for (Cause c : nar.causes) {
                    if (c instanceof Conclude.RuleCause) {
                        valueSum += c.value();
                    }
                }
                valueSum = (Util.tanhFast(valueSum) + 1f) / 2f;

                System.err.println(valueSum + " " + n4((end - start) / 1.0E9) + " sec");
                can.update(iter, valueSum, (end - start) / 1.0E9);
            }
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
            sub[which(t)].queue(t);
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

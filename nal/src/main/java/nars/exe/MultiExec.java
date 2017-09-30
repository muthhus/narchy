package nars.exe;

import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import jcog.Util;
import jcog.event.On;
import nars.NAR;
import nars.Task;
import nars.control.Activate;
import nars.task.ITask;
import nars.task.NativeTask;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

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
        }
        on = nar.onCycle(this::cycle);
    }

    class Sub extends UniExec implements Runnable {

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

                final float s = ((DisruptorBlockingQueue) q).size();
                int maxToPoll = (int) (s / MultiExec.this.concurrency());
                for (int i = 0; i < maxToPoll; i++) {
                    ITask k = q.poll();
                    if (k != null)
                        execute(k);
                    else
                        break;
                }

                workRemaining = /*Math.min(batchSize, */plan.size();//);
                plan.commit()
                        .sample(super::exeSample);

                Activate.BatchActivate.get().commit(nar);
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

    public Stream<ITask> stream() {
        return Stream.of(sub).flatMap(UniExec::stream);
    }

}

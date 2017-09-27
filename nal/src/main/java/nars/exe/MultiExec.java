package nars.exe;

import jcog.Util;
import jcog.event.On;
import jcog.pri.Prioritized;
import jcog.sort.TopN;
import nars.NAR;
import nars.task.ITask;
import nars.task.NativeTask;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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

    final Sub[] sub;
    private final int num;
    private On onCycle;

    final static int SUB_CAPACITY = 1024;

    @Override
    public void execute(@NotNull Runnable runnable) {
        add(new NativeTask.RunTask(runnable));
    }

    public Stream<ITask> stream() {
        return Stream.of(sub).flatMap(UniExec::stream);
    }


    public MultiExec(int threads, int qSize) {
        num = threads;
        sub = new Sub[num];
        q = Util.blockingQueue(qSize);
    }

    @Override
    public float load() {
        int siz = q.size();
        int cap = q.remainingCapacity();
        return ((float) siz) / (siz + cap);
    }

    @Override
    public synchronized void start(NAR nar) {
        super.start(nar);
        exe = Executors.newFixedThreadPool(num);
        for (int i = 0; i < num; i++) {

            exe.execute( sub[i] = new Sub(nar, SUB_CAPACITY));

//            exe.execute(() -> {
//                int idle = 0;
//                while (true) {
//                    ITask r = q.poll();
//                    if (r != null) {
//                        execute(r);
//                        idle = 0;
//                    } else {
//                        Util.pauseNext(++idle);
//                    }
//                }
//            });
        }
    }


    class Sub extends UniExec implements Runnable {

        int batchSize;

        Sub(NAR nar, int capacity) {
            super(capacity);
            this.batchSize = Math.max(1, capacity/16);
            start(nar);
        }

        public void run() {
            while (true) {

                int pending = q.size();
                if (pending > 0) {
                    ITask i = q.poll();
                    if (i != null)
                        execute(i);
                }

                System.out.println(plan.size());

                workRemaining = batchSize;
                plan.commit(null)
                        .sample( super::exeSample);


            }
        }

        public void queue(ITask input) {
            plan.putAsync(u(input));
        }

        @Override
        public void add(ITask input) {
            MultiExec.this.add(input);
        }

        protected void execute(ITask x) {
            Iterable<? extends ITask> y;
            try {
                y = x.run(nar);
                if (y!=null)
                    y.forEach(MultiExec.this::add);
            } catch (Throwable t) {
                logger.error("{} {}", x, t);
                return;
            }

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
//        if (!q.offer(t)) {
//            drainEvict(t);
//        }
        if (t instanceof NativeTask) {
            int stall = 0;
            while (!q.offer(t)) {
                Util.pauseNext(stall++);
                if (stall > 128) {
                    drainEvict(t);
                    break;
                }
            }
        } else {
            sub[which(t)].queue(t);
        }
    }

    protected int which(ITask t) {
        return Math.abs(t.hashCode()) % sub.length;
    }

    public boolean tryAdd(/*@NotNull*/ ITask t) {
//        if (!q.offer(t)) {
//            drainEvict(t);
//        }
        return q.offer(t);
    }

    private void drainEvict(ITask t) {
        //1. reduce system power parameter
        //TODO

        Util.pauseNext(0);

        //2. attempt to evict any weaker tasks consuming space
        int sample = Math.max(4, q.size() / (num + 1) / 8);

        int survive = Math.round(sample * 0.5f);

        TopN<ITask> tmpEvict = new TopN<>(new ITask[survive], Prioritized::priElseNeg1) {
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


    @Override
    public int concurrency() {
        return num; //TODO calculate based on # of sub-NAR's but definitely is concurrent so we add 1 here in case passive=1
    }

    @Override
    public boolean concurrent() {
        return true;
    }



}
//class Reasoner extends Causable /* TODO use Throttled */ implements Consumer<Activate> {
//        private final FocusExec sub;
//
//        /**
//         * customizable filter, or striping so that every reasoner doesnt get the same inputs
//         */
//        private final Predicate<Activate> filter;
//        private final Cause out;
//
//        public Reasoner(FocusExec focusExec, Predicate<Activate> filter) {
//            super(nar);
//            this.sub = focusExec;
//            this.filter = filter;
//            this.out = nar.newCauseChannel(this);
//            sub.cause = this.out.id;
//            sub.start(nar);
//        }
//
//        @Override
//        public boolean singleton() {
//            return true;
//        }
//
//
//        @Override
//        public float value() {
//            return out.amp();
//        }
//
//        @Override
//        protected int next(NAR n, int work) {
//            FocusExec f = (FocusExec) this.sub;
//            f.run(work);
//            return work; //TODO better estimate than this, even to the precision of the TTL spent
//        }
//
//        public void stop() {
//            sub.stop();
//        }
//
//        @Override
//        public void accept(Activate a) {
//            if (filter.test(a))
//                sub.add(a);
//        }
//    }
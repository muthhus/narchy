package nars.nar.exe;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.BasicExecutor;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.dsl.ProducerType;
import nars.NAR;
import nars.Task;
import net.openhft.affinity.AffinityLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Created by me on 8/16/16.
 */
public class MultiThreadExecutioner extends Executioner {

    private static final Logger logger = LoggerFactory.getLogger(MultiThreadExecutioner.class);

    private final int threads;
    private final RingBuffer<TaskEvent> ring;
    private final Executor exec;
    private SequenceBarrier barrier;
    private long cursor;
    //private long cursor;


    static final class TaskEvent {
        @Nullable
        public Task[] tasks;
        @Nullable
        public Runnable runnable;
    }

    @NotNull
    final Disruptor<TaskEvent> disruptor;


    public MultiThreadExecutioner(int threads, int ringSize) {
        this(threads, ringSize,
                new BasicExecutor(Executors.defaultThreadFactory()) {

                    @Override public void execute(Runnable command) {
                        super.execute(()->{

                            try (AffinityLock al = AffinityLock.acquireLock()) {

                                //logger.info("CPU Thread AffinityLock {} ACQUIRE {}", al.toString() , al.cpuId() );

                                command.run();

                            }

                            //logger.info("CPU Thread AffinityLock RELEASE");

                        });
                    }
                }

//                new ForkJoinPool(
//                        threads+extraThreads,
//                        //Runtime.getRuntime().availableProcessors()-1 /* leave one thread available */,
//
//                        defaultForkJoinWorkerThreadFactory,
//
//                        null, true /* async */,
//                        threads+extraThreads,
//                        threads*2+extraThreads, //max threads (safe to increase)
//                        threads+extraThreads, //minimum threads to keep running otherwise new ones will be created
//                        null, 10L, TimeUnit.MILLISECONDS)
        );
    }

    public MultiThreadExecutioner(int threads, int ringSize, Executor exe) {

        this.threads = threads;
        this.exec = exe;


        this.disruptor = new Disruptor<>(
                TaskEvent::new,
                ringSize /* ringbuffer size */,
                exe,
                ProducerType.MULTI,
                new SleepingWaitStrategy()
                //new BlockingWaitStrategy()
                //new LiteTimeoutBlockingWaitStrategy(10, TimeUnit.MILLISECONDS)
                //new LiteBlockingWaitStrategy()
        );

        this.ring = disruptor.getRingBuffer();
        //this.cursor = ring.getCursor();

    }

    @Override
    public void stop() {
        synchronized (disruptor) {
            disruptor.shutdown();
            disruptor.halt();
        }
    }

    @Override
    public int concurrency() {
        return threads;
    }

    @Override
    public void next(@NotNull NAR nar) {
        //nar.eventFrameStart.emitAsync(nar, runWorker);

        Consumer[] vv = nar.eventFrameStart.getCachedNullTerminatedArray();
        if (vv != null) {
            for (int i = 0; ; ) {
                Consumer c = vv[i++];
                if (c != null) {
                    execute(() -> { //TODO these Runnables can be cached in an array parallel to 'vv'
                        c.accept(nar);
                    });
                } else
                    break; //null terminator hit
            }
        }

    }

    @Override
    public void start(@NotNull NAR nar) {

        WorkHandler[] taskWorker = new WorkHandler[threads];
        for (int i = 0; i < threads; i++)
            taskWorker[i] = new TaskEventWorkHandler(nar);
        EventHandlerGroup workers = disruptor.handleEventsWithWorkerPool(taskWorker);

        barrier = workers.asSequenceBarrier();

        disruptor.start();
    }

    @Override
    public void synchronize() {

            try {
                long lastCursor = this.cursor;
                if (lastCursor!=(this.cursor = ring.getCursor())) {
                    barrier.waitFor(cursor);
                }
            } catch (AlertException | InterruptedException | TimeoutException e1) {
                e1.printStackTrace();
            }

    }


    final static EventTranslatorOneArg<TaskEvent, Task[]> taskPublisher = (TaskEvent x, long seq, Task[] b) -> x.tasks = b;

    @Override
    public final void inputLater(@NotNull Task[] t) {
        if (!ring.tryPublishEvent(taskPublisher, t)) {
            overflow(t);
        }
    }

    public static void overflow(@NotNull Task[] t) {
        //TODO use a bag to collect these
        logger.warn("dropped: {}", t);
    }

    final static EventTranslatorOneArg<TaskEvent, Runnable> runPublisher = (TaskEvent x, long seq, Runnable b) -> {
        x.runnable = b;
    };

    @Override
    public final void execute(@NotNull Runnable r) {
        disruptor.publishEvent(runPublisher, r);
    }

    @Override
    public final boolean executeMaybe(Runnable r) {
        return ring.tryPublishEvent(runPublisher, r);
    }

    private final static class TaskEventWorkHandler implements WorkHandler<TaskEvent> {
        @NotNull
        private final NAR nar;

        public TaskEventWorkHandler(@NotNull NAR nar) {
            this.nar = nar;
        }

        @Override
        public void onEvent(@NotNull TaskEvent te) throws Exception {
            Task[] tt = te.tasks;
            if (tt != null) {
                te.tasks = null;
                nar.input(tt);
            }

            Runnable rr = te.runnable;
            if (rr != null) {
                te.runnable = null;
                rr.run();
            }
        }
    }

}

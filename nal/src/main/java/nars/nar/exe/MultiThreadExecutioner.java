package nars.nar.exe;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.dsl.ProducerType;
import nars.NAR;
import nars.Task;
import nars.nar.exe.util.AffinityExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Created by me on 8/16/16.
 */
public class MultiThreadExecutioner extends Executioner {

    private static final Logger logger = LoggerFactory.getLogger(MultiThreadExecutioner.class);

    private final int threads;
    private final RingBuffer<TaskEvent> ring;
    private final AffinityExecutor exe;

    //private CPUThrottle throttle;

    private SequenceBarrier barrier;
    private long cursor;
    private boolean sync;

    public void sync(boolean b) {
        this.sync = b;
    }


    enum EventType {
        TASK_ARRAY, RUNNABLE, NAR_CONSUMER
    }

    static final class TaskEvent {
        @Nullable EventType e;
        @Nullable Object val;
//        @Nullable
//        public Task[] tasks;
//        @Nullable
//        public Runnable runnable;
//        @Nullable
//        public Consumer<NAR> nar;
    }

    @NotNull
    final Disruptor<TaskEvent> disruptor;


//    public MultiThreadExecutioner(int threads, int ringSize) {
//        this(threads, ringSize
//
//
////                new ForkJoinPool(
////                        threads+extraThreads,
////                        //Runtime.getRuntime().availableProcessors()-1 /* leave one thread available */,
////
////                        defaultForkJoinWorkerThreadFactory,
////
////                        null, true /* async */,
////                        threads+extraThreads,
////                        threads*2+extraThreads, //max threads (safe to increase)
////                        threads+extraThreads, //minimum threads to keep running otherwise new ones will be created
////                        null, 10L, TimeUnit.MILLISECONDS)
//        );
//    }
//

    public MultiThreadExecutioner(int threads, int ringSize) {

        this.threads = threads;

        this.exe = new AffinityExecutor("exe");

        this.disruptor = new Disruptor<>(
                TaskEvent::new,
                ringSize /* ringbuffer size */,
                exe,
                ProducerType.MULTI,
                //new SleepingWaitStrategy()
                //new BlockingWaitStrategy()
                //new LiteTimeoutBlockingWaitStrategy(10, TimeUnit.MILLISECONDS)
                new LiteBlockingWaitStrategy()
        );

        this.ring = disruptor.getRingBuffer();
        //this.cursor = ring.getCursor();

    }

    @Override
    public boolean sync() {
        return sync;
    }

    @Override
    public void stop() {
        super.stop();

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

        //SYNCHRONIZE -----------------------------
        if (sync) {
            try {
                long lastCursor = this.cursor;
                if (lastCursor != (this.cursor = ring.getCursor())) {
                    barrier.waitFor(cursor);
                }
            } catch (AlertException | InterruptedException | TimeoutException e1) {
                logger.error("Barrier Synchronization: {}", e1);
            }
        } else {
            if (!ring.hasAvailableCapacity(((int)(0.25f * ring.getBufferSize())))) {
                return; //allow buffer to clear
            }
        }


        Consumer[] vv = nar.eventFrameStart.getCachedNullTerminatedArray();
        if (vv != null) {
            for (int i = 0; ; ) {
                Consumer c = vv[i++];
                if (c != null) {
                    run(c);
                } else
                    break; //null terminator hit
            }
        }



    }

    @Override
    public void start(@NotNull NAR nar) {
        super.start(nar);

        WorkHandler[] taskWorker = new WorkHandler[threads];
        for (int i = 0; i < threads; i++)
            taskWorker[i] = new TaskEventWorkHandler(nar);
        EventHandlerGroup workers = disruptor.handleEventsWithWorkerPool(taskWorker);


        barrier = workers.asSequenceBarrier();

        disruptor.start();

        this.sync =
                //!(nar.clock instanceof RealtimeClock);
                true;

//        this.throttle = new CPUThrottle(
//                new MutableFloat(25));
//                //new MutableFloat(0.5f),
//                //exe.threadIDs());
    }

//    @Override public float throttle() {
//        return throttle.throttle;
//    }


    final static EventTranslatorOneArg<TaskEvent, Task[]> taskPublisher = (TaskEvent x, long seq, Task[] b) -> {
        x.e = EventType.TASK_ARRAY;
        x.val = b;
    };

    final static EventTranslatorOneArg<TaskEvent, Runnable> runPublisher = (TaskEvent x, long seq, Runnable b) -> {
        x.e = EventType.RUNNABLE;
        x.val = b;
    };

    final static EventTranslatorOneArg<TaskEvent, Consumer<NAR>> narPublisher = (TaskEvent x, long seq, Consumer<NAR> b) -> {
        x.e = EventType.NAR_CONSUMER;
        x.val = b;
    };

    @Override
    public final void run(@NotNull Runnable r) {
        //disruptor.publishEvent(runPublisher, r);
        if (!ring.tryPublishEvent(runPublisher, r)) {
            logger.warn("dropped: {}", r);
        }
    }

    @Override
    public final void run(@NotNull Consumer<NAR> r) {
        disruptor.publishEvent(narPublisher, r);
    }


    @Override
    public final void run(@NotNull Task[] t) {
        if (!ring.tryPublishEvent(taskPublisher, t)) {
            //TODO use a bag to collect these
            logger.warn("dropped: {}", t);
        }
    }

    private final static class TaskEventWorkHandler implements WorkHandler<TaskEvent> {
        @NotNull
        private final NAR nar;

        public TaskEventWorkHandler(@NotNull NAR nar) {
            this.nar = nar;
        }

        @Override
        public final void onEvent(@NotNull TaskEvent te) {
            Object val = te.val;
            te.val = null;
            switch (te.e) {
                case TASK_ARRAY:
                    nar.input((Task[])val);
                    break;
                case RUNNABLE:
                    ((Runnable)val).run();
                    break;
                case NAR_CONSUMER:
                    ((Consumer<NAR>)val).accept(nar);
                    break;
                default:
                    throw new RuntimeException();
            }
        }


    }



}

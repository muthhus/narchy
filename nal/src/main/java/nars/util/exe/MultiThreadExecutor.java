package nars.util.exe;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.dsl.ProducerType;
import jcog.AffinityExecutor;
import nars.NAR;
import nars.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Created by me on 8/16/16.
 */
public class MultiThreadExecutor extends Executioner {

    private static final Logger logger = LoggerFactory.getLogger(MultiThreadExecutor.class);

    /** actual load value to report as 100% load */
    public final long safetyLimit;

    private final int threads;
    private final RingBuffer<TaskEvent> ring;
    @NotNull
    private final AffinityExecutor exe;
    private final int cap;

    //private CPUThrottle throttle;

    private SequenceBarrier barrier;
    private long cursor;
    private boolean sync = true;


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

    public MultiThreadExecutor(int threads, int ringSize, boolean sync) {
        this(threads, ringSize);
        sync(sync);
    }

    public MultiThreadExecutor(int threads, int ringSize) {

        this.threads = threads;

        this.exe = new AffinityExecutor("exe");

        this.cap = ringSize;

        float safetyThreshold = 0.1f;
        this.safetyLimit =
                (int)(Math.ceil(ringSize * safetyThreshold));
                //1;

        this.disruptor = new Disruptor<>(
                TaskEvent::new,
                ringSize /* ringbuffer size */,
                exe,
                ProducerType.MULTI,
                new BusySpinWaitStrategy()
                /*new PhasedBackoffWaitStrategy(250,500, TimeUnit.MICROSECONDS,
                        new LiteBlockingWaitStrategy())*/
                //new LiteBlockingWaitStrategy()
                //new SleepingWaitStrategy()
                //new BlockingWaitStrategy()
                //new LiteTimeoutBlockingWaitStrategy(0, TimeUnit.MILLISECONDS)

        );

        this.ring = disruptor.getRingBuffer();
        //this.cursor = ring.getCursor();

    }

    public MultiThreadExecutor sync(boolean b) {
        this.sync = b;
        return this;
    }


    @Override
    public synchronized void stop() {

        synchronized (disruptor) {
            super.stop();

            disruptor.shutdown();
            disruptor.halt();
        }
    }

    @Override
    public final float load() {

        int remaining = (int) ring.remainingCapacity();
        if (remaining < safetyLimit)
            return 1f;

        return Math.max(0f, 1f - ((float)ring.remainingCapacity() / (cap - safetyLimit)));
    }

    @Override
    public int concurrency() {
        return threads;
    }

    @Override
    public void cycle(@NotNull NAR nar) {

        //SYNCHRONIZE -----------------------------
        if (sync) {
            try {
                long lastCursor = this.cursor;
                if (lastCursor != (this.cursor = ring.getCursor())) {
                    barrier.waitFor(cursor);
                }
            } catch (@NotNull AlertException | InterruptedException | TimeoutException e1) {
                logger.error("Barrier Synchronization: {}", e1);
            }
        } /*else {
            if (!ring.hasAvailableCapacity(((int)(0.25f * ring.getBufferSize())))) {
                return; //allow buffer to clear
            }
        }*/

        Consumer[] vv;
        synchronized (nar.eventCycleStart) {
            vv = nar.eventCycleStart.getCachedNullTerminatedArray();
        }
        if (vv != null) {
            for (int i = 0; ; ) {
                Consumer c = vv[i++];
                if (c == null)
                    break; //null terminator hit

                nar.exe.run(c); //use nar.exe because this executor may be wrapped in something else that we want this to pass through, ie. instrumentor
            }
        }

    }

    @Override
    public synchronized void start(@NotNull NAR nar) {
        super.start(nar);

        WorkHandler[] taskWorker = new WorkHandler[threads];
        for (int i = 0; i < threads; i++)
            taskWorker[i] = new TaskEventWorkHandler(nar);
        EventHandlerGroup workers = disruptor.handleEventsWithWorkerPool(taskWorker);


        barrier = workers.asSequenceBarrier();



        disruptor.start();

//        nar.eventReset.on((n) -> {
//            sync(); //should empty all pending tasks
//        });

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
            r.run(); //execute in own thread
            //logger.warn("dropped: {}", r);
        }
    }

    @Override
    public final void run(@NotNull Consumer<NAR> r) {
        //disruptor.publishEvent(narPublisher, r);
        if (!ring.tryPublishEvent(narPublisher, r)) {
            r.accept(nar); //execute in own thread
            //logger.warn("dropped: {}", r);
        }
    }


    @Override
    public final void run(@NotNull Task... t) {
        if (!ring.tryPublishEvent(taskPublisher, t)) {
            nar.processAll(t); //execute in own thread
            //logger.warn("dropped: {}", t);
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
                    nar.processAll((Task[])val);
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

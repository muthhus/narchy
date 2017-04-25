package nars.util.exe;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.dsl.ProducerType;
import jcog.AffinityExecutor;
import nars.NAR;
import nars.Param;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Created by me on 8/16/16.
 */
public class MultiThreadExecutor extends Executioner  {

    private static final Logger logger = LoggerFactory.getLogger(MultiThreadExecutor.class);

    /** actual load value to report as 100% load */
    public final long safetyLimit;

    private final int threads;
    private final RingBuffer<TaskEvent> ring;
    @NotNull
    private final Executor exe;
    private final int cap;

    //private CPUThrottle throttle;

    private SequenceBarrier barrier;
    private long cursor;
    private boolean sync = true;

//    @Override
//    public void handleEventException(Throwable ex, long sequence, TaskEvent event) {
//
//    }
//
//    @Override
//    public void handleOnStartException(Throwable ex) {
//        logger.error("start: {}", ex);
//    }
//
//    @Override
//    public void handleOnShutdownException(Throwable ex) {
//        logger.error("stop: {}", ex);
//    }


    enum EventType {
        RUNNABLE, NAR_CONSUMER
    }

    static final class TaskEvent {
        @Nullable Object val;
    }

    @NotNull
    final Disruptor<TaskEvent> disruptor;





    public MultiThreadExecutor(int threads, int ringSize, boolean sync) {
        this(threads, ringSize, sync, new AffinityExecutor("exe"));
    }

    public MultiThreadExecutor(int threads, int ringSize, boolean sync, Executor exe) {
        this(threads, ringSize, exe);
        sync(sync);
    }

    public MultiThreadExecutor(int threads, int ringSize, Executor exe) {

        this.threads = threads;

        this.exe = exe;

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
                //new BusySpinWaitStrategy()
                //new PhasedBackoffWaitStrategy(1,1, TimeUnit.MILLISECONDS,
                //        new SleepingWaitStrategy())
                //new LiteBlockingWaitStrategy()
                new SleepingWaitStrategy()
                //new BlockingWaitStrategy()
                //new LiteTimeoutBlockingWaitStrategy(1, TimeUnit.MILLISECONDS)
        );

        this.ring = disruptor.getRingBuffer();
        //this.cursor = ring.getCursor();


        WorkHandler[] taskWorker = new WorkHandler[threads];
        for (int i = 0; i < threads; i++)
            taskWorker[i] = new TaskEventWorkHandler();
        EventHandlerGroup workers = disruptor.handleEventsWithWorkerPool(taskWorker);



        barrier = workers.asSequenceBarrier();



    }

    public MultiThreadExecutor sync(boolean b) {
        this.sync = b;
        return this;
    }



    @Override
    public void stop() {

        synchronized (disruptor) {

            disruptor.shutdown();
            disruptor.halt();

            super.stop();
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

        Consumer[] vv = nar.eventCycleStart.getCachedNullTerminatedArray();
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
        synchronized (disruptor) {
            super.start(nar);
            disruptor.start();
        }

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


    final static EventTranslatorOneArg<TaskEvent, Runnable> runPublisher = (TaskEvent x, long seq, Runnable b) -> {
        x.val = b;
    };

    final static EventTranslatorOneArg<TaskEvent, Consumer<NAR>> narPublisher = (TaskEvent x, long seq, Consumer<NAR> b) -> {
        x.val = b;
    };


    @Override
    public final void run(@NotNull Runnable r) {
        if (!ring.tryPublishEvent(runPublisher, r)) {
            r.run(); //execute in callee's thread
        }
    }

    @Override
    public final void run(@NotNull Consumer<NAR> r) {
        if (!ring.tryPublishEvent(narPublisher, r)) {
            r.accept(nar); //execute in callee's thread
        }
    }


    private final class TaskEventWorkHandler implements WorkHandler<TaskEvent> {

        @Override
        public final void onEvent(@NotNull TaskEvent te) {

            Object val = te.val;
            te.val = null;

            try {
                if (val instanceof Runnable) {
                    ((Runnable) val).run();
                } else {
                    ((Consumer) val).accept(nar);
                }
            } catch (Throwable t) {
                if (Param.DEBUG)
                    logger.error("{} {} ", val, t);
            }
        }


    }



}

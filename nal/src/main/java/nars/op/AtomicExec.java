package nars.op;

import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.task.NativeTask;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static nars.time.Tense.ETERNAL;

/**
 * debounced and atomically/asynchronously executable operation
 */
public class AtomicExec implements BiFunction<Task, NAR, Task> {

    private final float minPeriod;
    private final float expThresh;

    /**
     * time of the current rising edge, or ETERNAL if not activated
     */
    final AtomicLong rise = new AtomicLong(ETERNAL);

    /**
     * how many durations before the current time in which a goal remains active in the present
     */
    final static float presentDurs = 0.5f;
    private final float freqThreshPrefilter;

    long lastActivity = ETERNAL;
    public static final Logger logger = LoggerFactory.getLogger(AtomicExec.class);

    final BiConsumer<Task, NAR> exe;

    public AtomicExec(BiConsumer<Task, NAR> exe, float expThresh) {
        this(exe, expThresh, 0);
    }

    public AtomicExec(BiConsumer<Task, NAR> exe, float expThresh, float minRecoveryPeriod /* dur's */) {
        this.exe = exe;
        this.minPeriod = minRecoveryPeriod;
        this.expThresh = expThresh;
        this.freqThreshPrefilter = 0.5f;
    }

    @Override
    public @Nullable Task apply(Task x, NAR n) {

        if (x.freq() < freqThreshPrefilter)
            return x; //dont even think about executing it

        if (!exePrefilter(x))
            return x; //pass thru

        long now = n.time();
        int dur = n.dur();

        long xs = x.start();

        FutureTask possiblyExec = new FutureTask(xs, x);

        if (xs <= now + presentDurs * dur) {
            n.runLater(possiblyExec); //triggers truth eval AFTER the cycle is over. coalesces with equivalent tasks
        } else {
            //schedule for future execution
            n.time.at(xs, possiblyExec);
        }

        return x;
    }

    protected boolean exePrefilter(Task x) {
        return true;
    }

    class FutureTask extends NativeTask.SchedTask {

        private final Task task;

        public FutureTask(long whenOrAfter, Task x) {
            super(whenOrAfter, (NAR nn) -> tryInvoke(x, nn));
            this.task = x;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof FutureTask && ((FutureTask)obj).task.equals(task) && ((FutureTask)obj).when == when;
        }
    }

    public void tryInvoke(Task x, NAR n) {

        long now= n.time();
        if (lastActivity == ETERNAL || ((now ) - lastActivity > minPeriod * n.dur()) && rise.compareAndSet(ETERNAL, now)) {
            try {
                @Nullable Concept cc = x.concept(n, true);
                if (cc != null) {
                    Truth desire = cc.goals().truth(now, n);
                    if (desire != null && desire.expectation() >= expThresh) {
                        exe.accept(x, n);
                    }
                }
            } catch (Throwable t) {
                logger.info("{} {}", this, t);
            } finally {
                //end invocation
                lastActivity = n.time();
                rise.set(ETERNAL);
            }
        }
    }

    public boolean isInvoked() {
        return rise.get() != ETERNAL;
    }

}

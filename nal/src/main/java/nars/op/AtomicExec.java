package nars.op;

import jcog.bag.impl.ArrayBag;
import jcog.pri.PLink;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.control.DurService;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static nars.time.Tense.ETERNAL;

/**
 * debounced and atomically/asynchronously executable operation
 */
public class AtomicExec implements BiFunction<Task, NAR, Task> {

    //    private final float minPeriod;
    private final float desireThresh;

//    /**
//     * time of the current rising edge, or ETERNAL if not activated
//     */
//    final AtomicLong rise = new AtomicLong(ETERNAL);
//
//    /**
//     * how many durations before the current time in which a goal remains active in the present
//     */
//    final static float presentDurs = 0.5f;

//
//    long lastActivity = ETERNAL;

    static final Logger logger = LoggerFactory.getLogger(AtomicExec.class);

    final BiConsumer<Task, NAR> exe;

    final static int ACTIVE_CAPACITY = 16;
    final ArrayBag<Term, PLink<Term>> active = new ArrayBag<Term, PLink<Term>>(PriMerge.max, new HashMap()) {
        @Nullable
        @Override
        public Term key(PLink<Term> l) {
            return l.get();
        }
    };

    private DurService onCycle;

    public AtomicExec(BiConsumer<Task, NAR> exe, float dThresh) {
        this(exe, dThresh, 0);
    }

    public AtomicExec(BiConsumer<Task, NAR> exe, float dThresh, @Deprecated float minRecoveryPeriod /* dur's */) {
        this.exe = exe;
        active.setCapacity(ACTIVE_CAPACITY);
        this.desireThresh = dThresh;
    }

    /**
     * implementations can override this to prefilter invalid operation patterns
     */
    protected boolean exePrefilter(Task x) {
        return true;
    }

    protected synchronized void update(NAR n) {
        //probe all active concepts.
        //  remove any below desire threshold
        //  execute any above desire-belief threshold
        //  if no active remain, disable update service

        assert(!active.isEmpty());

        long now = n.time();
        int dur = n.dur();
        long start = now - dur /2;
        long end = now + dur /2;
        active.forEach(x -> {
            Task desire = n.goal(x.get(), start, end);
            Truth desireTruth;
            float dFreq;
            if (desire == null
                    || (desireTruth = desire.truth(now,  now)) == null
                    || (dFreq = desireTruth.freq()) < desireThresh) {
                x.delete();
                return;
            }
            Truth belief = n.beliefTruth(x.get(), start, end);
            float bFreq = belief == null ? 0 /* assume false with no evidence */ : belief.freq();

            float delta = dFreq - bFreq;
            if (delta > desireThresh) {
                n.runLater(()->exe.accept(desire, n));
            }
        });
        active.commit();
        if (active.isEmpty()) {
            onCycle.stop();
            onCycle = null;
        }
    }

    @Override
    public @Nullable Task apply(Task x, NAR n) {

        if (x.expectation() < desireThresh)
            return x; //dont even think about executing it, but pass thru to reasoner

        if (x.meta("mimic")!=null)
            return x; //filter instructive tasks (would cause feedback loop)

        if (!exePrefilter(x))
            return x; //pass thru to reasoner

        active.put(new PLink(x.term().root() /* incase it contains temporal, we will dynamically match task anyway on invocation */,
                x.priElseZero()
        ));

        if (onCycle == null) {
            onCycle = DurService.on(n, this::update);
        }

        return x;
    }


//    public void tryInvoke(Task x, NAR n) {
//
//        long now = n.time();
//        if (!x.isDeleted() && lastActivity == ETERNAL || ((now) - lastActivity > minPeriod * n.dur()) && rise.compareAndSet(ETERNAL, now)) {
//            try {
//                @Nullable Concept cc = x.concept(n, true);
//                if (cc != null) {
//                    Truth desire = cc.goals().truth(now, n);
//                    if (desire != null && desire.expectation() >= desireThresh) {
//                        exe.accept(x, n);
//                    }
//                }
//            } catch (Throwable t) {
//                logger.info("{} {}", this, t);
//            } finally {
//                //end invocation
//                lastActivity = n.time();
//                rise.set(ETERNAL);
//            }
//        }
//    }

//    class FutureTask extends NativeTask.SchedTask {
//
//        private final Task task;
//
//        public FutureTask(long whenOrAfter, Task x) {
//            super(whenOrAfter, (NAR nn) -> tryInvoke(x, nn));
//            this.task = x;
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            return obj instanceof FutureTask && ((FutureTask)obj).task.equals(task) && ((SchedTask) obj).when == when;
//        }
//    }

}

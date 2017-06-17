package nars.nar;

import jcog.AffinityExecutor;
import jcog.Loop;
import jcog.Util;
import jcog.event.On;
import jcog.math.FloatAveraged;
import jcog.pri.classify.EnumClassifier;
import jcog.pri.mix.PSinks;
import jcog.pri.mix.control.MultiHaiQMixAgent;
import jcog.pri.mix.control.RLMixControl;
import nars.$;
import nars.NAR;
import nars.NARLoop;
import nars.Task;
import nars.attention.Activate;
import nars.conceptualize.DefaultConceptBuilder;
import nars.control.ConceptFire;
import nars.index.term.HijackTermIndex;
import nars.task.ITask;
import nars.task.NALTask;
import nars.time.Time;
import nars.util.exe.Executioner;
import nars.util.exe.TaskExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.util.concurrent.ForkJoinPool.defaultForkJoinWorkerThreadFactory;
import static nars.Op.*;

/**
 * recursive cluster of NAR's
 * <sseehh> any hierarchy can be defined including nars within nars within nars
 * <sseehh> each nar runs in its own thread
 * <sseehh> they share concepts
 * <sseehh> but not the importance of concepts
 * <sseehh> each one has its own concept attention
 * <sseehh> link attention is currently shared but ill consider if this needs changing
 */
public class NARS extends NAR {


    public final List<SubExecutor> sub = $.newArrayList();
    private int num;

    private AffinityExecutor pool;
    private List<Loop> loops;

    NARS(@NotNull Time time, @NotNull Random rng, Executioner e) {
        super(time,
                new HijackTermIndex(new DefaultConceptBuilder(), 128 * 1024, 4),
                //new CaffeineIndex(new DefaultConceptBuilder(), 96*1024, -1, e),
                rng, e);
    }

    @Override
    protected PSinks newInput() {
        RLMixControl<String, ITask> r = new RLMixControl<>(this::inputSub, 15f,

                //new HaiQMixAgent(),
                new MultiHaiQMixAgent(),

                FloatAveraged.averaged(emotion.happy.sumIntegrator()::meanThenClear, 2),

                10,

                new EnumClassifier<>("type", new String[]{
                        "Belief", "Goal", "Question", "Quest",
                        "Activation", "ConceptFire"
                }, (x) -> {

                    if (x instanceof Task) {
                        //NAL
                        switch (x.punc()) {
                            case BELIEF:
                                return 0;
                            case GOAL:
                                return 1;
                            case QUESTION:
                                return 2;
                            case QUEST:
                                return 3;
                        }
                    } else if (x instanceof Activate) {
                        return 4;
                    } else if (x instanceof ConceptFire) {
                        return 5;
                    }

                    return -1;
                }),

                new EnumClassifier<>("complexity", 3, (t) -> {
                    if (t instanceof NALTask) {
                        int c = ((NALTask) t).complexity();
                        int m = termVolumeMax.intValue();
                        if (c < m / 4) return 0;
                        if (c < m / 2) return 1;
                        return 2;
                    }
                    return -1;
                }),

                new EnumClassifier<>("when", new String[]{"Present", "Future", "Past"}, (t) -> {
                    if (t instanceof NALTask) {
                        long now = time();
                        long h = ((NALTask) t).nearestStartOrEnd(now);
                        if (Math.abs(h - now) <= dur() * 2) {
                            return 0; //present
                        } else if (h > now) {
                            return 1; //future
                        } else {
                            return 2; //past
                        }
                    }
                    return -1;
                })

//            new MixRouter.Classifier<>("original",
//                    (x) -> x.stamp().length <= 2),
//            new MixRouter.Classifier<>("unoriginal",
//                    (x) -> x.stamp().length > 2),
        );


        return r;
    }

    @Override
    public void input(ITask x) {
        if (x == null)
            return;
        ((RLMixControl) in).accept(x);
    }

    void inputSub(ITask x) {
        int sub =
                //random.nextInt(num);
                Math.abs(Util.hashWangJenkins(x.hashCode())) % num;
        this.sub.get(sub).run(x);
    }


    /**
     * default implementation convenience method
     */
    public void addNAR(int capacity, float rate) {
        synchronized (sub) {
            SubExecutor x = new SubExecutor(capacity, rate);
            sub.add(x);
            num = sub.size();
        }
    }

    private static class RootExecutioner extends Executioner implements Runnable {

        private final int passiveThreads;

        public ForkJoinTask lastCycle;

        final ForkJoinPool passive;

        public RootExecutioner(int passiveThreads) {
            this.passiveThreads = passiveThreads;
            passive = new ForkJoinPool(passiveThreads, defaultForkJoinWorkerThreadFactory,
                    null, true /* async */);

        }


        @Override
        public void runLater(Runnable cmd) {
            passive.execute(cmd);
        }

        @Override
        public boolean run(@NotNull ITask t) {
            throw new UnsupportedOperationException("should be intercepted by class NARS");
        }

        @Override
        public void stop() {
            lastCycle = null;
            super.stop();
        }

        final AtomicBoolean busy = new AtomicBoolean(false);

        @Override
        public void cycle(@NotNull NAR nar) {


//            int waitCycles = 0;
//            while (!passive.isQuiescent()) {
//                Util.pauseNext(waitCycles++);
//            }

            if (!busy.compareAndSet(false, true))
                return; //already in the cycle


            if (lastCycle != null) {
                //System.out.println(lastCycle + " " + lastCycle.isDone());
                if (!lastCycle.isDone()) {
                    //long start = System.currentTimeMillis();
                    lastCycle.join(); //wait for lastCycle's to finish
                    //long end = System.currentTimeMillis();
                    //System.out.println("cycle lag: " + (end - start) + "ms");
                }

                lastCycle.reinitialize();
                passive.execute(lastCycle);

            } else {
                lastCycle = passive.submit(this);
            }

            busy.set(false);
        }

        /**
         * dont call directly
         */
        @Override
        public void run() {
            nar.eventCycleStart.emitAsync(nar, passive); //TODO make a variation of this for ForkJoin specifically
        }

        @Override
        public int concurrency() {
            return passiveThreads + 2; //TODO calculate based on # of sub-NAR's but definitely is concurrent so we add 1 here in case passive=1
        }

        @Override
        public boolean concurrent() {
            return true;
        }

        @Override
        public void forEach(Consumer<ITask> each) {
            ((NARS) nar).sub.forEach(s -> s.forEach(each));
        }

    }

    class SubExecutor extends TaskExecutor {
        public SubExecutor(int inputQueueCapacity, float exePct) {
            super(inputQueueCapacity, exePct);
        }

        @Override
        protected void actuallyFeedback(ITask x, ITask[] next) {
            NARS.this.input(next); //through post mix
        }

        @Override
        public void runLater(@NotNull Runnable r) {
            pool.execute(r); //use the common threadpool
        }

        public Loop start() {
            start(NARS.this);
            Loop l = new Loop(0) {
                @Override
                public boolean next() {
                    flush();
                    return true;
                }
            };
            return l;
        }
    }


    public NARS(@NotNull Time time, @NotNull Random rng, int passiveThreads) {
        this(time, rng, new RootExecutioner(passiveThreads));
    }


    public boolean running() {
        return this.loop.isRunning();
    }


    @Override
    public NARLoop startPeriodMS(int ms) {
        synchronized (terms) {

            assert (!running());

            int num = sub.size();

            this.pool = new AffinityExecutor(self().toString());

            this.loops = $.newArrayList(num);
            sub.forEach(s -> loops.add(s.start()));

            loops.forEach(pool::execute);
        }

        return super.startPeriodMS(ms);
    }

    @Override
    public void stop() {
        synchronized (terms) {
            if (!running())
                return;

            super.stop();

            loops.forEach(Loop::stop);
            this.loops = null;

            this.pool.shutdownNow();
            this.pool = null;

        }
    }

    public TreeMap<String, Object> stats() {
        synchronized (terms) {
            TreeMap<String, Object> m = new TreeMap();

            m.put("now", new Date());

            m.put("terms", terms.summary());
            return m;
        }
    }

//    public static void main(String[] args) {
//
//        NARS n = new NARS(
//                new RealTime.DSHalf(true),
//                new XorShift128PlusRandom(1), 2);
//
//
//        n.addNAR(2048);
//        n.addNAR(2048);
//
//        //n.log();
//
//        new DeductiveMeshTest(n, 5, 5);
//
//        n.start();
//
//        for (int i = 0; i < 10; i++) {
//            System.out.println(n.stats());
//            Util.sleep(500);
//        }
//
//        n.stop();
//    }
//

}

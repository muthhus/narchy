package nars.nar;


import jcog.AffinityExecutor;
import jcog.Loop;
import jcog.Util;
import jcog.math.FloatAveraged;
import jcog.pri.classify.EnumClassifier;
import jcog.pri.mix.PSinks;
import jcog.pri.mix.control.CLink;
import jcog.pri.mix.control.MixContRL;
import nars.$;
import nars.NAR;
import nars.NARLoop;
import nars.Task;
import nars.attention.Activate;
import nars.conceptualize.DefaultConceptBuilder;
import nars.control.ConceptFire;
import nars.control.NARMixAgent;
import nars.index.term.HijackTermIndex;
import nars.index.term.map.CaffeineIndex;
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
    public int num;

    private AffinityExecutor pool;
    private List<Loop> loops;

    NARS(@NotNull Time time, @NotNull Random rng, Executioner e) {
        super(new CaffeineIndex(new DefaultConceptBuilder(), 64*1024,  e) {

//                  @Override
//                  protected void onBeforeRemove(Concept c) {
//
//                      //victimize neighbors
//                      PriReference<Term> mostComplex = c.termlinks().maxBy((x -> x.get().volume()));
//                      if (mostComplex!=null) shrink(mostComplex.get());
//
//                      PriReference<Task> mostComplexTa = c.tasklinks().maxBy((x -> x.get().volume()));
//                      if (mostComplexTa!=null) shrink(mostComplexTa.get());
//
//                  }
//
//                  private void shrink(Term term) {
//                      Concept n = nar.concept(term);
//                      if (n != null) {
//                          shrink(n);
//                      }
//                  }
//
//                  private void shrink(Task task) {
//                      Concept n = task.concept(nar);
//                      if (n != null) {
//                          shrink(n);
//                      }
//                  }
//
//                  private void shrink(Concept n) {
//                      int ntl = n.termlinks().capacity();
//                      if (ntl > 0) {
//                          n.termlinks().setCapacity(ntl - 1);
//                      }
//                  }
//
//

              }, e, time,
            //new HijackTermIndex(new DefaultConceptBuilder(), 128 * 1024, 4),
                rng);
    }

    @Override
    protected PSinks newInputMixer() {
        MixContRL<ITask> r = new MixContRL<>(20f,
                null,

                FloatAveraged.averaged(emotion.happy.sumIntegrator()::sumThenClear, 1),

                8,

                new EnumClassifier("type", new String[]{
                        "Belief", "Goal", "Question", "Quest",
                        "Activation", "ConceptFire"
                }, (x) -> {

                    if (x instanceof Task) {
                        //NAL
                        switch (((Task) x).punc()) {
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

                new EnumClassifier("complexity", 3, (t) -> {
                    if (t instanceof NALTask) {
                        int c = ((NALTask) t).complexity();
                        int m = termVolumeMax.intValue();
                        if (c < m / 4) return 0;
                        if (c < m / 2) return 1;
                        return 2;
                    }
                    return -1;
                }),

                new EnumClassifier("when", new String[]{"Present", "Future", "Past"}, (t) -> {
                    if (t instanceof NALTask) {
                        long now = time();
                        long h = ((NALTask) t).nearestStartOrEnd(now);
                        if (Math.abs(h - now) <= dur()) {
                            return 0; //present
                        } else if (h > now) {
                            return 1; //future
                        } else {
                            return 2; //past
                        }
                    }
                    return -1;
                }, true)

//            new MixRouter.Classifier<>("original",
//                    (x) -> x.stamp().length <= 2),
//            new MixRouter.Classifier<>("unoriginal",
//                    (x) -> x.stamp().length > 2),
        );

        r.setAgent(
                new NARMixAgent<>(new NARBuilder()
                        .index(
                                new HijackTermIndex(new DefaultConceptBuilder(), 8*1024, 3)
                                //new CaffeineIndex(new DefaultConceptBuilder(), -1, MoreExecutors.newDirectExecutorService())
                        ).get(), r, this)

                //new HaiQMixAgent()

                //new MultiHaiQMixAgent()
        );

        return r;
    }

    @Override
    public void input(ITask unclassified) {
        if (unclassified == null)
            return;
        super.input(unclassified);
    }

    @Override
    public void input(@NotNull CLink<ITask> partiallyClassified) {
        ((MixContRL) in).test(partiallyClassified);
        super.input(partiallyClassified);
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
        public void start(NAR nar) {
            super.start(nar);
        }

        @Override
        public void runLater(Runnable cmd) {
            passive.execute(cmd);
        }

        @Override
        public boolean run(@NotNull CLink<ITask> x) {
            NARS nar = (NARS) this.nar;
            int sub =
                    //random.nextInt(num);
                    Math.abs(Util.hashWangJenkins(x.hashCode())) % nar.num;
            apply(x);
            return nar.sub.get(sub).run(x);
        }


        public void apply(CLink<ITask> x) {
            if (x!=null && !x.isDeleted())
                x.priMult(((MixContRL) (((NARS) nar).in)).gain(x));
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

            ((NARS) nar).nextCycle();
            try {

                if (lastCycle != null) {
                    //System.out.println(lastCycle + " " + lastCycle.isDone());
                    if (!lastCycle.isDone()) {
                        //long start = System.currentTimeMillis();
                        lastCycle.join(); //wait for lastCycle's to finish
                        //System.out.println("cycle lag: " + (System.currentTimeMillis() - start) + "ms");
                    }

                    lastCycle.reinitialize();
                    passive.execute(lastCycle);

                    ((NARS) nar).nextCycle();

                } else {
                    lastCycle = passive.submit(this);
                }
            } finally {
                busy.set(false);
            }
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

    protected void nextCycle() {
//        if (!((HijackMemoize)truthCache).isEmpty()) {
//            System.out.println("Truth Cache: " + truthCache.summary());
//        } else {
//            truthCache.summary(); //HACK to call stat reset
//        }
//
//        truthCache.clear();
    }

//    /** temporary 1-cycle old cache of truth calculations */
//    final Memoize<Pair<Termed, ByteLongPair>, Truth> truthCache =
//            new HijackMemoize<>(2048, 3,
//                    k -> {
//                        Truth x = super.truth(k.getOne(), k.getTwo().getOne(), k.getTwo().getTwo());
//                        if (x == null)
//                            return Truth.Null;
//                        return x;
//                    }
//            );
//
//    @Override
//    public @Nullable Truth truth(@Nullable Termed concept, byte punc, long when) {
//        Pair<Termed, ByteLongPair> key = Tuples.pair(concept, PrimitiveTuples.pair(punc, when));
//        Truth t = truthCache.apply(key);
//        if (t == Truth.Null) {
//            return null;
//        }
//        return t;
//        //return truthCache.computeIfAbsent(key, k -> super.truth(k.getOne(), k.getTwo().getOne(), k.getTwo().getTwo()));
//        //return super.truth(concept, punc, when);
//    }


    class SubExecutor extends TaskExecutor {
        public SubExecutor(int inputQueueCapacity, float exePct) {
            super(inputQueueCapacity, exePct);
        }

        @Override
        protected void actuallyRun(CLink<ITask> x) {

            super.actuallyRun(x);

            ((RootExecutioner) exe).apply(x); //apply gain after running

        }

        @Override
        protected void actuallyFeedback(CLink<ITask> x, ITask[] next) {
            if (next != null)
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


    @Override
    public NARLoop startPeriodMS(int ms) {
        assert (!this.loop.isRunning());

        synchronized (terms) {

            exe.start(this);

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
            if (!this.loop.isRunning())
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

package nars.nar;

import com.gs.collections.impl.map.mutable.ConcurrentHashMapUnsafe;
import nars.Global;
import nars.NAR;
import nars.budget.Budgeted;
import nars.budget.policy.DefaultConceptBudgeting;
import nars.concept.Concept;
import nars.index.Indexes;
import nars.index.TermIndex;
import nars.link.BLink;
import nars.nal.meta.PremiseEval;
import nars.nar.util.DefaultCore;
import nars.term.Termed;
import nars.time.Clock;
import nars.time.FrameClock;
import nars.util.data.Util;
import nars.util.data.random.XorShift128PlusRandom;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

/**
 * Multithreaded - Each core runs in its own thread
 */
public class Multi extends AbstractNAR {

    @NotNull
    final WorkerCore[] cores;
    final ConcurrentHashMapUnsafe<Concept,DefaultCore> active = new ConcurrentHashMapUnsafe<>();


    @Deprecated
    public Multi(int cores) {
        this(cores, 1024, 1, 1, 3);
    }

    public Multi(int cores, int conceptsPerCore, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept) {
        this(cores, conceptsPerCore, conceptsFirePerCycle, taskLinksPerConcept, termLinksPerConcept, new XorShift128PlusRandom(1));
    }

    public Multi(int cores, int conceptsPerCore, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept, @NotNull Random random) {
        this(cores, conceptsPerCore, conceptsFirePerCycle, taskLinksPerConcept, termLinksPerConcept, random,
                new Indexes.DefaultTermIndex(conceptsPerCore * cores * INDEX_TO_CORE_INITIAL_SIZE_RATIO, random),
                //new CaffeineIndex(new DefaultConceptBuilder(random)),
                new FrameClock());
    }


    public Multi(int cores, int conceptsPerCore, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept, @NotNull Random random, TermIndex index, @NotNull Clock clock) {
        super(clock,
                index,
                random,
                Global.DEFAULT_SELF);


        this.cores = new WorkerCore[cores];
        for (int i = 0; i < cores; i++) {
            PremiseEval matcher = new PremiseEval(random, newDeriver());
            WorkerCore core = this.cores[i] = newCore(i,
                    conceptsPerCore,
                    conceptsFirePerCycle,
                    termLinksPerConcept, taskLinksPerConcept, matcher
            );

            eventReset.on(core::reset);
        }

        eventFrameStart.on((x) -> {
            //framesPending.addAndGet(cores);
            /*for (WorkerCore w : Multi.this.cores)
                if (w.sleeping.compareAndSet(false,true)) {
                    w.wake();
                }*/
            Util.pause(1); //HACK
        });

        runLater(this::initHigherNAL);


        //new QueryVariableExhaustiveResults(this.memory());

        /*
        the("memory_sharpen", new BagForgettingEnhancer(memory, core.active));
        */

    }


    /** runs asynchronously in its own thread. counts down a # of pending cycles */
    public class WorkerCore extends DefaultCore implements Runnable {

        private final Logger logger = LoggerFactory.getLogger(WorkerCore.class);

        @NotNull
        private final Thread thread;
        private boolean stopped;
        long lastTime = -1;
        private final ConcurrentLinkedDeque<Runnable> pendingActivations = new ConcurrentLinkedDeque<>();

        public WorkerCore(int n, PremiseEval matcher, DefaultConceptBudgeting warm, DefaultConceptBudgeting cold) {
            super(Multi.this, matcher, warm, cold);
            this.thread = new Thread(this);
            thread.setName(nar.toString() + "Worker" + n);
            thread.start();
        }

        @Override
        protected synchronized void activate(@NotNull Concept c) {
            if (Multi.this.active.putIfAbsent(c, this)==null) {
                super.activate(c);
            }
        }

        @Override
        protected synchronized void deactivate(@NotNull BLink<Concept> c) {
            Concept cc = c.get();
            if (Multi.this.active.remove(cc)==cc) {
                super.deactivate(c);
            }
        }


        protected void wake() {
            logger.info("wake");
            thread.interrupt();
        }



        public void stop() {
            stopped = true;
        }

        @Override public final void run() {
            while (!stopped) {
                long now = time();
                if (now > lastTime) {
                    logger.info("frame " + now);
                    try {
                        frame(Multi.this);
                        lastTime = now;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    //logger.info("sleep");
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {  }
                }
            }
        }

        @Override
        public void frame(@NotNull NAR nar) {
            int n = pendingActivations.size();
            for (int i = 0; i < n; i++) {
                pendingActivations.removeFirst().run();
            }

            super.frame(nar);
        }

        @Override public void conceptualize(@NotNull Concept c, @NotNull Budgeted b, float conceptActivation, float linkActivation, MutableFloat conceptOverflow) {
            pendingActivations.add(() -> {
                super.conceptualize(c, b, conceptActivation, linkActivation, conceptOverflow);
            });
        }
    }



    protected @NotNull WorkerCore newCore(int id, int activeConcepts, int conceptsFirePerCycle, int termLinksPerConcept, int taskLinksPerConcept, PremiseEval matcher) {

        WorkerCore c = new WorkerCore(id, matcher, conceptWarm, conceptCold);
        c.concepts.setCapacity(activeConcepts);

        //TODO move these to a PremiseGenerator which supplies
        c.termlinksFiredPerFiredConcept.set(termLinksPerConcept);
        c.tasklinksFiredPerFiredConcept.set(taskLinksPerConcept);

        c.conceptsFiredPerCycle.set(conceptsFirePerCycle);

        return c;
    }

    @Override
    public final float conceptPriority(@NotNull Termed termed) {

        DefaultCore core = active.get(termed);
        if (core!=null) {
            BLink<Concept> c = core.concepts.get(termed);
            if (c != null)
                return c.priIfFiniteElseZero();
        }

        return 0;
    }


    @Nullable
    @Override
    public final Concept conceptualize(@NotNull Termed termed, @NotNull Budgeted b, float conceptActivation, float linkActivation, @Nullable MutableFloat conceptOverflow) {
        Concept c = concept(termed, true);
        if (c != null) {
            DefaultCore core = active.get(c);
            if (core == null) {
                //select a core at random
                core = cores[random.nextInt(cores.length)];
            }

            core.conceptualize(c, b, conceptActivation, linkActivation, conceptOverflow);
        }
        return c;
    }


    @NotNull
    @Override
    public NAR forEachConcept(@NotNull Consumer<Concept> recip) {
        for (WorkerCore core : this.cores) {
            core.concepts.forEachKey(recip);
        }
        return this;
    }


}

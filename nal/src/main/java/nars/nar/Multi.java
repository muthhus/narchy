package nars.nar;

import com.gs.collections.impl.map.mutable.ConcurrentHashMapUnsafe;
import nars.NAR;
import nars.Param;
import nars.concept.Concept;
import nars.index.CaffeineIndex;
import nars.index.TermIndex;
import nars.link.BLink;
import nars.nal.meta.PremiseEval;
import nars.nar.util.DefaultConceptBuilder;
import nars.nar.util.DefaultCore;
import nars.term.Termed;
import nars.time.Clock;
import nars.time.FrameClock;
import nars.util.data.random.XorShift128PlusRandom;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

/**
 * Multithreaded - Each core runs in its own thread
 */
public class Multi extends AbstractNAR {

    @NotNull
    final WorkerCore[] cores;
    @NotNull
    final Map<Concept, DefaultCore> active;
    //new ConcurrentHashMap();

    //final CyclicBarrier barrier;
    @NotNull
    final Semaphore demand, supply;

    @Deprecated
    public Multi(int cores) {
        this(cores, 1024, 1, 1, 3);
    }

    public Multi(int cores, int conceptsPerCore, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept) {
        this(cores, conceptsPerCore, conceptsFirePerCycle, taskLinksPerConcept, termLinksPerConcept, new XorShift128PlusRandom(1));
    }

    public Multi(int cores, int conceptsPerCore, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept, @NotNull Random random) {
        this(cores, conceptsPerCore, conceptsFirePerCycle, taskLinksPerConcept, termLinksPerConcept, random,
                new CaffeineIndex(new DefaultConceptBuilder(random), 500000, false),
                new FrameClock());
    }


    public Multi(int cores, int conceptsPerCore, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept, @NotNull Random random, @NotNull TermIndex index, @NotNull Clock clock) {
        super(clock,
                index,
                random,
                Param.DEFAULT_SELF);

        active = new ConcurrentHashMapUnsafe<>(cores * conceptsPerCore);
        //barrier = new CyclicBarrier(cores + 1);
        demand = new Semaphore(0);
        supply = new Semaphore(cores);

        this.cores = new WorkerCore[cores];
        for (int i = 0; i < cores; i++) {
            PremiseEval matcher = newMatcher();
            WorkerCore core = this.cores[i] = newCore(i,
                    conceptsPerCore,
                    conceptsFirePerCycle,
                    termLinksPerConcept, taskLinksPerConcept, matcher
            );

            eventReset.on(core::reset);
        }

        eventFrameStart.on(this::frame);

        runLater(this::initHigherNAL);


        //new QueryVariableExhaustiveResults(this.memory());

        /*
        the("memory_sharpen", new BagForgettingEnhancer(memory, core.active));
        */

    }


    protected final void frame(NAR n) {
        //try {
        int numCores = cores.length;
        int supplied = supply.availablePermits();
        int demanded = Math.min(numCores, numCores - supplied);
        if (demanded > 0) {
            demand.release(demanded);
        }

        //wait for at least one to finish
        int waitFor = Math.max(1, supplied);

        //System.out.println("demanded=" + demanded + ", waiting=" + waitFor);

        try {
            supply.acquire(waitFor);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


            //barrier.await();
        //} catch (Exception e) {
            //e.printStackTrace();
        //}
    }


    public class WorkerCore extends DefaultCore implements Runnable {

        private final Logger logger = LoggerFactory.getLogger(WorkerCore.class);

        @NotNull
        private final Thread thread;
        private boolean stopped;


        public WorkerCore(int n, @NotNull PremiseEval matcher) {
            super(Multi.this, matcher);
            this.thread = new Thread(this);
            thread.setName(nar.toString() + ".Worker" + n);
            thread.start();
        }

        @Override
        protected boolean awake(@NotNull Concept c) {
            if (Multi.this.active.putIfAbsent(c, this) == null) {
                super.awake(c);
                return true;
            }
            return false;
        }

        @Override
        protected void sleep(@NotNull Concept c) {
            if (Multi.this.active.remove(c) == c) {
                super.sleep(c);
            }
        }

        public void stop() {
            stopped = true;
        }

        @Override
        public final void run() {
            while (!stopped) {

                try {

                    demand.acquire();

                    try {
                        frame(Multi.this);
                    } catch (ConcurrentModificationException e) {
                        e.printStackTrace();
                        printWorkers();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    supply.release();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

//                try {
//                    barrier.await();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }

            }
        }

        @Override
        public String toString() {
            return thread.toString();
        }
    }

    private synchronized void printWorkers() {
        Map<Concept, WorkerCore> conceptCores = new HashMap();
        for (WorkerCore w : cores) {

            w.concepts.forEach(bc -> {
                Concept c = bc.get();
                WorkerCore e = conceptCores.put(c, w);
                if (e != null) {
                    System.err.println(c + " already in " + e + " and " + w);
                }
            });

        }
//        active.forEach((c,w) -> {
//            System.out.println(c + " " + w);
//        });


    }


    protected @NotNull WorkerCore newCore(int id, int activeConcepts, int conceptsFirePerCycle, int termLinksPerConcept, int taskLinksPerConcept, @NotNull PremiseEval matcher) {

        WorkerCore c = new WorkerCore(id, matcher);
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
        if (core != null) {
            BLink<Concept> c = core.concepts.get(termed);
            if (c != null)
                return c.priIfFiniteElseZero();
        }

        return 0;
    }


//    @Nullable
//    @Override
//    public final Concept activate(@NotNull Termed termed, @Nullable Activation conceptOverflow) {
//        Concept c = concept(termed, true);
//        if (c != null) {
////            {
////                int cc = 0;
////                int as = active.size();
////                for (WorkerCore w : cores) {
////                    cc += w.concepts.size();
////                }
////                if (Math.abs(cc - as) > 1) {
////                    System.err.println("active size fault: " + cc + " " + as);
////                }
////            }
//
//            DefaultCore w;
//            if ((w = active.get(c)) == null) {
//                w = assign(c);
//            }
//            w.conceptualize(c, b, conceptActivation, linkActivation, conceptOverflow);
//
//
//        }
//        return c;
//    }

    @NotNull
    public Multi.@NotNull WorkerCore assign(Concept c) {
        @NotNull WorkerCore[] cores = this.cores;
        return cores[random.nextInt(cores.length)];
    }


    @NotNull
    @Override
    public NAR forEachActiveConcept(@NotNull Consumer<Concept> recip) {
        for (WorkerCore core : this.cores) {
            core.concepts.forEachKey(recip);
        }
        return this;
    }

    @Override
    public synchronized void clear() {
        //TODO may require additional synchronization
        for (WorkerCore core : this.cores) {
            core.concepts.clear();
        }
    }
}

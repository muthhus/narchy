package nars.control;

import com.google.common.collect.Lists;
import jcog.AffinityExecutor;
import jcog.data.MutableIntRange;
import nars.Control;
import nars.NAR;
import nars.bag.Bag;
import nars.bag.CurveBag;
import nars.budget.Budget;
import nars.budget.BudgetMerge;
import nars.concept.Concept;
import nars.derive.DefaultDeriver;
import nars.derive.Deriver;
import nars.link.BLink;
import nars.premise.DefaultPremiseBuilder;
import nars.premise.PremiseBuilder;
import nars.term.Termed;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.System.out;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static nars.bag.CurveBag.power2BagCurve;

/**
 * Adaptive Logic and Neural Network
 * Original design by Tony Lofthouse
 */
public class AlannControl implements Control {

    private static final Logger logger = LoggerFactory.getLogger(AlannControl.class);

    public final List<AlannAgent> cores;
    final AtomicBoolean running = new AtomicBoolean(false);
    private final Executor coreExe;
    private final NAR nar;

    public AlannControl(NAR nar, int cores, int coreSize, int coreFires, int coreThreads) {

        this.nar = nar;

        coreExe =
                //Executors.newCachedThreadPool();
                //Executors.newFixedThreadPool(coreThreads);
                new AffinityExecutor("alann");

        this.cores = range(0, cores).mapToObj(i ->
                        new ConceptFirer(nar, coreSize, coreFires)
                //new TaskFirer(coreSize, coreFires)
        ).collect(toList());


        nar.onCycle(nn -> {
            if (!running.get()) {
                start(cores, coreThreads);
            }
        });
        nar.eventReset.on(nn -> {
            stop();
        });

    }

    @NotNull Runnable loop(@NotNull AlannAgent[] group) {
        return () -> {
            int n = group.length;
            int stopped;
            do {
                stopped = 0;
                for (AlannAgent g : group) {
                    try {
                        g.next();
                    } catch (Throwable t) {
                        logger.error("run: {}", t);
                    }
                    if (g.stopped)
                        stopped++;
                }
            } while (stopped < n);
        };
    }

    void start(float cores, int coreThreads) {
        synchronized (coreExe) {
            if (running.compareAndSet(false, true)) {
                List<List<AlannAgent>> ll = Lists.partition(this.cores, (int) Math.ceil(cores / coreThreads));

                for (List<AlannAgent> l : ll) {
                    int s = l.size();
                    if (s == 1) {
                        coreExe.execute(l.get(0)::loop);
                    } else {
                        coreExe.execute(loop(l.toArray(new AlannAgent[s])));
                    }

                }

            }
        }
    }

    public void stop() {
        synchronized (coreExe) {

            for (AlannAgent a : cores)
                a.stop();
            //coreExe.shutdownNow();

            running.set(false);
        }
    }

    /**
     * which core is handling a term
     */
    public AlannAgent core(@NotNull Termed term) {
        return cores.get(Math.abs(term.hashCode()) % cores.size());
    }


    @Override
    public void activate(Termed term, float priToAdd) {
        core(term).activate((Concept) term, ConceptBagControl.insertionBudget, priToAdd, null);
    }

    @Override
    public final float pri(@NotNull Termed concept) {
        float p = core(concept).pri(concept);
        return (p != p) ? Float.NaN : p;
    }


    @Override
    public Iterable<BLink<Concept>> conceptsActive() {
        //int s = cores.size();
        //int perCore = (int)Math.ceil((float)maxNodes / s);


        return () -> {
            Iterator[] coreBags = new Iterator[cores.size()];
            for (int i = 0; i < coreBags.length; i++) {
                coreBags[i] = cores.get(i).activeBag().iterator();
            }
            return IteratorUtils.zippingIterator(coreBags);
            //return new RoundRobinIterator<>(coreBags);
        };
    }


    public static abstract class AlannAgent {

        public volatile boolean stopped;

        public abstract void next();

        abstract public Iterator<Concept> active();

        abstract public Bag<Concept> activeBag();

        public void stop() {
            stopped = true;
        }

        public void loop() {
            while (!stopped) {
                try {
                    next();
                } catch (Throwable t) {
                    logger.error("run: {}", t);
                }
            }
        }


        public abstract float pri(@NotNull Termed concept);

        public abstract void activate(@NotNull Termed term, float priToAdd);

        public abstract void activate(Concept c, Budget b, float v, MutableFloat overflow);

    }


    public class ConceptFirer extends AlannAgent {

        final Deriver deriver = new DefaultDeriver();

        final PremiseBuilder premiser = new DefaultPremiseBuilder();

        public final Bag<Concept> active;

        int fireTaskLinks = 1;
        final MutableIntRange fireTermLinks = new MutableIntRange();

        public ConceptFirer(NAR nar, int capacity, int fireRate) {
            //new HijackBag<>(128, 3, blend, random);

            fireTermLinks.set(1, fireRate);

            active = new CurveBag<Concept>(capacity, new CurveBag.NormalizedSampler(power2BagCurve, nar.random), BudgetMerge.plusBlend, new ConcurrentHashMap<>(capacity)) {

                @Override
                public void onAdded(BLink<Concept> value) {
                    value.get().state(nar.concepts.conceptBuilder().awake(), nar);
                }

                @Override
                public void onRemoved(@NotNull BLink<Concept> value) {
                    value.get().state(nar.concepts.conceptBuilder().sleep(), nar);
                }
            };
        }

        @Override
        public float pri(@NotNull Termed concept) {
            Bag<Concept> cc = active;
            return cc.pri(concept, Float.NaN);
        }

        @Override
        public void activate(@NotNull Termed term, float priToAdd) {
            active.add(term, priToAdd);
        }

        @Override
        public void activate(Concept c, Budget b, float v, MutableFloat overflow) {
            active.put(c, b, v, overflow);
        }

        @Override
        public Iterator<Concept> active() {
            return IteratorUtils.transformedIterator(active.iterator(), BLink::get);
        }

        @Override
        public Bag<Concept> activeBag() {
            return active;
        }

        @Override
        public void next() {


            BLink<Concept> next = active.commit().sample();

            if (next == null) {
                //seed(seedRate);
                return;
            }


//                float conceptVisitCost = 1f - (1f / terms.size());
//                terms.mul(here, conceptVisitCost);

            Concept here = next.get();


            premiser.newPremiseMatrix(here, fireTaskLinks, fireTermLinks, here.tasklinks(), here.termlinks(), deriver, AlannControl.this.nar::input, AlannControl.this.nar
                    //input them within the current thread here
                    //this.tasklinks,
            );


        }


        void print() {
            //out.println("\nlocal:"); local.print();
            out.println("\nconcepts: " + active.size() + "/" + active.capacity());
            active.print();
//            out.println("\ntasklinks:");
//            tasklinks.print();
        }


    }

}

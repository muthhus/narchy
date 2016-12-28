package nars.nar;

import com.google.common.collect.Lists;
import jcog.AffinityExecutor;
import jcog.data.MutableIntRange;
import jcog.data.random.XorShift128PlusRandom;
import nars.Control;
import nars.NAR;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.budget.Budget;
import nars.budget.merge.BudgetMerge;
import nars.concept.Concept;
import nars.index.term.map.CaffeineIndex;
import nars.link.BLink;
import nars.nal.Deriver;
import nars.op.time.STMTemporalLinkage;
import nars.reason.*;
import nars.term.Termed;
import nars.time.Time;
import nars.util.exe.MultiThreadExecutioner;
import nars.util.exe.SynchronousExecutor;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.System.out;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static nars.bag.impl.CurveBag.power2BagCurve;

/**
 * ALANN Hybrid - experimental
 * Adaptive Logic and Neural Network
 * Original design by Tony Lofthouse
 */
public class Alann extends NAR {

    private static final Logger logger = LoggerFactory.getLogger(Alann.class);



    final static Deriver deriver = new DefaultDeriver();

    final static PremiseBuilder premiser = new DefaultPremiseBuilder();


    public abstract class AlannAgent {

        public volatile boolean stopped = false;

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


    class ConceptFirer extends AlannAgent {

        private BLink<Concept> current;

        public final Bag<Concept> active;

        int fireTaskLinks = 1;
        final MutableIntRange fireTermLinks = new MutableIntRange();

        public ConceptFirer(int capacity, int fireRate) {
            //new HijackBag<>(128, 3, blend, random);

            fireTermLinks.set(1, fireRate);

            active = new CurveBag<Concept>(capacity, new CurveBag.NormalizedSampler(power2BagCurve, random), BudgetMerge.plusBlend, new ConcurrentHashMap<>(capacity)) {

                @Override
                public void onAdded(BLink<Concept> value) {
                    value.get().state(concepts.conceptBuilder().awake(), Alann.this);
                }

                @Override
                public void onRemoved(@NotNull BLink<Concept> value) {
                    value.get().state(concepts.conceptBuilder().sleep(), Alann.this);
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
            active.activate(term, priToAdd);
        }

        @Override
        public void activate(Concept c, Budget b, float v, MutableFloat overflow) {
            active.put(c, b, v, overflow);
        }

        @Override
        public Iterator<Concept> active() {
            return IteratorUtils.transformedIterator( active.iterator(), BLink::get );
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


            premiser.newPremiseMatrix(here, fireTaskLinks, fireTermLinks, here.tasklinks(), here.termlinks(), deriver, Alann.this::input, Alann.this
                    //input them within the current thread here
                    //this.tasklinks,
            );


        }


        void print() {
            logger.info("at: {}", current);
            //out.println("\nlocal:"); local.print();
            out.println("\nconcepts: " + active.size() + "/" + active.capacity());
            active.print();
//            out.println("\ntasklinks:");
//            tasklinks.print();
        }


    }


    public Alann(@NotNull Time time, int cores, int coreSize, int coreFires) {
        this(time, cores, coreSize, coreFires, Runtime.getRuntime().availableProcessors() - 1, 1);
    }

    public Alann(@NotNull Time time, int cores, int coreSize, int coreFires, int coreThreads, int auxThreads) {
        super(time,

                //new TreeTermIndex.L1TreeIndex(new DefaultConceptBuilder(), 512 * 1024, 1024 * 32, 3),
                new CaffeineIndex(new DefaultConceptBuilder(), 128*1024, false, ForkJoinPool.commonPool()),

                new XorShift128PlusRandom(1),

                auxThreads == 1 ? new SynchronousExecutor() :
                        new MultiThreadExecutioner(auxThreads, 1024 * auxThreads).sync(true)
        );

        quaMin.setValue(BUDGET_EPSILON * 2f);

        int level = level();

        if (level >= 7) {

            STMTemporalLinkage stmLinkage = new STMTemporalLinkage(this, 2);

        }

        setControl(new AlannControl(this, cores, coreSize, coreFires, coreThreads));

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


    //    @Override
//    public final void activate(Iterable<ObjectFloatPair<Concept>> concepts, MutableFloat overflow) {
//
//
//        concepts.forEach((cv) -> {
//            Concept c = cv.getOne();
//            float scale = cv.getTwo();
//
//            float p = scale;
//            float q = 1f / (1f + c.complexity());
//
//            core(c).activate(c, $.b(p, q), 1f, overflow);
//        });
//    }


    public class AlannControl implements Control {

        public final List<AlannAgent> cores;
        final AtomicBoolean running = new AtomicBoolean(false);
        private final Executor coreExe;

        public AlannControl(NAR nar, int cores, int coreSize, int coreFires, int coreThreads) {

            coreExe =
                    //Executors.newCachedThreadPool();
                    //Executors.newFixedThreadPool(coreThreads);
                    new AffinityExecutor("alann" );

            this.cores = range(0, cores).mapToObj(i ->
                            new ConceptFirer(coreSize, coreFires)
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
            core(term).activate((Concept)term, ConceptBagReasoner.baseBudget, priToAdd, null);
        }

        @Override
        public final float pri(@NotNull Termed concept) {
            float p = core(concept).pri(concept);
            return (p != p) ? Float.NaN : p;
        }


        @Override
        public Iterable<? extends BLink<Concept>> conceptsActive() {
            //int s = cores.size();
            //int perCore = (int)Math.ceil((float)maxNodes / s);


            return () -> {
                Iterator[] coreBags = new Iterator[cores.size()];
                for (int i = 0; i <coreBags.length; i++) {
                    coreBags[i] = cores.get(i).activeBag().iterator();
                }
                return IteratorUtils.zippingIterator(coreBags);
                //return new RoundRobinIterator<>(coreBags);
            };
        }

    }



}


//public static class RoundRobinIterator<X> implements Iterator<X> {
//
//    //Define the iterator position in the overall list of iterators
//    //and the list of iterators itself.
//    private Iterator<Iterator<X>> iter;
//    private List<Iterator<X>> iterators;
//
//    //The next value to be returned.  If null, the next value has
//    //not been located yet.
//    private X nextValue;
//
//    public RoundRobinIterator(Iterator<X>[] iterators) {
//
//        //Gets an iterator over a list of iterators.
//        this.iterators = Lists.newArrayList(iterators);
//        iter = this.iterators.iterator();
//        this.nextValue = null;
//    }
//
//    @Override
//    public X next() {
//        if (!hasNext())
//            throw new NoSuchElementException();
//        X n = nextValue;
//        nextValue = null;
//        return n;
//    }
//
//    @Override
//    public boolean hasNext() {
//        return nextValue != null || setNext();
//    }
//
//    private boolean setNext() {
//
//        //If we've already found the next element, do nothing.
//        if (nextValue != null) return true;
//
//        //Loop until we determine the next element or that no elements remain.
//        while (true) {
//
//            //If we're at the end of the list of iterators, restart at the beginning, assuming
//            //any of the contained lists have remaining elements.
//            if (!iter.hasNext()) {
//                if (!iterators.isEmpty()) iter = iterators.iterator();
//                else return false;
//            }
//
//            //Get the next iterator from the list of iterators, assuming we're
//            //not at the last one already.
//            if (iter.hasNext()) {
//                Iterator<X> currentIter = iter.next();
//
//                //If the iterator we are positioned at has more elements left in its
//                //sub-list, then take the next element and return it.  If no elements remain
//                //then remove the iterator from the round-robin iterator list for good.
//                if (currentIter.hasNext()) {
//                    nextValue = currentIter.next();
//                    return true;
//                }
//                else {
//                    iter.remove();
//                }
//            }
//        }
//    }
//}
//    class TaskFirer extends AlannAgent {
//
//
//        public final Bag<Task> active;
//
//        final MutableIntRange fireTermLinks = new MutableIntRange();
//
//        public TaskFirer(int capacity, int fireRate) {
//            //new HijackBag<>(128, 3, blend, random);
//
//            fireTermLinks.set(1, fireRate);
//
//            active = new CurveBag<Task>(capacity, new CurveBag.NormalizedSampler(power2BagCurve, random), BudgetMerge.plusBlend, new ConcurrentHashMap<>(capacity)) {
//
//                @Override
//                public void onAdded(BLink<Task> value) {
//                    //value.get().state(concepts.conceptBuilder().awake(), Alann.this);
//                }
//
//                @Override
//                public void onRemoved(@NotNull BLink<Task> value) {
//                    //value.get().state(concepts.conceptBuilder().sleep(), Alann.this);
//                }
//            };
//        }
//
//        @Override
//        public float pri(@NotNull Termed concept) {
//            throw new UnsupportedOperationException("TODO");
//        }
//
//        @Override
//        public void activate(Concept c, Budget b, float v, MutableFloat overflow) {
//            BLink<Task> taskLink = c.tasklinks().sample();
//            if (taskLink!=null)
//                active.put(taskLink.get(), b, v, overflow);
//        }
//
//        @Override
//        public void activate(@NotNull Termed term, float priToAdd) {
//            BLink<Task> taskLink = concept(term).tasklinks().sample();
//            if (taskLink!=null)
//                active.put(taskLink.get(), taskLink, priToAdd, null); //HACK
//        }
//
//        @Override
//        public Iterator<Concept> active() {
//            Set<Concept> s = new LinkedHashSet();
//            active.forEachKey(t -> {
//                s.add(t.concept(Alann.this));
//            });
//            return s.iterator();
//        }
//
//        @Override
//        public Bag<Concept> activeBag() {
//            throw new UnsupportedOperationException();
//        }
//
//        @Override
//        public void next() {
//
//
//
//            BLink<Task> taskLink = active.commit().sample();
//            if (taskLink == null) {
//                //seed(seedRate);
//                return;
//            }
//
//            Task task = taskLink.get();
//
//
////                float conceptVisitCost = 1f - (1f / terms.size());
////                terms.mul(here, conceptVisitCost);
//
//            Concept concept = task.concept(Alann.this);
//
//
//            premiser.newPremiseMatrix(concept,
//                    fireTermLinks,
//                    Alann.this::input, //input them within the current thread here
//                    deriver,
//                    concept.termlinks(),
//                    Lists.newArrayList(taskLink),
//                    Alann.this
//            );
//
//        }
//
//
//    }

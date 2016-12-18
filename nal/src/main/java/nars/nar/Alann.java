package nars.nar;

import com.google.common.collect.Lists;
import jcog.Util;
import jcog.data.random.XorShift128PlusRandom;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.budget.merge.BudgetMerge;
import nars.concept.Concept;
import nars.index.term.tree.TreeTermIndex;
import nars.link.BLink;
import nars.nal.Deriver;
import nars.nar.exe.MultiThreadExecutioner;
import nars.nar.exe.SynchronousExecutor;
import nars.nar.util.DefaultConceptBuilder;
import nars.nar.util.PremiseMatrix;
import nars.op.time.STMTemporalLinkage;
import nars.term.Termed;
import nars.time.Time;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

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

    static private final BudgetMerge blend = BudgetMerge.plusBlend;

    public final List<GraphPremiseBuilder> cores;

    final static Deriver deriver = Default.newDefaultDeriver();

    final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService coreExe = null;

    public final class GraphPremiseBuilder {

        private BLink<Concept> current;

        public final Bag<Concept> terms =
                //new HijackBag<>(128, 3, blend, random);
                new CurveBag<Concept>(64, new CurveBag.NormalizedSampler(power2BagCurve, random), blend, new ConcurrentHashMap(64)) {

                    @Override
                    public void onAdded(BLink<Concept> value) {
                        value.get().state(concepts.conceptBuilder().awake(), Alann.this);
                    }

                    @Override
                    public void onRemoved(@NotNull BLink<Concept> value) {
                        value.get().state(concepts.conceptBuilder().sleep(), Alann.this);
                    }
                };

//        /**
//         * the tasklink bag is only modified and accessed by the core, locally, so it does not need to have a concurrent Map
//         */
//        public final Bag<Task> tasklinks =
//                //new HijackBag<>(128, 3, blend, random);
//                new CurveBag(64, new CurveBag.NormalizedSampler(power2BagCurve, random), blend, new HashMap(64));

        int fireTaskLinks = 1;
        int fireTermLinksMin = 1;
        int fireTermLinksMax = 4;


        public void loop() {
            while (true) {
                iterateSafe();
            }
        }

        public void iterateSafe() {
            try {
                iterate();
            } catch (Throwable t) {
                logger.error("run: {}", t);
            }
        }


        void iterate() {


            BLink<? extends Termed> next = go();

            if (next == null) {
                //seed(seedRate);
                go();
            }

            if (current != null) {

//                float conceptVisitCost = 1f - (1f / terms.size());
//                terms.mul(here, conceptVisitCost);

                Concept here = current.get();

                int fireTermLinks = (int) Math.ceil(Util.lerp(fireTermLinksMax, fireTermLinksMin, current.pri()));

                PremiseMatrix.run(here, Alann.this,
                        fireTaskLinks,
                        fireTermLinks,
                        Alann.this::input, //input them within the current thread here
                        deriver,
                        here.tasklinks(), //this.tasklinks,
                        here.termlinks()
                );
            }

        }


        @Nullable
        private BLink<Concept> go() {
            BLink<Concept> next = terms.commit().sample();
            if (next != null) {

                //Concept d = next.get(); //concept(next.get());


                //d.termlinks().commit().transfer(2, terms);
                //d.tasklinks().commit().copy(tasklinks, TASKLINK_COLLECTION_RATE);

                this.current = next;
            }
            return next;
        }

        void print() {
            logger.info("at: {}", current);
            //out.println("\nlocal:"); local.print();
            out.println("\nconcepts: " + terms.size() + "/" + terms.capacity());
            terms.print();
//            out.println("\ntasklinks:");
//            tasklinks.print();
        }


    }


    public Alann(@NotNull Time time, int cores) {
        this(time, cores, Runtime.getRuntime().availableProcessors()-1, 1);
    }

    public Alann(@NotNull Time time, int cores, int coreThreads, int auxThreads) {
        super(time,
                new TreeTermIndex.L1TreeIndex(new DefaultConceptBuilder(), 512 * 1024,
                        1024 * 32, 3),
                new XorShift128PlusRandom(1), Param.defaultSelf(),
                auxThreads == 1 ? new SynchronousExecutor() :
                        new MultiThreadExecutioner(auxThreads, 1024 * auxThreads).sync(true)
        );

        quaMin.setValue(BUDGET_EPSILON * 2f);

        int level = level();

        if (level >= 7) {
            initNAL7();

            if (level >= 8) {
                initNAL8();
            }

        }

        this.cores = range(0, cores).mapToObj(i -> new GraphPremiseBuilder()).collect(toList());

        runLater(() -> {
            start(coreThreads);
        });

    }


    public synchronized void start(int threads) {


        if (running.compareAndSet(false, true)) {
            List<List<GraphPremiseBuilder>> ll = Lists.partition(cores, (int) Math.ceil(((float) cores.size()) / threads));
            coreExe =
                    //Executors.newCachedThreadPool();
                    Executors.newFixedThreadPool(threads);
            for (List<GraphPremiseBuilder> l : ll) {
                int s = l.size();
                if (s == 1) {
                    coreExe.execute(l.get(0)::loop);
                } else {
                    coreExe.execute(loop(l.toArray(new GraphPremiseBuilder[s])));
                }

            }

        }
    }

    @NotNull Runnable loop(@NotNull GraphPremiseBuilder[] group) {
        return () -> {
            while (true) {
                for (GraphPremiseBuilder g : group) {
                    g.iterateSafe();
                }
            }
        };
    }

    @Override
    public Iterable<? extends BLink<Concept>> conceptsActive(int maxNodes) {
        //int s = cores.size();
        //int perCore = (int)Math.ceil((float)maxNodes / s);


        return () -> {
            Iterator[] coreBags = cores.stream().map(x -> x.terms.iterator()).toArray(Iterator[]::new);
            return IteratorUtils.zippingIterator(coreBags);
            //return new RoundRobinIterator<>(coreBags);
        };
    }

    public void print() {
        for (GraphPremiseBuilder core : cores) {
            core.print();
        }
        System.out.println();
    }


    /**
     * NAL7 plugins
     */
    protected void initNAL7() {

        STMTemporalLinkage stmLinkage = new STMTemporalLinkage(this, 2);

    }

    /* NAL8 plugins */
    protected void initNAL8() {

    }


    @Override
    public final Concept concept(@NotNull Termed term, float priToAdd) {
        Concept c = concept(term);
        if (c != null) {
            core(term).terms.add(term, priToAdd);
        }
        return c;
    }

    /**
     * which core is handling a term
     */
    public GraphPremiseBuilder core(@NotNull Termed term) {
        return cores.get(Math.abs(term.hashCode()) % cores.size());
    }

    @Override
    public final void activate(Iterable<ObjectFloatPair<Concept>> concepts, MutableFloat overflow) {


        concepts.forEach((cv) -> {
            Concept c = cv.getOne();
            float scale = cv.getTwo();

            float p = scale;
            float q = 1f / (1f + c.complexity());

            core(c).terms.put(c, $.b(p, q), 1f, overflow);
        });
    }

    @Override
    public final float priority(@NotNull Termed concept, float valueIfInactive) {
        //TODO impl
        Bag<Concept> cc = core(concept).terms;
        return cc.pri(concept, Float.NaN);
    }


    @NotNull
    @Override
    public NAR forEachActiveConcept(@NotNull Consumer<Concept> recip) {
        conceptsActive(Integer.MAX_VALUE).forEach(x -> recip.accept(x.get()));
        return this;
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

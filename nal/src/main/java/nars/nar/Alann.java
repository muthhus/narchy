package nars.nar;

import jcog.data.random.XorShift128PlusRandom;
import nars.NAR;
import nars.index.term.map.CaffeineIndex;
import nars.op.time.STMTemporalLinkage;
import nars.reason.concept.DefaultConceptBuilder;
import nars.reason.control.AlannControl;
import nars.time.Time;
import nars.util.exe.MultiThreadExecutioner;
import nars.util.exe.SynchronousExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ForkJoinPool;

/**
 * ALANN Hybrid - experimental
 * Adaptive Logic and Neural Network
 * Original design by Tony Lofthouse
 */
public class Alann extends NAR {



    public Alann(@NotNull Time time, int cores, int coreSize, int coreFires) {
        this(time, cores, coreSize, coreFires, Runtime.getRuntime().availableProcessors() - 1, 1);
    }

    public Alann(@NotNull Time time, int cores, int coreSize, int coreFires, int coreThreads, int auxThreads) {
        super(time,

                //new TreeTermIndex.L1TreeIndex(new DefaultConceptBuilder(), 512 * 1024, 1024 * 32, 3),
                new CaffeineIndex(new DefaultConceptBuilder(), 128 * 1024, false, ForkJoinPool.commonPool()),

                new XorShift128PlusRandom(1),

                auxThreads == 1 ? new SynchronousExecutor() :
                        new MultiThreadExecutioner(auxThreads, 1024 * auxThreads).sync(true)
        );


        int level = level();

        if (level >= 7) {

            STMTemporalLinkage stmLinkage = new STMTemporalLinkage(this, 2);

        }

        setControl(new AlannControl(this, cores, coreSize, coreFires, coreThreads));

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


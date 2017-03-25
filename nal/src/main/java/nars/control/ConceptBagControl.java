package nars.control;

import jcog.bag.Bag;
import jcog.bag.PLink;
import jcog.bag.RawPLink;
import jcog.data.FloatParam;
import jcog.data.MutableIntRange;
import nars.Control;
import nars.NAR;
import nars.concept.Concept;
import nars.premise.MatrixPremiseBuilder;
import nars.task.DerivedTask;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * The default deterministic memory cycle implementation that is currently used as a standard
 * for development and testing.
 *
 * multithreading granularity at the concept (outermost loop)
 */
public abstract class ConceptBagControl implements Control, Consumer<DerivedTask> {

    static final Logger logger = LoggerFactory.getLogger(ConceptBagControl.class);

    final MatrixPremiseBuilder premiser;

//    /** this will be scaled by the input priority factor for each concept */
//    public static final ROBudget insertionBudget = new ROBudget(1f, 0.5f);


    /**
     * concepts active in this cycle
     */
    @NotNull
    public final Bag<Concept,PLink<Concept>> active;

    @Deprecated
    public final transient @NotNull NAR nar;

    /** distinct from the NAR's */
    public final FloatParam activationRate = new FloatParam(1f);

    public final AtomicBoolean clear = new AtomicBoolean(false);

    protected float currentActivationRate = 1f;



    //public final HitMissMeter meter = new HitMissMeter(ConceptBagControl.class.getSimpleName());


    public ConceptBagControl(@NotNull NAR nar, @NotNull Bag<Concept, PLink<Concept>> conceptBag, MatrixPremiseBuilder premiseBuilder) {

        this.nar = nar;

        this.premiser = premiseBuilder;

        this.active = conceptBag;

        nar.onCycle(()->{

            currentActivationRate = activationRate.floatValue();

            //while clear is enabled, keep active clear
            if (clear.get()) {

                active.clear();

            } else {
                active.commit();

                try {
                    cycle();
                } catch (Exception e) {
                    logger.error("cycle {}", e);
                }
            }


        });

        nar.onReset((n)->{
            active.clear();
        });

    }

    protected abstract void cycle();

    //    /** called when a concept is displaced from the concept bag */
//    protected void sleep(@NotNull Concept c) {
//        NAR n = this.nar;
//
//        n.setState(c, conceptBuilder.sleep());
//
//        n.emotion.alert(1f / active.size());
//    }



    @Override
    public void activate(/*Concept*/ Concept concept, float priToAdd) {
        active.put(new RawPLink(concept, priToAdd), currentActivationRate, null);
    }


    @Override
    public float pri(@NotNull Termed concept) {
        PLink c = active.get(concept);
        return (c != null) ? c.priSafe(0) : Float.NaN;
    }


    @Override
    public Iterable<PLink<Concept>> conceptsActive() {
        return active;
    }


    public class PremiseMatrix implements Consumer<NAR> {
        private final int _tasklinks;
        private final int batchSize;
        private final MutableIntRange _termlinks;

        public PremiseMatrix(int batchSize, int _tasklinks, MutableIntRange _termlinks) {
            this.batchSize = batchSize;
            this._tasklinks = _tasklinks;
            this._termlinks = _termlinks;
        }

        @Override
        public void accept(NAR nar) {
            active.sample(batchSize, c -> {
                premiser.newPremiseMatrix(c.get(),
                        _tasklinks, _termlinks,
                        ConceptBagControl.this, //input them within the current thread here
                        nar
                );
                return true;
            });
        }

    }

//    static final class BudgetSavings extends RawBudget {
//        public final long savedAt;
//
//        public BudgetSavings(@NotNull Budget value, long savedAt) {
//            super(value);
//            this.savedAt = savedAt;
//        }
//
//    }


//    /** extends CurveBag to invoke entrance/exit event handler lambda */
//    public final class ConceptBag extends BagAdapter<Concept> {
//
//
//        public ConceptBag( @NotNull Bag<Concept,BLink<Concept>> bag) {
//            super(bag);
//        }
//
////        final AtomicBoolean busyPut = new AtomicBoolean(false);
////        final ArrayBlockingQueue<Object[]> pending =
////                //ArrayBlockingQueue
////                new ArrayBlockingQueue(8);
////
////        @Override
////        public void put(ObjectFloatHashMap<? extends Concept> values, Budgeted in, float scale, MutableFloat overflow) {
////            if (busyPut.compareAndSet(false, true)) {
////
////                //synchronized (items) {
////                    super.put(values, in, scale, overflow);
////
////                    //process any pending that arrived
////                    Object[] p;
////                    while ((p = pending.poll()) != null) {
////                        super.put((ObjectFloatHashMap) p[0], (Budgeted) p[1], (float) p[2], (MutableFloat) p[3]);
////                    }
////                //}
////
////                busyPut.set(false);
////            } else {
////                try {
////                    values.compact();
////                    pending.put(new Object[] { values, in, scale, overflow });
////                } catch (InterruptedException e) {
////                    e.printStackTrace();
////                }
////            }
////        }
//
//
//
//
//        /** called when a concept enters the concept bag
//         * */
//        @Override
//        public final void onAdded(@NotNull BLink<Concept> v) {
//            Concept c = v.get();
//
//            //float forgetPeriod = getForgetPeriod();
//
//            nar.setState(c, conceptBuilder.awake());
//            /*BudgetSavings existing = c.get(this);
//            if (existing!=null) {
//                if (existing.isDeleted())
//                    throw new UnsupportedOperationException();
//
//                // cost at least 1 time unit. if zero time units are allowed then concepts could theoreticaly avoid the normal forgetting while being asleep during the same time frame
//                float forgetScale = 1f - Math.max(1,(now - existing.savedAt)) / forgetPeriod;
//                if (forgetScale > 0) {
//                    BudgetMerge.plusBlend.apply(v, existing, forgetScale);
//                    c.put(this, null);
//                }
//            }*/
//
//        }
//
//        /*private float getForgetPeriod() {
//            return (float) Math.ceil(1 + Math.sqrt(capacity()));
//        }*/
//
//
////        @Override
////        public final void onRemoved(@NotNull BLink<Concept> v) {
////            super.onRemoved(v);
////
////            Concept c  = v.get();
////            sleep(c);
////
////                /*if (value.priIfFiniteElseNeg1() > Param.BUDGET_EPSILON) {
////                    BudgetSavings s = new BudgetSavings(value, now);
////                    if (!s.isDeleted()) {
////                        c.put(this, s);
////                    }
////                }*/
////        }
////
////        @Override
////        public @Nullable BLink<Concept> remove(@NotNull Concept x) {
////            BLink<Concept> r = super.remove(x);
////            if (r!=null) {
////                sleep(x);
////            }
////            return r;
////        }
////
//
//    }


//    public void conceptualize(@NotNull Concept c, @NotNull Budgeted b, float conceptActivation, float linkActivation, NAR.Activation activation) {
//
//        concepts.put(c, b, conceptActivation, activation.overflow);
//        //if (b.isDeleted())
//            //return;
//            //throw new RuntimeException("Concept rejected: " + b);
//        if (linkActivation > 0)
//            c.link(b, linkActivation, nar, activation);
//    }


    //try to implement some other way, this is here because of serializability

}

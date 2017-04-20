package nars.control;

import jcog.bag.Bag;
import jcog.data.FloatParam;
import jcog.pri.PLink;
import jcog.pri.RawPLink;
import nars.Focus;
import nars.NAR;
import nars.term.Termed;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.jetbrains.annotations.NotNull;

/**
 * The default deterministic memory cycle implementation that is currently used as a standard
 * for development and testing.
 *
 * multithreading granularity at the concept (outermost loop)
 */
public class ConceptBagFocus implements Focus {

    public final FloatParam activationRate = new FloatParam(1f);

    /**
     * concepts active in this cycle
     */
    @NotNull
    public final Bag<Termed,PLink<Termed>> active;

    @Deprecated
    public final transient @NotNull NAR nar;


    public ConceptBagFocus(@NotNull NAR nar, @NotNull Bag<Termed, PLink<Termed>> conceptBag) {

        this.nar = nar;

        this.active = conceptBag;
    }



    @Override
    public PLink<Termed> activate(/*Concept*/ Termed concept, float priToAdd) {
        return active.put(new RawPLink<>(concept, priToAdd * activationRate.floatValue() ));
    }

    @Override
    public float pri(@NotNull Termed concept) {
        PLink c = active.get(concept);
        return (c != null) ? c.priSafe(0) : Float.NaN;
    }


    @Override
    public Iterable/* <PLink<Concept>> */ concepts() {
        active.commit();
        return active; //HACK here it is purposefully being ambiguous about the type whether it is Termed or more specifically Concept
    }

    @Override
    public void sample(int max, IntObjectToIntFunction/*<? super PLink<Concept>>*/ c) {
        active.sample(max, c);
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

package nars.nar.core;

import jcog.data.MutableIntRange;
import jcog.data.MutableInteger;
import jcog.data.Range;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.budget.Budget;
import nars.budget.ROBudget;
import nars.budget.merge.BudgetMerge;
import nars.concept.Concept;
import nars.concept.util.ConceptBuilder;
import nars.link.BLink;
import nars.nal.Deriver;
import nars.nar.util.DefaultConceptBuilder;
import nars.nar.util.PremiseMatrix;
import nars.term.Termed;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The default deterministic memory cycle implementation that is currently used as a standard
 * for development and testing.
 *
 * multithreading granularity at the concept (outermost loop)
 */
public class ConceptBagCycle {


    private static final Logger logger = LoggerFactory.getLogger(ConceptBagCycle.class);

    final Deriver deriver;

    /**
     * concepts active in this cycle
     */
    @NotNull
    public final Bag<Concept> active;

    @Deprecated
    public final transient @NotNull NAR nar;

    /**
     * How many concepts to fire each cycle; measures degree of parallelism in each cycle
     */
    @Range(min = 0, max = 64, unit = "Concept")
    public final @NotNull MutableInteger conceptsFiredPerCycle;


    /** size of each sampled concept batch that adds up to conceptsFiredPerCycle.
     *  reducing this value should provide finer-grained / higher-precision concept selection
     *  since results between batches can affect the next one.
     */
    public final @NotNull MutableInteger conceptsFiredPerBatch;


    @Range(min = 0, max = 16, unit = "TaskLink") //TODO use float percentage
    public final MutableInteger tasklinksFiredPerFiredConcept = new MutableInteger(1);

    //@Range(min = 0, max = 16, unit = "TermLink")
    public final MutableIntRange termlinksFiredPerFiredConcept = new MutableIntRange(1, 1);



    @NotNull
    private final ConceptBuilder conceptBuilder;

    final AtomicBoolean busy = new AtomicBoolean(false);

//    private Comparator<? super BLink<Concept>> sortConceptLinks = (a, b) -> {
//        Concept A = a.get();
//        Concept B = b.get();
//        String as = A!=null ? A.term
//    };

//    private static final Logger logger = LoggerFactory.getLogger(AbstractCore.class);

    //private final CapacityLinkedHashMap<Premise,Premise> recent = new CapacityLinkedHashMap<>(256);
    //long novel=0, total=0;

    public ConceptBagCycle(@NotNull NAR nar, Deriver deriver, int initialCapacity) {

        this.nar = nar;

        this.deriver = deriver;
        this.conceptsFiredPerCycle = new MutableInteger(1);
        this.conceptsFiredPerBatch = new MutableInteger(Param.CONCEPT_FIRE_BATCH_SIZE);
        this.conceptBuilder = nar.concepts.conceptBuilder();

        this.active = new BagIndexAdapter(initialCapacity, ((DefaultConceptBuilder) conceptBuilder).defaultCurveSampler);


        //nar.onFrame(this);
        nar.onCycle((n) -> {
            if (busy.compareAndSet(false, true)) {

                //updae concept bag
                active.commit();

                float load = nar.exe.load();

                int cpf = Math.round(conceptsFiredPerCycle.floatValue() * (1f - load));
                if (cpf > 0) {

                    int cbs = conceptsFiredPerBatch.intValue();

                    //logger.info("firing {} concepts (exe load={})", cpf, load);

                    while (cpf > 0) {

                        int batchSize = Math.min(cpf, cbs);
                        cpf -= cbs;

                        this.nar.runLater(() -> {

                            List<BLink<Concept>> toFire = $.newArrayList(batchSize);
                            toFire.clear();
                            active.sample(batchSize, toFire::add);


                            int _tasklinks = tasklinksFiredPerFiredConcept.intValue();

                            for (int i = 0, toFireSize = toFire.size(); i < toFireSize; i++) {
                                BLink<Concept> cl = toFire.get(i);
                                int _termlinks = termlinksFiredPerFiredConcept.lerp(cl.pri());
                                if (_termlinks > 0) {

                                    Concept c = cl.get();

                                    PremiseMatrix.run(c, this.nar,
                                            _tasklinks, _termlinks,
                                            this.nar::input, //input them within the current thread here
                                            deriver
                                    );
                                }
                            }
                        });


                    }
                }

                busy.set(false);
            }

        });
        nar.eventReset.on((n)->active.clear());

    }

    /** called when a concept is displaced from the concept bag */
    protected void sleep(@NotNull Concept c) {
        NAR n = this.nar;

        n.policy(c, conceptBuilder.sleep());

        n.emotion.alert(1f / active.size());
    }


    public static final Budget baseConceptBudget = new ROBudget(1f, 0.5f);

    public void priorityAdd(Iterable<ObjectFloatPair<Concept>> activations, MutableFloat overflow) {
        this.active.put(activations, baseConceptBudget, overflow);
    }

    public float priority(@NotNull Termed concept, float valueIfInactive) {
        BLink c = active.get(concept);
        if (c == null) return valueIfInactive;
        float p = c.priActive(valueIfInactive);

        return c != null ? c.pri() : Float.NaN;
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


    /** extends CurveBag to invoke entrance/exit event handler lambda */
    public final class BagIndexAdapter extends CurveBag<Concept> {


        public BagIndexAdapter(int capacity, @NotNull CurveSampler sampler) {
            super(capacity, sampler, BudgetMerge.plusBlend,
                    //new ConcurrentHashMap<>(capacity)
                    nar.exe.concurrent() ?  new java.util.concurrent.ConcurrentHashMap<>(capacity) : new HashMap(capacity)
                    //new NonBlockingHashMap<>(capacity)
            );
        }

//        final AtomicBoolean busyPut = new AtomicBoolean(false);
//        final ArrayBlockingQueue<Object[]> pending =
//                //ArrayBlockingQueue
//                new ArrayBlockingQueue(8);
//
//        @Override
//        public void put(ObjectFloatHashMap<? extends Concept> values, Budgeted in, float scale, MutableFloat overflow) {
//            if (busyPut.compareAndSet(false, true)) {
//
//                //synchronized (items) {
//                    super.put(values, in, scale, overflow);
//
//                    //process any pending that arrived
//                    Object[] p;
//                    while ((p = pending.poll()) != null) {
//                        super.put((ObjectFloatHashMap) p[0], (Budgeted) p[1], (float) p[2], (MutableFloat) p[3]);
//                    }
//                //}
//
//                busyPut.set(false);
//            } else {
//                try {
//                    values.compact();
//                    pending.put(new Object[] { values, in, scale, overflow });
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }

        @Override
        public void clear() {
            forEach((BLink<Concept> v) -> {

                Concept c = v.get();
                sleep(c);
                //c.put(this, null);

                //TODO clear BudgetSavings in the meta maps
            }); //HACK allow opportunity to process removals
            super.clear();
        }



        /** called when a concept enters the concept bag
         * */
        @Override
        public final void onAdded(@NotNull BLink<Concept> v) {
            Concept c = v.get();

            //float forgetPeriod = getForgetPeriod();

            nar.policy(c, conceptBuilder.awake());
            /*BudgetSavings existing = c.get(this);
            if (existing!=null) {
                if (existing.isDeleted())
                    throw new UnsupportedOperationException();

                // cost at least 1 time unit. if zero time units are allowed then concepts could theoreticaly avoid the normal forgetting while being asleep during the same time frame
                float forgetScale = 1f - Math.max(1,(now - existing.savedAt)) / forgetPeriod;
                if (forgetScale > 0) {
                    BudgetMerge.plusBlend.apply(v, existing, forgetScale);
                    c.put(this, null);
                }
            }*/

        }

        /*private float getForgetPeriod() {
            return (float) Math.ceil(1 + Math.sqrt(capacity()));
        }*/


        @Override
        public final void onRemoved(@NotNull BLink<Concept> value) {
                Concept c  = value.get();
                sleep(c);

                /*if (value.priIfFiniteElseNeg1() > Param.BUDGET_EPSILON) {
                    BudgetSavings s = new BudgetSavings(value, now);
                    if (!s.isDeleted()) {
                        c.put(this, s);
                    }
                }*/
        }

        @Override
        public @Nullable BLink<Concept> remove(@NotNull Concept x) {
            BLink<Concept> r = super.remove(x);
            if (r!=null) {
                sleep(x);
            }
            return r;
        }


    }


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

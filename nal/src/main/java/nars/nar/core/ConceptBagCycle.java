package nars.nar.core;

import nars.$;
import nars.NAR;
import nars.Param;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.budget.Budget;
import nars.budget.Budgeted;
import nars.budget.RawBudget;
import nars.budget.merge.BudgetMerge;
import nars.concept.Concept;
import nars.concept.util.ConceptBuilder;
import nars.link.BLink;
import nars.nal.Deriver;
import nars.nar.util.DefaultConceptBuilder;
import nars.nar.util.PremiseMatrix;
import nars.util.data.MutableInteger;
import nars.util.data.Range;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * The default deterministic memory cycle implementation that is currently used as a standard
 * for development and testing.
 *
 * multithreading granularity at the concept (outermost loop)
 */
public class ConceptBagCycle implements Consumer<NAR> {


    private static final Logger logger = LoggerFactory.getLogger(ConceptBagCycle.class);

    final static Deriver deriver = Deriver.getDefaultDeriver();



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


    @Range(min = 0, max = 16, unit = "TaskLink") //TODO use float percentage
    public final MutableInteger tasklinksFiredPerFiredConcept = new MutableInteger(1);

    @Range(min = 0, max = 16, unit = "TermLink")
    public final MutableInteger termlinksFiredPerFiredConcept = new MutableInteger(1);



    @NotNull
    private final ConceptBuilder conceptBuilder;

    //cached value for use in the next firing
    private int _tasklinks, _termlinks;
    private long now;


//    private Comparator<? super BLink<Concept>> sortConceptLinks = (a, b) -> {
//        Concept A = a.get();
//        Concept B = b.get();
//        String as = A!=null ? A.term
//    };

//    private static final Logger logger = LoggerFactory.getLogger(AbstractCore.class);

    //private final CapacityLinkedHashMap<Premise,Premise> recent = new CapacityLinkedHashMap<>(256);
    //long novel=0, total=0;

    public ConceptBagCycle(@NotNull NAR nar, int initialCapacity) {

        this.nar = nar;

        this.conceptsFiredPerCycle = new MutableInteger(1);
        this.conceptBuilder = nar.concepts.conceptBuilder();

        this.active = new BagIndexAdapter(initialCapacity, ((DefaultConceptBuilder) conceptBuilder).defaultCurveSampler);


        nar.onFrame(this);
        nar.eventReset.on((n)->active.clear());

    }

    /** called when a concept is displaced from the concept bag */
    protected void sleep(@NotNull Concept c) {
        NAR n = this.nar;

        n.policy(c, conceptBuilder.sleep(), now);

        n.emotion.alert(1f / active.size());
    }




    final AtomicBoolean busy = new AtomicBoolean(false);

    /** called each frame */
    @Override public void accept(@NotNull NAR nar) {

        if (busy.compareAndSet(false, true)) {
            now = nar.time();

            //updae concept bag
            active.commit();

            float load = nar.exe.load();

            int cpf = Math.round(conceptsFiredPerCycle.floatValue() * (1f - load));
            if (cpf > 0) {


                //logger.info("firing {} concepts (exe load={})", cpf, load);

                this._tasklinks = tasklinksFiredPerFiredConcept.intValue();
                this._termlinks = termlinksFiredPerFiredConcept.intValue();

                List<BLink<Concept>> toFire = $.newArrayList(cpf);
                active.sample(cpf, toFire::add);

                //toFire.sort(sortConceptLinks);

                this.nar.runLater(toFire, bc -> {
                    Concept c = bc.get();
                    if (c != null) {
                        PremiseMatrix.run(c, this.nar,
                                _tasklinks, _termlinks,
                                this.nar::input, //input them within the current thread here
                                deriver
                        );
                    }
                }, 1);

            }

            busy.set(false);
        }

    }

    public void activate(@NotNull ObjectFloatHashMap<Concept> activations, @NotNull Budgeted in, float activation, MutableFloat overflow) {
        this.active.put(activations, in, activation, overflow);
    }

    static final class BudgetSavings extends RawBudget {
        public final long savedAt;

        public BudgetSavings(@NotNull Budget value, long savedAt) {
            super(value);
            this.savedAt = savedAt;
        }

    }


    /** extends CurveBag to invoke entrance/exit event handler lambda */
    public final class BagIndexAdapter extends CurveBag<Concept> {


        public BagIndexAdapter(int capacity, @NotNull CurveSampler sampler) {
            super(capacity, sampler, BudgetMerge.plusBlend,
                    //new ConcurrentHashMap<>(capacity)
                    nar.exe.concurrent() ?  new java.util.concurrent.ConcurrentHashMap<>(capacity) : new HashMap(capacity)
                    //new NonBlockingHashMap<>(capacity)
            );
        }



        @Override
        public void clear() {
            forEach((BLink<Concept> v) -> {
                if (v!=null) {
                    Concept c = v.get();
                    if (c!=null) {
                        sleep(c);
                        c.put(this, null);
                    }
                }
                //TODO clear BudgetSavings in the meta maps
            }); //HACK allow opportunity to process removals
            super.clear();
        }



        /** called when a concept enters the concept bag
         * */
        @Override
        public final void onAdded(@NotNull BLink<Concept> v) {
            Concept c = v.get();
            if (c == null)
                throw new NullPointerException();

            float forgetPeriod = getForgetPeriod();

            nar.policy(c, conceptBuilder.awake(), now);
            BudgetSavings existing = c.get(this);
            if (existing!=null) {
                if (existing.isDeleted())
                    throw new UnsupportedOperationException();

                /** cost at least 1 time unit. if zero time units are allowed then concepts could theoreticaly avoid the normal forgetting while being asleep during the same time frame */
                float forgetScale = 1f - Math.max(1,(now - existing.savedAt)) / forgetPeriod;
                if (forgetScale > 0) {
                    BudgetMerge.plusBlend.apply(v, existing, forgetScale);
                    c.put(this, null);
                }
            }

        }

        private float getForgetPeriod() {
            return (float) Math.ceil(1 + Math.sqrt(capacity()));
        }


        @Override
        public final void onRemoved(@Nullable BLink<Concept> value) {
            if (value!=null) {
                Concept c  = value.get();
                sleep(c);

                if (value.priIfFiniteElseNeg1() > Param.BUDGET_EPSILON) {
                    BudgetSavings s = new BudgetSavings(value, now);
                    if (!s.isDeleted()) {
                        c.put(this, s);
                    }
                }
            }
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

package nars.control;

import jcog.data.MutableIntRange;
import jcog.data.MutableInteger;
import jcog.data.Range;
import jcog.meter.event.HitMissMeter;
import nars.$;
import nars.Control;
import nars.NAR;
import nars.Param;
import jcog.bag.Bag;
import nars.bag.BagAdapter;
import nars.budget.BLink;
import nars.budget.BudgetMerge;
import nars.budget.RawBLink;
import nars.concept.Concept;
import nars.conceptualize.ConceptBuilder;
import nars.derive.Deriver;
import nars.premise.MatrixPremiseBuilder;
import nars.task.DerivedTask;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * The default deterministic memory cycle implementation that is currently used as a standard
 * for development and testing.
 *
 * multithreading granularity at the concept (outermost loop)
 */
public class ConceptBagControl implements Control, Consumer<DerivedTask> {


    private static final Logger logger = LoggerFactory.getLogger(ConceptBagControl.class);

    public static final BudgetMerge CONCEPT_BAG_BLEND = BudgetMerge.orBlend;

    final Deriver deriver;

    final MatrixPremiseBuilder premiser = new MatrixPremiseBuilder();

//    /** this will be scaled by the input priority factor for each concept */
//    public static final ROBudget insertionBudget = new ROBudget(1f, 0.5f);


    /**
     * concepts active in this cycle
     */
    @NotNull
    public final Bag<Concept,BLink<Concept>> active;

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

    @NotNull private final ConceptBuilder conceptBuilder;

    final AtomicBoolean busy = new AtomicBoolean(false);

    public final HitMissMeter meter = new HitMissMeter(ConceptBagControl.class.getSimpleName());

//    private Comparator<? super BLink<Concept>> sortConceptLinks = (a, b) -> {
//        Concept A = a.get();
//        Concept B = b.get();
//        String as = A!=null ? A.term
//    };

//    private static final Logger logger = LoggerFactory.getLogger(AbstractCore.class);

    //private final CapacityLinkedHashMap<Premise,Premise> recent = new CapacityLinkedHashMap<>(256);
    //long novel=0, total=0;

    public ConceptBagControl(@NotNull NAR nar, @NotNull Deriver deriver, @NotNull Bag<Concept,BLink<Concept>> conceptBag) {

        this.nar = nar;

        this.deriver = deriver;
        this.conceptsFiredPerCycle = new MutableInteger(1);
        this.conceptsFiredPerBatch = new MutableInteger(Param.CONCEPT_FIRE_BATCH_SIZE);
        this.conceptBuilder = nar.concepts.conceptBuilder();

        this.active = new ConceptBag( conceptBag );


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
                                premiser.newPremiseMatrix(toFire.get(i).get(), this.nar,
                                        _tasklinks, termlinksFiredPerFiredConcept,
                                        this, //input them within the current thread here
                                        deriver
                                );
                            }
                        });


                    }
                }

                busy.set(false);
            }

        });
        nar.onReset((n)->active.clear());

    }

    @Override
    public void accept(DerivedTask d) {
        if (nar.input(d)!=null) {
            meter.hit();
        } else {
            meter.miss();
        }
    }

//    /** called when a concept is displaced from the concept bag */
//    protected void sleep(@NotNull Concept c) {
//        NAR n = this.nar;
//
//        n.setState(c, conceptBuilder.sleep());
//
//        n.emotion.alert(1f / active.size());
//    }



    @Override
    public void activate(Termed term, float priToAdd) {
        active.put(new RawBLink(term, priToAdd, 0.5f), 1f, null);
    }


    @Override
    public float pri(@NotNull Termed concept) {
        BLink c = active.get(concept);
        return (c != null) ? c.pri() : Float.NaN;
    }


    @Override
    public Iterable<BLink<Concept>> conceptsActive() {
        return active;
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
    public final class ConceptBag extends BagAdapter<Concept> {


        public ConceptBag( @NotNull Bag<Concept,BLink<Concept>> bag) {
            super(bag);
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




        /** called when a concept enters the concept bag
         * */
        @Override
        public final void onAdded(@NotNull BLink<Concept> v) {
            Concept c = v.get();

            //float forgetPeriod = getForgetPeriod();

            nar.setState(c, conceptBuilder.awake());
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


//        @Override
//        public final void onRemoved(@NotNull BLink<Concept> v) {
//            super.onRemoved(v);
//
//            Concept c  = v.get();
//            sleep(c);
//
//                /*if (value.priIfFiniteElseNeg1() > Param.BUDGET_EPSILON) {
//                    BudgetSavings s = new BudgetSavings(value, now);
//                    if (!s.isDeleted()) {
//                        c.put(this, s);
//                    }
//                }*/
//        }
//
//        @Override
//        public @Nullable BLink<Concept> remove(@NotNull Concept x) {
//            BLink<Concept> r = super.remove(x);
//            if (r!=null) {
//                sleep(x);
//            }
//            return r;
//        }
//

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

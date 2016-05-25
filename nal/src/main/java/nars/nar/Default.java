package nars.nar;

import javassist.scopedpool.SoftValueHashMap;
import nars.Global;
import nars.Memory;
import nars.NAR;
import nars.bag.BLink;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.budget.Budgeted;
import nars.budget.forget.BudgetForget;
import nars.budget.forget.Forget;
import nars.budget.merge.BudgetMerge;
import nars.budget.policy.DefaultConceptBudgeting;
import nars.concept.Concept;
import nars.concept.DefaultConceptBuilder;
import nars.concept.PremiseGenerator;
import nars.data.Range;
import nars.nal.Deriver;
import nars.nal.meta.PremiseEval;
import nars.task.Task;
import nars.term.TermIndex;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.index.GroupedMapIndex;
import nars.term.index.SimpleMapIndex2;
import nars.time.Clock;
import nars.time.FrameClock;
import nars.util.data.MutableInteger;
import nars.util.data.random.XorShift128PlusRandom;
import nars.util.event.Active;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * Various extensions enabled
 */
public class Default extends AbstractNAR {

    //private static final Logger logger = LoggerFactory.getLogger(Default.class);

    @NotNull
    public final DefaultCycle core;

    @NotNull
    public final DefaultPremiseGenerator premiser;

//    @Range(min = 0.01f, max = 8, unit = "Duration")
//    public final MutableFloat termLinkRemembering;
//
//    @Range(min = 0.01f, max = 8, unit = "Duration")
//    public final MutableFloat taskLinkRemembering;

    @Deprecated
    public Default() {
        this(1024, 1, 1, 3);
    }

//    public Default(int numConcepts,
//                   int conceptsFirePerCycle,
//                   int tasklinkFirePerConcept,
//                   int termlinkFirePerConcept) {
//
//        this(new Memory(new FrameClock(),
//                //TermIndex.memoryWeak(numConcepts * 2)
//                new DefaultTermIndex(256)
//
//        ), numConcepts, conceptsFirePerCycle, tasklinkFirePerConcept, termlinkFirePerConcept);
//    }

    public Default(int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept) {
        this(activeConcepts, conceptsFirePerCycle, taskLinksPerConcept, termLinksPerConcept, new XorShift128PlusRandom(1));
    }

    public Default(int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept, @NotNull Random random) {
        this(activeConcepts, conceptsFirePerCycle, taskLinksPerConcept, termLinksPerConcept, random,
                new DefaultTermIndex(activeConcepts * 4, random), new FrameClock());
    }


    public Default(int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept, @NotNull Random random, TermIndex index, @NotNull Clock clock) {
        super(clock,
                index,
                random,
                Global.DEFAULT_SELF);


        the("premiser", premiser = newPremiseGenerator());
        premiser.confMin.setValue(Global.TRUTH_EPSILON);


        the("core", core = initCore(
                activeConcepts,
                conceptsFirePerCycle,
                termLinksPerConcept, taskLinksPerConcept,
                premiser
        ));

        runLater(this::initHigherNAL);


        //new QueryVariableExhaustiveResults(this.memory());

        /*
        the("memory_sharpen", new BagForgettingEnhancer(memory, core.active));
        */

    }


//    public static class DefaultTermIndex extends MapIndex2 {
//
//        public DefaultTermIndex(int capacity, @NotNull Random random) {
//            super(new HashMap(capacity, 0.9f),
//                    new DefaultConceptBuilder(random, 8, 24));
//
//        }
//    }
    public static class DefaultTermIndex extends SimpleMapIndex2 {

        public DefaultTermIndex(int capacity, @NotNull Random random) {
            super(Terms.terms,
                    new DefaultConceptBuilder(random),
                    new HashMap(capacity),
                    new HashMap(capacity)
                    //new ConcurrentHashMapUnsafe(capacity)
            );
        }
    }

    public static class WeakTermIndex2 extends GroupedMapIndex {

        public WeakTermIndex2(int capacity, @NotNull Random random) {
            super(new WeakHashMap<>(capacity),
                    new DefaultConceptBuilder(random));

        }
    }
    public static class WeakTermIndex extends SimpleMapIndex2 {

        public WeakTermIndex(int capacity, @NotNull Random random) {
            super(Terms.terms,
                    new DefaultConceptBuilder(random),
                    //new SoftValueHashMap(capacity)
                    new WeakHashMap<>(capacity),
                    new WeakHashMap<>(capacity)
            );

        }
    }

    public static class SoftTermIndex extends SimpleMapIndex2 {

        public SoftTermIndex(int capacity, @NotNull Random random) {
            super(Terms.terms,
                    new DefaultConceptBuilder(random),
                    new SoftValueHashMap(capacity),
                    new WeakHashMap(capacity)
                    //new WeakHashMap<>(capacity)
            );

        }
    }
//    public static class SoftTermIndex extends MapIndex2 {
//
//        public SoftTermIndex(int capacity, @NotNull Random random) {
//            super(new SoftValueHashMap(capacity),
//                    new DefaultConceptBuilder(random)
//
//                    //new WeakHashMap<>(capacity)
//            );
//
//        }
//    }

    /**
     * process a Task through its Concept
     */
    @Nullable @Override
    public final Concept process(@NotNull Task input, float activation) {

        Concept c = concept(input, true);
        if (c == null) {

            input.delete("Inconceivable");


        /*if (Global.DEBUG_DERIVATION_STACKTRACES && Global.DEBUG_TASK_LOG)
            task.log(Premise.getStack());*/

            //eventTaskRemoved.emit(task);

        /* else: a more destructive cleanup of the discarded task? */

            return null;
        }

        float business = input.pri() * activation;
        emotion.busy(business);


        Task t = c.process(input, this);
        if (t != null && !t.isDeleted()) {
            //TaskProcess succeeded in affecting its concept's state (ex: not a duplicate belief)

            t.onConcept(c);

            //propagate budget
            MutableFloat overflow = new MutableFloat();

            conceptualize(c, t, activation, activation, overflow);

            if (overflow.floatValue() > 0) {
                emotion.stress(overflow.floatValue());
            }

            eventTaskProcess.emit(t); //signal any additional processes

        } else {
            emotion.frustration(business);
        }

        return c;
    }


//    @NotNull
//    public TaskPerception initInput() {
//
//        //return new FIFOTaskPerception(this, null, this::process);
//
//        return new SetTaskPerception(
//                this,
//                this::process,
//                //BudgetMerge.plusDQBlend
//                BudgetMerge.plusDQDominant
//        );
//
//
//        //return new SortedTaskPerception(this, 64, 4, BudgetMerge.avgDQBlend);
//
//        /* {
//            @Override
//            protected void onOverflow(Task t) {
//                memory.eventError.emit("Overflow: " + t + " " + getStatistics());
//            }
//        };*/
//        //input.inputsMaxPerCycle.set(conceptsFirePerCycle);;
//    }

    @NotNull
    protected DefaultCycle initCore(int activeConcepts, int conceptsFirePerCycle, int termLinksPerConcept, int taskLinksPerConcept, PremiseGenerator pg) {

        DefaultCycle c = new DefaultCycle(this, pg, conceptWarm, conceptCold);
        c.active.setCapacity(activeConcepts);

        //TODO move these to a PremiseGenerator which supplies
        c.premiser.termlinksFiredPerFiredConcept.set(termLinksPerConcept);
        c.premiser.tasklinksFiredPerFiredConcept.set(taskLinksPerConcept);

        c.conceptsFiredPerCycle.set(conceptsFirePerCycle);


        return c;
    }

    @NotNull
    public DefaultPremiseGenerator newPremiseGenerator() {
        return new DefaultPremiseGenerator(this, newDeriver());
    }

    protected Deriver newDeriver() {
        return Deriver.getDefaultDeriver();
    }


//    public Bag<Concept> newConceptBagAggregateLinks(int initialCapacity) {
//        return new CurveBag<Concept>(initialCapacity, rng) {
//
//            @Override public BLink<Concept> put(Object v) {
//                BLink<Concept> b = get(v);
//                Concept c = (Concept) v;
//                if (b==null)
//                    b = new BLink(c, 0,1,1);
//
//                c.getTaskLinks().commit();
//                c.getTermLinks().commit();
//
//                float p =
//                        //Math.max(
//                        //c.getTaskLinks().getSummarySum()/taskLinkBagSize
//                        //(
//                        //c.getTaskLinks().getSummaryMean()
//                        //+c.getTermLinks().getSummaryMean()) * 0.5f
//                        c.getTaskLinks().getPriorityMax()
//
//                        // c.getTermLinks().getPriorityMax()
//
//                        //)
//                        ;
//
//                b.budget(p, 1f, 1f);
//
//                return put(c, b);
//            }
//
//        }.mergeNull();
//    }


    @Override
    public final float conceptPriority(@NotNull Termed termed) {
        if (termed!=null) {
            Concept cc = concept(termed);
            if (cc != null) {
                BLink<Concept> c = core.active.get(cc);
                if (c != null)
                    return c.priIfFiniteElseZero();
            }
        }
        return 0;
    }


    @Nullable
    @Override
    public Concept conceptualize(@NotNull Termed termed, @NotNull Budgeted b, float conceptActivation, float linkActivation, @Nullable MutableFloat conceptOverflow) {

        Concept c = concept(termed, true);
        if (c != null) {
            core.activate(c, b, conceptActivation, conceptOverflow);
            if (linkActivation > 0)
                c.link(b, linkActivation, Default.this, conceptOverflow);
        }
        return c;

    }


    @NotNull
    @Override
    public NAR forEachConcept(@NotNull Consumer<Concept> recip) {
        core.active.forEachKey(recip);
        return this;
    }

    /**
     * The original deterministic memory cycle implementation that is currently used as a standard
     * for development and testing.
     */
    public abstract static class AbstractCycle {

        @NotNull
        final Active handlers;


        /**
         * How many concepts to fire each cycle; measures degree of parallelism in each cycle
         */
        @NotNull
        @Range(min = 0, max = 64, unit = "Concept")
        public final MutableInteger conceptsFiredPerCycle;



        @Range(min = 0.01f, max = 8, unit = "Duration")
        public final MutableFloat conceptRemembering;




        public final Forget.ExpForget taskLinkForget;

        @NotNull
        public final BudgetForget termLinkForget;


        //public final MutableFloat activationFactor = new MutableFloat(1.0f);

//        final Function<Task, Task> derivationPostProcess = d -> {
//            return LimitDerivationPriority.limitDerivation(d);
//        };


        /**
         * concepts active in this cycle
         */
        public final Bag<Concept> active;

        @NotNull
        @Deprecated
        public final transient NAR nar;

//        @Range(min=0,max=8192,unit="Concept")
//        public final MutableInteger capacity = new MutableInteger();


//        @NotNull
//        @Deprecated @Range(min = 0, max = 1f, unit = "Perfection")
//        public final MutableFloat perfection;


        @NotNull public final Forget.AbstractForget conceptForget;


        @NotNull final PremiseGenerator premiser;

        private float cyclesPerFrame;
        private int cycleNum;

//        @Deprecated
//        int tasklinks = 2; //TODO use MutableInteger for this
//        @Deprecated
//        int termlinks = 3; //TODO use MutableInteger for this


        protected AbstractCycle(@NotNull NAR nar, PremiseGenerator premiseGenerator) {

            this.nar = nar;

            this.premiser = premiseGenerator;

            this.conceptRemembering = nar.conceptRemembering;

            this.conceptsFiredPerCycle = new MutableInteger(1);
            this.active = newConceptBag();

            this.handlers = new Active(
                    nar.eventFrameStart.on(this::frame),
                    nar.eventCycleEnd.on(this::cycle),
                    nar.eventReset.on(this::reset)
            );

            this.conceptForget = new Forget.ExpForget(conceptRemembering, nar.perfection);
            this.termLinkForget = new Forget.ExpForget(nar.termLinkRemembering, nar.perfection);
            this.taskLinkForget = new Forget.ExpForget(nar.taskLinkRemembering, nar.perfection);

        }

        protected abstract Bag<Concept> newConceptBag();

        protected void frame(@NotNull NAR nar) {
            cyclesPerFrame = nar.cyclesPerFrame.floatValue();
            cycleNum = 0;

            conceptForget.update(nar);
            taskLinkForget.update(nar);
            termLinkForget.update(nar);

            premiser.frame(nar);
        }

        protected void cycle(Memory memory) {

            float subCycle = cycleNum++ / cyclesPerFrame;
            conceptForget.cycle(subCycle);
            termLinkForget.cycle(subCycle);
            taskLinkForget.cycle(subCycle);

            //active.forEach(conceptForget); //TODO use downsampling % of concepts not TOP
            //active.printAll();

            active.commit( /*active.isFull() ? */conceptForget /*: null*/ );

            fireConcepts(conceptsFiredPerCycle.intValue());

            //active.commit(lastForget != now ? conceptForget : .. );

//            if (!((CurveBag)active).isSorted()) {
//                throw new RuntimeException(active + " not sorted");
//            }

        }


        /**
         * samples a next active concept
         */
        @Deprecated
        public final Concept next() {
            return active.sample().get();
        }


        private void reset(Memory m) {
            active.clear();
        }


        protected final void fireConcepts(int conceptsToFire) {

            Bag<Concept> b = this.active;

            if (conceptsToFire == 0 || b.isEmpty()) return;

            b.sample(conceptsToFire, this::fireConcept);

        }

        protected final void fireConcept(BLink<Concept> conceptLink) {
            Concept concept = conceptLink.get();

            Bag<Task> taskl = concept.tasklinks();
            taskl.commit(/*taskl.isFull() ? */taskLinkForget/* : null*/);

            Bag<Termed> terml = concept.termlinks();
            terml.commit(/*terml.isFull() ? */termLinkForget/* : null*/);

            premiser.accept(conceptLink);
        }



        @Nullable
        final void activate(@NotNull Concept c, @NotNull Budgeted b, float scale, @Nullable MutableFloat overflowing) {
            active.put(c, b, scale, overflowing);
        }


        //try to implement some other way, this is here because of serializability

    }

    /**
     * groups each derivation's tasks as a group before inputting into
     * the main perception buffer, allowing post-processing such as budget normalization.
     * <p>
     * ex: this can ensure that a premise which produces many derived tasks
     * will not consume budget unfairly relative to another premise
     * with less tasks but equal budget.
     */
    public static final class DefaultCycle extends AbstractCycle {

        private final DefaultConceptBudgeting cold;
        private final DefaultConceptBudgeting warm;


        public DefaultCycle(@NotNull NAR nar, PremiseGenerator premiseGenerator, DefaultConceptBudgeting warm, DefaultConceptBudgeting cold) {
            super(nar, premiseGenerator);
            this.warm = warm;
            this.cold = cold;
        }


        /** called when a concept is displaced from the concept bag */
        protected void deactivate(BLink<Concept> cl) {
            Concept c = cl.get();

            //apply forgetting so that shrinking capacity will be applied to concept's components fairly
            c.tasklinks().commit(Forget.QualityToPriority);
            c.termlinks().commit(Forget.QualityToPriority);

            c.capacity(cold);

            nar.emotion.alert(1f/active.size());
        }

        /** called when a concept enters the concept bag */
        protected void activate(Concept c) {

            //set capacity first in case there are any queued items, they may join during the commit */
            c.capacity(warm);

            //clean out any deleted links since having been deactivated
            c.tasklinks().commit(Forget.QualityToPriority);
            c.termlinks().commit(Forget.QualityToPriority);
        }

        @Override
        protected Bag<Concept> newConceptBag() {
            return new MonitoredCurveBag(nar, 1, nar.random)
                    .merge(BudgetMerge.plusDQBlend);
        }

        /** extends CurveBag to invoke entrance/exit event handler lambda */
        public final class MonitoredCurveBag extends CurveBag<Concept> {

            final NAR nar;

            public MonitoredCurveBag(NAR nar, int capacity, Random rng) {
                super(capacity, rng);
                this.nar = nar;
                setCapacity(capacity);
            }


            @Nullable
            @Override
            protected BLink<Concept> putNew(Concept i, BLink<Concept> b) {
                BLink<Concept> displaced = super.putNew(i, b);
                if (displaced!=null) {

                    Concept dd = displaced.get();
                    if (dd != i)
                        activate(i);

                    deactivate(displaced);

                } else {
                    activate(i);
                }

                return displaced;
            }

            @Nullable
            @Override
            public BLink<Concept> remove(Concept x) {
                BLink<Concept> removed = super.remove(x);
                if (removed!=null) {
                    deactivate(removed);
                }
                return removed;
            }


        }





//        @NotNull
//        static HashBag<Task> detectDuplicates(@NotNull Collection<Task> buffer) {
//            HashBag<Task> taskCount = new HashBag<>();
//            taskCount.addAll(buffer);
//            taskCount.forEachWithOccurrences((t, i) -> {
//                if (i == 1) return;
//
//                System.err.println("DUPLICATE TASK(" + i + "): " + t);
//                List<Task> equiv = buffer.stream().filter(u -> u.equals(t)).collect(toList());
//                HashBag<String> rules = new HashBag();
//                equiv.forEach(u -> {
//                    String rule = u.getLogLast().toString();
//                    rules.add(rule);
//
////                    System.err.println("\t" + u );
////                    System.err.println("\t\t" + rule );
////                    System.err.println();
//                });
//                rules.forEachWithOccurrences((String r, int c) -> System.err.println("\t" + c + '\t' + r));
//                System.err.println("--");
//
//            });
//            return taskCount;
//        }

    }


    /**
     * single-threaded premise generator and processor; re-uses the same result collection buffer
     */
    public static class DefaultPremiseGenerator extends PremiseGenerator {



        /**
         * derived tasks with truth confidence lower than this value are discarded.
         */
        @NotNull
        @Range(min = 0, max = 1f)
        public final MutableFloat confMin;

//        public DefaultPremiseGenerator(@NotNull NAR nar, Deriver deriver) {
//            /** the resutls buffer should probably be a Set because the derivations may duplicate */
//            this(nar, deriver, Global.newHashSet(64));
//        }

        public DefaultPremiseGenerator(@NotNull NAR nar, @NotNull Deriver deriver) {
            super(nar, new PremiseEval(nar.index, nar.random, deriver));

            this.confMin = new MutableFloat(Global.TRUTH_EPSILON);
        }

        /**
         * update derivation parameters (each frame)
         */
        @Override public final void frame(@NotNull NAR nar) {
            matcher.setMinConfidence(confMin.floatValue());
        }




    }

}

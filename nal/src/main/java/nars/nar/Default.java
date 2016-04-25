package nars.nar;

import nars.Global;
import nars.Memory;
import nars.NAR;
import nars.bag.BLink;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.budget.BudgetMerge;
import nars.budget.Budgeted;
import nars.budget.Forget;
import nars.concept.Concept;
import nars.concept.DefaultConceptBuilder;
import nars.concept.PremiseGenerator;
import nars.data.Range;
import nars.nal.Deriver;
import nars.nal.meta.PremiseEval;
import nars.task.Task;
import nars.task.flow.SetTaskPerception;
import nars.task.flow.TaskPerception;
import nars.term.Term;
import nars.term.TermIndex;
import nars.term.Termed;
import nars.time.FrameClock;
import nars.util.data.MutableInteger;
import nars.util.data.random.XorShift128PlusRandom;
import nars.util.event.Active;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;

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
                new DefaultTermIndex(activeConcepts * 8, random));
    }

    public Default(int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept, @NotNull Random random, TermIndex index) {
        super(new FrameClock(),
                index,
                random,
                Global.DEFAULT_SELF);

        the("input", initInput());

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


    /**
     * process a Task through its Concept
     */
    @Nullable @Override
    public final Concept process(@NotNull Task input, float activation) {

        Concept c = concept(input, true);
        if (c == null) {
            remove(input, "Inconceivable");
            return null;
        }

        float business = input.pri() * activation;
        emotion.busy(business);

        Task t = c.process(input, this);
        if (t != null) {

            //TaskProcess succeeded in affecting its concept's state (ex: not a duplicate belief)

            //propagate budget
            MutableFloat overflow = new MutableFloat();

            conceptualize(c, t, activation, overflow, true);

            if (overflow.floatValue() > 0) {
                emotion.stress(overflow.floatValue());
            }

            eventTaskProcess.emit(t); //signal any additional processes

        } else {
            emotion.frustration(business);
        }

        return c;
    }


    @NotNull
    public TaskPerception initInput() {

        //return new FIFOTaskPerception(this, null, this::process);

        return new SetTaskPerception(
                this,
                this::process,
                //BudgetMerge.plusDQBlend
                BudgetMerge.plusDQDominant
        );


        //return new SortedTaskPerception(this, 64, 4, BudgetMerge.avgDQBlend);

        /* {
            @Override
            protected void onOverflow(Task t) {
                memory.eventError.emit("Overflow: " + t + " " + getStatistics());
            }
        };*/
        //input.inputsMaxPerCycle.set(conceptsFirePerCycle);;
    }

    @NotNull
    protected DefaultCycle initCore(int activeConcepts, int conceptsFirePerCycle, int termLinksPerConcept, int taskLinksPerConcept, PremiseGenerator pg) {


        DefaultCycle c = new DefaultCycle(this, pg, activeConcepts);

        //TODO move these to a PremiseGenerator which supplies
        c.premiser.termlinksFiredPerFiredConcept.set(termLinksPerConcept);
        c.premiser.tasklinksFiredPerFiredConcept.set(taskLinksPerConcept);

        c.conceptsFiredPerCycle.set(conceptsFirePerCycle);


        return c;
    }

    @NotNull
    public DefaultPremiseGenerator newPremiseGenerator() {
        return new DefaultPremiseGenerator(this, newDeriver(),
            new Forget.ExpForget<>(taskLinkRemembering, perfection).withDeletedItemFiltering(),
            new Forget.ExpForget<>(termLinkRemembering, perfection)
        );
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
    public final float conceptPriority(@NotNull Termed termed, float priIfNonExistent) {
        Concept cc = concept(termed);
        if (cc != null) {
            BLink<Concept> c = core.active.get(cc);
            if (c != null)
                return c.priIfFiniteElse(priIfNonExistent);
        }
        return priIfNonExistent;
    }


    @Nullable
    @Override
    public Concept conceptualize(@NotNull Termed termed, @NotNull Budgeted activation, float scale, @Nullable MutableFloat conceptOverflow, boolean link) {

        Concept c = concept(termed, true);
        if (c != null) {
            core.activate(c, activation, scale, conceptOverflow);
            if (link)
                c.link(activation, scale, Default.this, conceptOverflow);
        }
        return c;

    }


    @NotNull
    @Override
    public NAR forEachConcept(@NotNull Consumer<Concept> recip) {
        core.active.forEachKey(recip);
        return this;
    }

    @Nullable
    @Override
    public Function<Term, Concept> newConceptBuilder() {
        return new DefaultConceptBuilder(random,
                12 /* tasklinks*/,
                16 /*termlinks */);
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


        @Range(min = 0.0f, max = 1.0, unit = "Percent")
        public final MutableFloat activationRate;

        @Range(min = 0.01f, max = 8, unit = "Duration")
        public final MutableFloat conceptRemembering;



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


        @NotNull
        @Deprecated @Range(min = 0, max = 1f, unit = "Perfection")
        public final MutableFloat perfection;


        @NotNull
        public final Forget.AbstractForget<Concept> conceptForget;


        final PremiseGenerator premiser;

//        @Deprecated
//        int tasklinks = 2; //TODO use MutableInteger for this
//        @Deprecated
//        int termlinks = 3; //TODO use MutableInteger for this

        /* ---------- Short-term workspace for a single cycle ------- */

        protected AbstractCycle(@NotNull NAR nar, Bag<Concept> concepts, PremiseGenerator premiseGenerator) {

            this.nar = nar;

            this.premiser = premiseGenerator;

            this.activationRate = nar.activationRate;
            this.conceptRemembering = nar.conceptRemembering;
            this.perfection = nar.perfection;

            conceptsFiredPerCycle = new MutableInteger(1);
            active = concepts;

            this.handlers = new Active(
                    nar.eventFrameStart.on(this::frame),
                    nar.eventCycleEnd.on(this::cycle),
                    nar.eventReset.on(this::reset)
            );

            conceptForget = new Forget.ExpForget(conceptRemembering, perfection);


        }

        protected final void frame(@NotNull NAR nar) {
            conceptForget.update(nar);
            premiser.frame(nar);
        }

        protected final void cycle(Memory memory) {


            fireConcepts(conceptsFiredPerCycle.intValue());
            commit();

        }

        /**
         * apply pending activity at the end of a cycle
         */
        private final void commit() {

            Bag<Concept> active = this.active;

            active.forEach(conceptForget); //TODO use downsampling % of concepts not TOP
            //active.printAll();

            active.commit();

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

            b.sample(conceptsToFire, this.premiser);

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
    public static class DefaultCycle extends AbstractCycle {


        public DefaultCycle(@NotNull NAR nar, PremiseGenerator premiseGenerator, int activeConcepts) {
            super(nar, newConceptBag(nar.random, activeConcepts), premiseGenerator);
        }


        @NotNull
        public static Bag<Concept> newConceptBag(@NotNull Random r, int n) {
            return new CurveBag<Concept>(n, r)
                    //.merge(BudgetMerge.plusDQBlend);
                    .merge(BudgetMerge.plusDQDominant);
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

        public DefaultPremiseGenerator(@NotNull NAR nar, @NotNull Deriver deriver, @NotNull Forget.BudgetForgetFilter<Task> taskLinkForget, @NotNull Forget.BudgetForget<Termed> termLinkForget) {
            super(nar, new PremiseEval(nar.random, deriver), taskLinkForget, termLinkForget);

            this.confMin = new MutableFloat(Global.TRUTH_EPSILON);

        }

        /**
         * update derivation parameters (each frame)
         */
        @Override public final void frame(@NotNull NAR nar) {
            super.frame(nar);
            matcher.setMinConfidence(confMin.floatValue());
            taskLinkForget.update(nar);
            termLinkForget.update(nar);
        }



    }

}

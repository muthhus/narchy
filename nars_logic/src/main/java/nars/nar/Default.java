package nars.nar;

import nars.Global;
import nars.Memory;
import nars.NAR;
import nars.bag.BLink;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.budget.Budget;
import nars.budget.BudgetMerge;
import nars.budget.Forget;
import nars.concept.Concept;
import nars.concept.ConceptProcess;
import nars.concept.DefaultConceptProcess;
import nars.concept.PremiseGenerator;
import nars.data.Range;
import nars.nal.Deriver;
import nars.nal.meta.PremiseMatch;
import nars.task.Task;
import nars.task.flow.SetTaskPerception;
import nars.task.flow.TaskPerception;
import nars.term.TermIndex;
import nars.term.Termed;
import nars.time.FrameClock;
import nars.util.data.MutableInteger;
import nars.util.event.Active;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * Various extensions enabled
 */
public class Default extends AbstractNAR {

    @NotNull
    public final DefaultCycle core;


    @NotNull
    public final TaskPerception input;

    @NotNull
    public final DefaultPremiseGenerator premiser;


    @Deprecated
    public Default() {
        this(1024, 1, 1, 3);
    }

    public Default(int numConcepts,
                   int conceptsFirePerCycle,
                   int tasklinkFirePerConcept,
                   int termlinkFirePerConcept) {
        this(new Memory(new FrameClock(),
                //TermIndex.memoryWeak(numConcepts * 2)
                TermIndex.memory(numConcepts * 2)

        ), numConcepts, conceptsFirePerCycle, tasklinkFirePerConcept, termlinkFirePerConcept);
    }

    public Default(@NotNull Memory mem, int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept) {
        super(mem);

        the("input", input = initInput());

        the("premiser", premiser = newPremiseGenerator());
        premiser.confMin.setValue(4f * Global.TRUTH_EPSILON);

        the("core", core = initCore(
                activeConcepts,
                conceptsFirePerCycle,
                termLinksPerConcept, taskLinksPerConcept,
                premiser
        ));

        if (core!=null) {
            runLater(this::initHigherNAL);
        }


        //new QueryVariableExhaustiveResults(this.memory());

        /*
        the("memory_sharpen", new BagForgettingEnhancer(memory, core.active));
        */

    }


    @NotNull
    public TaskPerception initInput() {

        return new SetTaskPerception(
                memory, this::process, BudgetMerge.plusDQBlend);

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


        DefaultCycle c = new DefaultCycle(this, newDeriver(), newConceptBag(activeConcepts), pg);

        //TODO move these to a PremiseGenerator which supplies
        // batches of Premises
        c.termlinksFiredPerFiredConcept.set(termLinksPerConcept);
        c.tasklinksFiredPerFiredConcept.set(taskLinksPerConcept);

        c.conceptsFiredPerCycle.set(conceptsFirePerCycle);

        c.capacity.set(activeConcepts);

        return c;
    }

    @Nullable
    public DefaultPremiseGenerator newPremiseGenerator() {
        return new DefaultPremiseGenerator(this, Deriver.getDefaultDeriver());
    }

    @NotNull
    public Bag<Concept> newConceptBag(int initialCapacity) {
        return new CurveBag<Concept>(initialCapacity, rng)
                //.mergePlus();
                .merge(BudgetMerge.plusDQBlend);
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


    @Override public final float conceptPriority(Termed termed, float priIfNonExistent) {
        Concept cc = concept(termed);
        if (cc != null) {
            BLink<Concept> c = core.active.get(cc);
            if (c != null)
                return c.priIfFiniteElse(priIfNonExistent);
        }
        return priIfNonExistent;
    }

    @Override
    public Concept conceptualize(Termed termed, Budget activation, float scale) {
        Concept c = concept(termed);
        if (c!=null) {
            core.activate(c, activation, scale);
        }
        return c;
    }

    @NotNull
    @Override
    public NAR forEachConcept(Consumer<Concept> recip) {
        core.active.forEachKey(recip);
        return this;
    }




    /**
     * The original deterministic memory cycle implementation that is currently used as a standard
     * for development and testing.
     */
    public abstract static class AbstractCycle  {

        @NotNull
        final Active handlers;

        public final Deriver  der;

        /**
         * How many concepts to fire each cycle; measures degree of parallelism in each cycle
         */
        @NotNull
        @Range(min=0,max=64,unit="Concept")
        public final MutableInteger conceptsFiredPerCycle;

        @Range(min=0,max=16,unit="TaskLink") //TODO use float percentage
        public final MutableInteger tasklinksFiredPerFiredConcept = new MutableInteger(1);

        @Range(min=0,max=16,unit="TermLink")
        public final MutableInteger termlinksFiredPerFiredConcept = new MutableInteger(1);

        @Range(min=0.0f,max=1.0,unit="Percent")
        public final MutableFloat activationRate;

        @Range(min=0.01f,max=8,unit="Duration")
        public final MutableFloat conceptRemembering;

        @Range(min=0.01f,max=8,unit="Duration")
        public final MutableFloat termLinkRemembering;

        @Range(min=0.01f,max=8,unit="Duration")
        public final MutableFloat taskLinkRemembering;

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

        @Range(min=0,max=8192,unit="Concept")
        public final MutableInteger capacity = new MutableInteger();

        /** activated concepts pending (re-)insert to bag */
        public final LinkedHashSet<Concept> activated = new LinkedHashSet();


        @NotNull
        @Range(min=0, max=1f,unit="Perfection")
        public final MutableFloat perfection;

        final List<BLink<Concept>> firing = Global.newArrayList(1);

        @NotNull
        private final Forget.ForgetAndDetectDeletion<Task> taskLinkForget;
        @NotNull
        private final Forget.AbstractForget<Termed> termLinkForget;
        @NotNull
        private final Forget.AbstractForget<Concept> conceptForget;



        final PremiseGenerator premiser;

//        @Deprecated
//        int tasklinks = 2; //TODO use MutableInteger for this
//        @Deprecated
//        int termlinks = 3; //TODO use MutableInteger for this

        /* ---------- Short-term workspace for a single cycle ------- */

        protected AbstractCycle(@NotNull NAR nar, Deriver deriver, Bag<Concept> concepts, PremiseGenerator premiseGenerator) {

            this.nar = nar;

            this.der = deriver;


            this.premiser = premiseGenerator;

            Memory m = nar.memory;

            this.activationRate = m.activationRate;
            this.conceptRemembering = m.conceptForgetDurations;
            this.termLinkRemembering = m.termLinkForgetDurations;
            this.taskLinkRemembering = m.taskLinkForgetDurations;
            this.perfection = m.perfection;

            conceptsFiredPerCycle = new MutableInteger(1);
            active = concepts;

            this.handlers = new Active(
                m.eventCycleEnd.on(this::cycle),
                m.eventReset.on(this::reset)
            );

            conceptForget = new Forget.ExpForget(nar, conceptRemembering, perfection);
            termLinkForget = new Forget.ExpForget(nar, termLinkRemembering, perfection);
            taskLinkForget = new Forget.ExpForget(nar, taskLinkRemembering, perfection)
                    .withDeletedItemFiltering();

        }

        protected final void cycle(Memory memory) {

            fireConcepts(conceptsFiredPerCycle.intValue());

            commit();
        }

        /** apply pending activity at the end of a cycle */
        private final void commit() {

            Bag<Concept> active = this.active;

            active.forEach(conceptForget); //TODO use downsampling % of concepts not TOP
            //active.printAll();

            LinkedHashSet<Concept> a = this.activated;
            if (!a.isEmpty()) {
                active.putAll(a);
                a.clear();
            }

            active.commit();

        }

        /**
         * samples a next active concept
         */
        @Deprecated public final Concept next() {
            return active.sample().get();
        }


        private void reset(Memory m) {
            active.clear();
            activated.clear();
        }


        protected final void fireConcepts(int conceptsToFire) {

            Bag<Concept> b = this.active;

            b.setCapacity(capacity.intValue()); //TODO share the MutableInteger so that this doesnt need to be called ever
            if (conceptsToFire == 0 || b.isEmpty()) return;

            List<BLink<Concept>> f = this.firing;
            b.sample(conceptsToFire, f);

            int tasklinksToFire = tasklinksFiredPerFiredConcept.intValue();
            int termlnksToFire = termlinksFiredPerFiredConcept.intValue();
            Forget.AbstractForget<Termed> termLinkForget = this.termLinkForget;
            Forget.ForgetAndDetectDeletion<Task> taskLinkForget = this.taskLinkForget;

            PremiseGenerator pg = this.premiser;

            for (int i = 0, fSize = f.size(); i < fSize; i++) {
                pg.firePremiseSquared(
                        f.get(i),
                        tasklinksToFire,
                        termlnksToFire,
                        termLinkForget,
                        taskLinkForget
                );
            }

            f.clear();
        }

        final void activate(Concept c, Budget b, float scale) {
            active.put(c, b, scale * activationRate.floatValue());
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



        public DefaultCycle(@NotNull NAR nar, Deriver deriver, Bag<Concept> concepts, PremiseGenerator premiseGenerator) {
            super(nar, deriver, concepts, premiseGenerator);
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


    /** single-threaded premise generator and processor; re-uses the same result collection buffer */
    public static class DefaultPremiseGenerator extends PremiseGenerator {

        protected final Collection<Task> sharedResultBuffer;

        /**
         * re-used, not to be used outside of this
         */
        @NotNull
        final PremiseMatch matcher;

        /** derived tasks with truth confidence lower than this value are discarded. */
        @NotNull
        @Range(min=0, max=1f)
        public final MutableFloat confMin;

        public DefaultPremiseGenerator(@NotNull NAR nar, Deriver deriver) {
            /** the resutls buffer should probably be a Set because the derivations may duplicate */
            this(nar, deriver, Global.newHashSet(64));
        }

        public DefaultPremiseGenerator(@NotNull NAR nar, Deriver deriver, Collection<Task> resultsBuffer) {
            super(nar);
            this.matcher = new PremiseMatch(nar.memory.random, deriver);
            this.sharedResultBuffer = resultsBuffer;
            this.confMin = new MutableFloat(Global.TRUTH_EPSILON);

            nar.onFrame(this::updateDerivationParameters);
        }

        /** update derivation parameters (each frame) */
        private void updateDerivationParameters(NAR nar) {
            matcher.setMinConfidence(confMin.floatValue());
        }

        @Override
        protected void premise(BLink<? extends Concept> concept, BLink<? extends Task> taskLink, BLink<? extends Termed> termLink, Task belief) {
            newPremise(concept, taskLink, termLink, belief).run(matcher);
        }

        protected @NotNull ConceptProcess newPremise(BLink<? extends Concept> concept, BLink<? extends Task> taskLink, BLink<? extends Termed> termLink, Task belief) {
            return new DefaultConceptProcess(nar, concept, taskLink, termLink, belief, sharedResultBuffer);
        }
    }
}

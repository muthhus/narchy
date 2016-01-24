package nars.nar;

import com.gs.collections.impl.bag.mutable.HashBag;
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
import nars.data.Range;
import nars.nal.Deriver;
import nars.nal.meta.PremiseMatch;
import nars.nar.experimental.Derivelet;
import nars.task.Task;
import nars.task.flow.SetTaskPerception;
import nars.task.flow.TaskPerception;
import nars.term.TermIndex;
import nars.term.Termed;
import nars.time.FrameClock;
import nars.util.data.MutableInteger;
import nars.util.data.list.FasterList;
import nars.util.event.Active;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;

/**
 * Various extensions enabled
 */
public class Default extends AbstractNAR {

    @NotNull
    public final DefaultCycle core;
    @NotNull
    public final TaskPerception input;


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

        the("core", core = initCore(
                activeConcepts,
                conceptsFirePerCycle,
                termLinksPerConcept, taskLinksPerConcept
        ));

        if (core!=null) {
            runLater(this::initHigherNAL);
        }


        //new QueryVariableExhaustiveResults(this.memory());

        /*
        the("memory_sharpen", new BagForgettingEnhancer(memory, core.active));
        */

    }

//    public TaskPerception _initInput() {
//        return new FIFOTaskPerception(this, null, this::process);
//    }

    @NotNull
    public TaskPerception initInput() {

        return new SetTaskPerception(
                memory, this::process, BudgetMerge.plusDQDominated);

        /* {
            @Override
            protected void onOverflow(Task t) {
                memory.eventError.emit("Overflow: " + t + " " + getStatistics());
            }
        };*/
        //input.inputsMaxPerCycle.set(conceptsFirePerCycle);;
    }

    @NotNull
    protected DefaultCycle initCore(int activeConcepts, int conceptsFirePerCycle, int termLinksPerConcept, int taskLinksPerConcept) {

        DefaultCycle c = new DefaultCycle(this, newDeriver(), newConceptBag(activeConcepts));

        //TODO move these to a PremiseGenerator which supplies
        // batches of Premises
        c.termlinksFiredPerFiredConcept.set(termLinksPerConcept);
        c.tasklinksFiredPerFiredConcept.set(taskLinksPerConcept);

        c.conceptsFiredPerCycle.set(conceptsFirePerCycle);

        c.capacity.set(activeConcepts);

        c.confidenceDerivationMin.setValue(4f * Global.TRUTH_EPSILON);

        return c;
    }

    @NotNull
    public Bag<Concept> newConceptBag(int initialCapacity) {
        return new CurveBag<Concept>(initialCapacity, rng).mergePlus();
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
        Concept cc = memory.concept(termed);
        if (cc != null) {
            BLink<Concept> c = core.active.get(cc);
            if (c != null)
                return c.pri();
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


    public static final Predicate<BLink<?>> simpleForgetDecay = (b) -> {
        float p = b.pri() * 0.95f;
        if (p > b.qua()*0.1f)
            b.setPriority(p);
        return true;
    };

    /**
     * The original deterministic memory cycle implementation that is currently used as a standard
     * for development and testing.
     */
    public abstract static class AbstractCycle implements Consumer<BLink<? extends Concept>> {

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

        final Derivelet deriver = new Derivelet();

        @NotNull
        @Range(min=0, max=1f,unit="Perfection")
        public final MutableFloat perfection;

        final List<BLink<Concept>> firing = Global.newArrayList(1);

        @NotNull
        private final Forget.ForgetAndDetectItemDeletion<Task> taskLinkForget;
        @NotNull
        private final Forget.AbstractForget<Termed> termLinkForget;
        @NotNull
        private final Forget.AbstractForget<Concept> conceptForget;

        //cached
        private transient int termlnksToFire, tasklinksToFire;

//        @Deprecated
//        int tasklinks = 2; //TODO use MutableInteger for this
//        @Deprecated
//        int termlinks = 3; //TODO use MutableInteger for this

        /* ---------- Short-term workspace for a single cycle ------- */

        protected AbstractCycle(@NotNull NAR nar, Deriver deriver, Bag<Concept> concepts) {

            this.nar = nar;

            this.der = deriver;


            Memory m = nar.memory;

            this.activationRate = m.activationRate;
            this.conceptRemembering = m.conceptForgetDurations;
            this.termLinkRemembering = m.termLinkForgetDurations;
            this.taskLinkRemembering = m.taskLinkForgetDurations;
            this.perfection = m.perfection;

            conceptsFiredPerCycle = new MutableInteger(1);
            active = concepts;

            this.handlers = new Active(
                m.eventCycleEnd.on(this::onCycle),
                m.eventReset.on((mem) -> onReset())
            );

            conceptForget = new Forget.LinearForget(nar, conceptRemembering, perfection);
            termLinkForget = new Forget.LinearForget(nar, termLinkRemembering, perfection);
            taskLinkForget = new Forget.LinearForget(nar, taskLinkRemembering, perfection)
                    .withDeletedItemFiltering();

        }

        protected void onCycle(Memory memory) {

            fireConcepts(conceptsFiredPerCycle.intValue());

            Bag<Concept> active = this.active;

            active.forEach(conceptForget); //TODO use downsampling % of concepts not TOP
            //active.printAll();

            LinkedHashSet<Concept> a = this.activated;
            if (!a.isEmpty()) {
                a.forEach(active::put);
                a.clear();
            }

            active.commit();
        }

        /** processes derivation result */
        protected abstract void process(ConceptProcess cp);

        /**
         * samples an active concept
         */
        public final Concept next() {
            return active.sample().get();
        }


        private void onReset() {
            active.clear();
            activated.clear();
        }


        protected final void fireConcepts(int conceptsToFire) {

            Bag<Concept> b = this.active;

            b.setCapacity(capacity.intValue()); //TODO share the MutableInteger so that this doesnt need to be called ever
            if (conceptsToFire == 0 || b.isEmpty()) return;

            List<BLink<Concept>> f = this.firing;
            b.sample(conceptsToFire, f);

            tasklinksToFire = tasklinksFiredPerFiredConcept.intValue();
            termlnksToFire = termlinksFiredPerFiredConcept.intValue();

            f.forEach(this);
            f.clear();

        }

        final void activate(Concept c, Budget b, float scale) {
            active.put(c, b, scale * activationRate.floatValue());
        }

        /** fires a concept selected by the bag */
        @Override public final void accept(@NotNull BLink<? extends Concept> cb) {

            //c.getTermLinks().up(simpleForgetDecay);
            //c.getTaskLinks().update(simpleForgetDecay);

            deriver.firePremiseSquare(nar, this::process, cb,
                tasklinksToFire,
                termlnksToFire,
                termLinkForget,
                taskLinkForget
            );

            //activate(c);
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

        /** derived tasks with truth confidence lower than this value are discarded. */
        @Range(min=0, max=1f)
        public final MutableFloat confidenceDerivationMin = new MutableFloat(Global.TRUTH_EPSILON);

        /**
         * re-used, not to be used outside of this
         */
        @NotNull
        private final PremiseMatch matcher;

        /**
         * holds the resulting tasks of one derivation so they can
         * be normalized or some other filter or aggregation
         * applied collectively.
         */
        @NotNull
        private final Collection<Task> derivedTasksBuffer;

        private final Consumer<Task> onDerived;



        public DefaultCycle(@NotNull NAR nar, Deriver deriver, Bag<Concept> concepts) {
            super(nar, deriver, concepts);

            matcher = new PremiseMatch(nar.memory.random, deriver);
            /* if detecting duplicates, use a list. otherwise use a set to deduplicate anyway */
            derivedTasksBuffer =
                    Global.DEBUG_DETECT_DUPLICATE_DERIVATIONS ?
                            new FasterList() : Global.newHashSet(1);

            onDerived = derivedTasksBuffer::add;
        }

        @Override protected void onCycle(Memory memory) {
            matcher.setMinConfidence(confidenceDerivationMin.floatValue());
            super.onCycle(memory);
        }


        @Override
        public void process(@NotNull ConceptProcess p) {

            matcher.start(p, onDerived);

            Collection<Task> buffer = derivedTasksBuffer;

            if (Global.DEBUG_DETECT_DUPLICATE_DERIVATIONS) {
                HashBag<Task> b = detectDuplicates(buffer);
                buffer.clear();
                b.addAll(buffer);
            }

            if (!buffer.isEmpty()) {
                Task.inputNormalized( buffer,
                        //p.getMeanPriority()
                        p.getTask().pri()

                        //p.getTask().getPriority() * 1f/buffer.size()
                        //p.getTask().getPriority()/buffer.size()
                        //p.taskLink.getPriority()
                        //p.getTaskLink().getPriority()/buffer.size()

                        //p.conceptLink.getPriority()
                        //UtilityFunctions.or(p.conceptLink.getPriority(), p.taskLink.getPriority())

                ,nar::input);
                buffer.clear();
            }

        }

        @NotNull
        static HashBag<Task> detectDuplicates(@NotNull Collection<Task> buffer) {
            HashBag<Task> taskCount = new HashBag<>();
            taskCount.addAll(buffer);
            taskCount.forEachWithOccurrences((t, i) -> {
                if (i == 1) return;

                System.err.println("DUPLICATE TASK(" + i + "): " + t);
                List<Task> equiv = buffer.stream().filter(u -> u.equals(t)).collect(toList());
                HashBag<String> rules = new HashBag();
                equiv.forEach(u -> {
                    String rule = u.getLogLast().toString();
                    rules.add(rule);

//                    System.err.println("\t" + u );
//                    System.err.println("\t\t" + rule );
//                    System.err.println();
                });
                rules.forEachWithOccurrences((String r, int c) -> System.err.println("\t" + c + '\t' + r));
                System.err.println("--");

            });
            return taskCount;
        }

    }


}

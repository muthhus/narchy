package nars.nar;

import com.gs.collections.impl.tuple.Tuples;
import nars.Global;
import nars.Memory;
import nars.NAR;
import nars.Symbols;
import nars.bag.BLink;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.budget.*;
import nars.concept.*;
import nars.data.Range;
import nars.nal.Deriver;
import nars.nal.meta.PremiseEval;
import nars.task.Task;
import nars.task.flow.SetTaskPerception;
import nars.task.flow.TaskPerception;
import nars.term.Term;
import nars.term.Termed;
import nars.time.FrameClock;
import nars.util.data.MutableInteger;
import nars.util.data.random.XorShift128PlusRandom;
import nars.util.event.Active;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;

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

    public Default(int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept, Random random) {
        super(new FrameClock(),
                new DefaultTermIndex(256, random),
                random,
                Global.DEFAULT_SELF);

        the("input", input = initInput());

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

    final Activator processor = new Activator() {

        final NAR nar = Default.this; //temporary

        @Override
        public final void accept(@NotNull Task[] input, float activation) {
            for (Task t : input) {
                if (t == null) //for null-terminated arrays
                    break;
                if (!t.isDeleted())
                    apply(t, activation);
            }
        }

        /**
         * execute a Task as a TaskProcess (synchronous)
         * <p>
         * TODO make private
         */
        @Nullable
        public final Concept apply(@NotNull Task input, float activation) {

            Concept c = concept(input, true);
            if (c == null) {
                remove(input, "Inconceivable");
                return null;
            }

            emotion.busy(input, activation);

            Task t = c.process(input, nar);
            if (t != null) {

                //TaskProcess succeeded in affecting its concept's state (ex: not a duplicate belief)

                //1. propagate budget
                conceptualize(c, t.budget(), activation);


                switch (t.punc()) {
                    case Symbols.GOAL:
                        execute(t, c);

                        //TODO check for answer to parent quest task
                        break;
                    case Symbols.BELIEF:
                        //check for answer to parent question task(s) and emit an event
                        Task parentTask = t.getParentTask();
                        if (parentTask!=null && parentTask.isQuestion()) {
                            eventAnswer.emit(Tuples.twin(parentTask, t));
                        }
                        break;
                }

                //3. signal any additional processes
                eventTaskProcess.emit(t);

            }

            return c;
        }

    };

    @NotNull
    public TaskPerception initInput() {

        return new SetTaskPerception(
                this,
                (t) -> processor.accept(t, 1f),
                //BudgetMerge.plusDQBlend
                BudgetMerge.avgDQBlend
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


        DefaultCycle c = new DefaultCycle(this, newDeriver(), pg, activeConcepts);

        //TODO move these to a PremiseGenerator which supplies
        c.termlinksFiredPerFiredConcept.set(termLinksPerConcept);
        c.tasklinksFiredPerFiredConcept.set(taskLinksPerConcept);

        c.conceptsFiredPerCycle.set(conceptsFirePerCycle);


        return c;
    }

    @NotNull
    public DefaultPremiseGenerator newPremiseGenerator() {
        return new DefaultPremiseGenerator(this, Deriver.getDefaultDeriver());
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
    public final float conceptPriority(Termed termed, float priIfNonExistent) {
        Concept cc = concept(termed);
        if (cc != null) {
            BLink<Concept> c = core.active.get(cc);
            if (c != null)
                return c.priIfFiniteElse(priIfNonExistent);
        }
        return priIfNonExistent;
    }


    @Override
    public Concept conceptualize(Termed termed, Budgeted activation, float scale) {


        Concept c = concept(termed, true);
        if (c != null) {

            core.activate(c, activation.budget(), scale);

            c.link(activation, scale, Default.this);

//            if (templateConcept != null) {
//                templateConcept.linkTask(task, subScale, minScale, nar);
//
//                linkTerm(templateConcept, taskBudget, subScale);
//
//                /** recursively activate the template's task tlink */
//
//            }



//            float toTermLinks = 0;
//            if (toTermLinks != 0) {
//                int numTermLinks = c.termlinks().size();
//                if (numTermLinks > 0) {
//                    float baseScale = toTermLinks * scale / numTermLinks; //minimum wage termlinks can receive
//
//                    List<Termed> l = c.termlinkTemplates();
//                    for (Termed bt : l) {
//
//                    }
//
//                    /*c.termlinks().forEach(bt -> {
//                        conceptualizeLink(activation, c, bt, baseScale);
//                    });*/
//
//                }
////                    float basePriIncrease = baseScale * activation.pri();
////                    //if (baseScale > Global.BUDGET_PROPAGATION_EPSILON) {
////
////
////                        final float[] priDemand = {0}, priSurplus = {0};
////                        c.termlinks().forEach(t -> {
////                            float p = t.pri();
////
////                            if (1f - p > 0)
////                                priDemand[0] += 1f-p;
////
////                            float pot = (p + basePriIncrease);
////                            float potentialSurplus = pot - 1f;
////                            if (potentialSurplus > 0)
////                                priSurplus[0] += potentialSurplus;
////                        });
////
////
////                        //float tlScale = toTermLinks * scale / numTLToActivate;
////
////                        System.out.println(c + " " + priDemand[0] + " " + priSurplus[0]);
////
////                                //numTermLinks - priPotential[0];
////                        //float tlScale = toTermLinks * scale * (1f + surplus * (1f - bt.pri())/priPotential[0])/numTLToActivate;
////                        //if (tlScale >= Global.BUDGET_PROPAGATION_EPSILON) {
////                        c.termlinks().forEach(bt -> {
////
////                            float s = baseScale +
////                                    (priSurplus[0] *
////                                        (1f - bt.pri()) / priDemand[0]); //share of the total demand
////                            System.out.println("  " + c + " " + s);
////                            conceptualizeLink(activation, toTermLinks, c, bt, s);
////                        });
////
////
////                }
////
////
//
//            }


        }
        return c;

    }
//
//    public void conceptualizeLink(Budget activation, Concept c, BLink<? extends Termed> bt, float s) {
//
//        if (s * activation.pri() > Global.BUDGET_PROPAGATION_EPSILON) {
//
//            Concept tc = conceptualize(bt.get(), activation, s);
//
//            if (tc != null) {
//                AbstractConcept.linkTerm(c, tc, activation, s, true, false);
//            }
//        }
//    }

    @NotNull
    @Override
    public NAR forEachConcept(Consumer<Concept> recip) {
        core.active.forEachKey(recip);
        return this;
    }

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

        public final Deriver der;

        /**
         * How many concepts to fire each cycle; measures degree of parallelism in each cycle
         */
        @NotNull
        @Range(min = 0, max = 64, unit = "Concept")
        public final MutableInteger conceptsFiredPerCycle;

        @Range(min = 0, max = 16, unit = "TaskLink") //TODO use float percentage
        public final MutableInteger tasklinksFiredPerFiredConcept = new MutableInteger(1);

        @Range(min = 0, max = 16, unit = "TermLink")
        public final MutableInteger termlinksFiredPerFiredConcept = new MutableInteger(1);

        @Range(min = 0.0f, max = 1.0, unit = "Percent")
        public final MutableFloat activationRate;

        @Range(min = 0.01f, max = 8, unit = "Duration")
        public final MutableFloat conceptRemembering;

        @Range(min = 0.01f, max = 8, unit = "Duration")
        public final MutableFloat termLinkRemembering;

        @Range(min = 0.01f, max = 8, unit = "Duration")
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

//        @Range(min=0,max=8192,unit="Concept")
//        public final MutableInteger capacity = new MutableInteger();


        @NotNull
        @Range(min = 0, max = 1f, unit = "Perfection")
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

            this.activationRate = nar.activationRate;
            this.conceptRemembering = nar.conceptForgetDurations;
            this.termLinkRemembering = nar.termLinkForgetDurations;
            this.taskLinkRemembering = nar.taskLinkForgetDurations;
            this.perfection = nar.perfection;

            conceptsFiredPerCycle = new MutableInteger(1);
            active = concepts;

            this.handlers = new Active(
                    nar.eventCycleEnd.on(this::cycle),
                    nar.eventReset.on(this::reset)
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

        /**
         * apply pending activity at the end of a cycle
         */
        private final void commit() {

            Bag<Concept> active = this.active;

            active.forEach(conceptForget); //TODO use downsampling % of concepts not TOP
            //active.printAll();

            active.commit();

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

            List<BLink<Concept>> f = this.firing;
            b.sample(conceptsToFire, f::add);

            int tasklinksToFire = tasklinksFiredPerFiredConcept.intValue();
            int termlnksToFire = termlinksFiredPerFiredConcept.intValue();
            Forget.AbstractForget<Termed> termLinkForget = this.termLinkForget;
            Forget.ForgetAndDetectDeletion<Task> taskLinkForget = this.taskLinkForget;

            PremiseGenerator pg = this.premiser;

            for (int i = 0, fSize = f.size(); i < fSize; i++) {
                BLink<Concept> nextConcept = f.get(i);
                if (nextConcept == null)
                    throw new NullPointerException();
                pg.firePremiseSquared(
                        nextConcept,
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


        public DefaultCycle(@NotNull NAR nar, Deriver deriver, PremiseGenerator premiseGenerator, int activeConcepts) {
            super(nar, deriver, newConceptBag(nar.random, activeConcepts), premiseGenerator);
        }


        @NotNull
        public static Bag<Concept> newConceptBag(Random r, int n) {
            return new CurveBag<Concept>(n, r)
                    //.mergePlus();
                    .merge(BudgetMerge.plusDQBlend);
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

        protected final Collection<Task> sharedResultBuffer;

        /**
         * re-used, not to be used outside of this
         */
        final @NotNull PremiseEval matcher;

        /**
         * derived tasks with truth confidence lower than this value are discarded.
         */
        @NotNull
        @Range(min = 0, max = 1f)
        public final MutableFloat confMin;

        public DefaultPremiseGenerator(@NotNull NAR nar, Deriver deriver) {
            /** the resutls buffer should probably be a Set because the derivations may duplicate */
            this(nar, deriver, Global.newHashSet(64));
        }

        public DefaultPremiseGenerator(@NotNull NAR nar, Deriver deriver, Collection<Task> resultsBuffer) {
            super(nar);
            this.matcher = new PremiseEval(nar.random, deriver);
            this.sharedResultBuffer = resultsBuffer;
            this.confMin = new MutableFloat(Global.TRUTH_EPSILON);

            nar.onFrame(this::updateDerivationParameters);
        }

        /**
         * update derivation parameters (each frame)
         */
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

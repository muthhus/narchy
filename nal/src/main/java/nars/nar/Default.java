package nars.nar;

import jcog.data.random.XorShift128PlusRandom;
import nars.NAR;
import nars.bag.Bag;
import nars.bag.CurveBag;
import nars.concept.Concept;
import nars.conceptualize.DefaultConceptBuilder;
import nars.control.ConceptBagControl;
import nars.derive.DefaultDeriver;
import nars.index.term.TermIndex;
import nars.index.term.map.MapTermIndex;
import nars.link.BLink;
import nars.op.stm.STMTemporalLinkage;
import nars.time.FrameTime;
import nars.time.Time;
import nars.util.exe.Executioner;
import nars.util.exe.SynchronousExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Random;

/**
 * Various extensions enabled
 */
public class Default extends NAR {

    //private static final Logger logger = LoggerFactory.getLogger(Default.class);

    public final ConceptBagControl core;

    private final STMTemporalLinkage stmLinkage = new STMTemporalLinkage(this, 2);


    @Deprecated
    public Default() {
        this(1024, 1, 1, 3);
    }

    public Default(int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept) {
        this(activeConcepts, conceptsFirePerCycle, taskLinksPerConcept, termLinksPerConcept, new XorShift128PlusRandom(1));
    }

    public static final int INDEX_TO_CORE_INITIAL_SIZE_RATIO = 8;

    public Default(int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept, @NotNull Random random) {
        this(activeConcepts, conceptsFirePerCycle, taskLinksPerConcept, termLinksPerConcept, random,
                new DefaultTermTermIndex(activeConcepts * INDEX_TO_CORE_INITIAL_SIZE_RATIO),
                new FrameTime());
    }


    public Default(int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept, @NotNull Random random, @NotNull TermIndex index, @NotNull Time time) {
        this(activeConcepts, conceptsFirePerCycle, taskLinksPerConcept, termLinksPerConcept, random, index, time, new SynchronousExecutor());
    }


    public Default(int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept, @NotNull Random random, @NotNull TermIndex concepts, @NotNull Time time, Executioner exe) {
        super(time, concepts, random, exe);

        core = new ConceptBagControl(this, new DefaultDeriver(), newConceptBag(activeConcepts));

        core.active.capacity(activeConcepts);
        core.termlinksFiredPerFiredConcept.set(1, termLinksPerConcept);
        core.tasklinksFiredPerFiredConcept.set(taskLinksPerConcept);
        core.conceptsFiredPerCycle.set(conceptsFirePerCycle);

        setControl(this.core);
    }

    protected Bag<Concept,BLink<Concept>> newConceptBag(int activeConcepts) {


        return
                new CurveBag<Concept>(activeConcepts, ((DefaultConceptBuilder) concepts.conceptBuilder()).defaultCurveSampler, ConceptBagControl.CONCEPT_BAG_BLEND,
                this.exe.concurrent() ? new java.util.concurrent.ConcurrentHashMap<>() : new HashMap());

                //new HijackBag<>(8192, 8, BudgetMerge.maxBlend, nar.random )

    }


    /**
     * suitable for single-thread, testing use only. provides no limitations on size so it will grow unbounded. use with caution
     */
    public static class DefaultTermTermIndex extends MapTermIndex {

        public DefaultTermTermIndex(int capacity) {
            super(
                    new DefaultConceptBuilder(),
                    new HashMap<>(capacity),
                    new HashMap<>(capacity)
                    //new ConcurrentHashMap<>(capacity),
                    //new ConcurrentHashMap<>(capacity)
                    //new ConcurrentHashMapUnsafe(capacity)
            );
        }
    }




}

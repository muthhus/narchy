package nars.nar;

import jcog.data.random.XorShift128PlusRandom;
import nars.NAR;
import nars.concept.Concept;
import nars.index.term.TermIndex;
import nars.index.term.map.MapTermIndex;
import nars.link.BLink;
import nars.op.time.STMTemporalLinkage;
import nars.reason.ConceptBagReasoner;
import nars.reason.DefaultConceptBuilder;
import nars.reason.DefaultDeriver;
import nars.term.Termed;
import nars.time.FrameTime;
import nars.time.Time;
import nars.util.exe.Executioner;
import nars.util.exe.SynchronousExecutor;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Random;

/**
 * Various extensions enabled
 */
public class Default extends NAR {

    //private static final Logger logger = LoggerFactory.getLogger(Default.class);

    public final ConceptBagReasoner core = new ConceptBagReasoner(this, new DefaultDeriver());

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


    public Default(int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept, @NotNull Random random, @NotNull TermIndex index, @NotNull Time time, Executioner exe) {
        super(time, index, random, exe);

        core.active.capacity(activeConcepts);
        core.termlinksFiredPerFiredConcept.set(1, termLinksPerConcept);
        core.tasklinksFiredPerFiredConcept.set(taskLinksPerConcept);
        core.conceptsFiredPerCycle.set(conceptsFirePerCycle);

        setControl(this.core);
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

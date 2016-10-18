package nars.nar;

import nars.NAR;
import nars.Param;
import nars.budget.Budgeted;
import nars.concept.Concept;
import nars.index.term.TermIndex;
import nars.index.term.map.MapTermIndex;
import nars.link.BLink;
import nars.nal.nal8.AbstractOperator;
import nars.nar.exe.Executioner;
import nars.nar.exe.SingleThreadExecutioner;
import nars.nar.util.ConceptBagCycle;
import nars.nar.util.DefaultConceptBuilder;
import nars.op.time.STMTemporalLinkage;
import nars.term.Term;
import nars.term.Termed;
import nars.time.Clock;
import nars.time.FrameClock;
import nars.util.data.random.XorShift128PlusRandom;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Various extensions enabled
 */
public class Default extends AbstractNAR {

    //private static final Logger logger = LoggerFactory.getLogger(Default.class);

    public final @NotNull ConceptBagCycle core;


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
                new DefaultTermTermIndex(activeConcepts * INDEX_TO_CORE_INITIAL_SIZE_RATIO, random),
                new FrameClock());
    }


    public Default(int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept, @NotNull Random random, @NotNull TermIndex index, @NotNull Clock clock) {
        this(activeConcepts, conceptsFirePerCycle, taskLinksPerConcept, termLinksPerConcept, random, index, clock, new SingleThreadExecutioner());
    }

    public Default(int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept, @NotNull Random random, @NotNull TermIndex index, @NotNull Clock clock, Executioner exe) {
        super(clock,
                index,
                random,
                Param.defaultSelf(), exe);


        ConceptBagCycle c = new ConceptBagCycle(this, activeConcepts);

        c.termlinksFiredPerFiredConcept.set(termLinksPerConcept);
        c.tasklinksFiredPerFiredConcept.set(taskLinksPerConcept);
        c.conceptsFiredPerCycle.set(conceptsFirePerCycle);

        this.core = c;


        int level = level();

        if (level >= 7) {

            initNAL7();

            if (level >= 8) {

                initNAL8();

            }

        }

    }

    private STMTemporalLinkage stmLinkage = null;

    /** NAL7 plugins */
    protected void initNAL7() {

        stmLinkage = new STMTemporalLinkage(this, 2);

    }

    /* NAL8 plugins */
    protected void initNAL8() {
        for (AbstractOperator o : defaultOperators)
            onExec(o);
    }


    @Override
    public final Concept concept(Term term, float boost) {
        return core.concepts.boost(term, boost);
    }

    @Override
    public final void activationAdd(ObjectFloatHashMap<Concept> concepts, Budgeted in, float activation, MutableFloat overflow) {
        core.activate(concepts, in, activation, overflow);
    }

    @Override
    public final float activation(@NotNull Termed concept) {
        BLink<Concept> c = core.concepts.get(concept);
        return c != null ? c.priIfFiniteElseZero() : 0;
    }


//    @Nullable
//    @Override
//    public final Concept activate(@NotNull Termed termed, Activation activation) {
//        Concept c = concept(termed, true);
//        if (c != null)
//            core.conceptualize(c, null, Float.NaN, Float.NaN, activation);
//        return c;
//    }


    @NotNull
    @Override
    public NAR forEachActiveConcept(@NotNull Consumer<Concept> recip) {
        core.concepts.forEachKey(recip);
        return this;
    }

    @Override
    public void clear() {
        //TODO use a 'clear' event handler that these can attach to

        core.concepts.clear();

        if (stmLinkage!=null)
            stmLinkage.clear();

    }

    /**
     * suitable for single-thread, testing use only. provides no limitations on size so it will grow unbounded. use with caution
     */
    public static class DefaultTermTermIndex extends MapTermIndex {

        public DefaultTermTermIndex(int capacity, @NotNull Random random) {
            super(
                    new DefaultConceptBuilder(random),
                    new HashMap(capacity),
                    new HashMap(capacity*2)
                    //new ConcurrentHashMap<>(capacity),
                    //new ConcurrentHashMap<>(capacity)
                    //new ConcurrentHashMapUnsafe(capacity)
            );
        }
    }



}

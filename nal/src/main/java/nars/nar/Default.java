package nars.nar;

import nars.Global;
import nars.NAR;
import nars.budget.Budgeted;
import nars.concept.Concept;
import nars.index.Indexes;
import nars.index.TermIndex;
import nars.link.BLink;
import nars.nal.meta.PremiseEval;
import nars.nar.util.DefaultCore;
import nars.term.Termed;
import nars.time.Clock;
import nars.time.FrameClock;
import nars.util.data.random.XorShift128PlusRandom;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Consumer;

/**
 * Various extensions enabled
 */
public class Default extends AbstractNAR {

    //private static final Logger logger = LoggerFactory.getLogger(Default.class);

    public final @NotNull DefaultCore core;

    public final @NotNull PremiseEval matcher;

    @Deprecated
    public Default() {
        this(1024, 1, 1, 3);
    }

    public Default(int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept) {
        this(activeConcepts, conceptsFirePerCycle, taskLinksPerConcept, termLinksPerConcept, new XorShift128PlusRandom(1));
    }

    public Default(int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept, @NotNull Random random) {
        this(activeConcepts, conceptsFirePerCycle, taskLinksPerConcept, termLinksPerConcept, random,
                new Indexes.DefaultTermIndex(activeConcepts * INDEX_TO_CORE_INITIAL_SIZE_RATIO, random),
                //new CaffeineIndex(new DefaultConceptBuilder(random)),
                new FrameClock());
    }


    public Default(int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept, @NotNull Random random, TermIndex index, @NotNull Clock clock) {
        super(clock,
                index,
                random,
                Global.DEFAULT_SELF);

        the("reasoner", matcher = new PremiseEval(random, newDeriver()));

        the("core", core = newCore(
                activeConcepts,
                conceptsFirePerCycle,
                termLinksPerConcept, taskLinksPerConcept,
                matcher
        ));

        if (nal() >= 7) {
            initNAL7();
            if (nal() >= 8) {
                initNAL8();
//                if (nal() >= 9) {
//                    initNAL9();
//                }
            }
        }

    }


    protected @NotNull DefaultCore newCore(int activeConcepts, int conceptsFirePerCycle, int termLinksPerConcept, int taskLinksPerConcept, PremiseEval matcher) {

        DefaultCore c = new DefaultCore(this, matcher, conceptWarm, conceptCold);
        c.concepts.setCapacity(activeConcepts);

        //TODO move these to a PremiseGenerator which supplies
        c.termlinksFiredPerFiredConcept.set(termLinksPerConcept);
        c.tasklinksFiredPerFiredConcept.set(taskLinksPerConcept);

        c.conceptsFiredPerCycle.set(conceptsFirePerCycle);

        //this.handlers = new Active(
        eventFrameStart.on(c::frame);
        eventReset.on(c::reset);
        //);
        return c;
    }

    @Override
    public final float conceptPriority(@NotNull Termed termed) {
        //if (termed!=null) {
            //Concept cc = concept(termed);
            //if (cc != null) {
                BLink<Concept> c = core.concepts.get(termed);
                if (c != null)
                    return c.priIfFiniteElseZero();
            //}
        //}
        return 0;
    }


    @Nullable
    @Override
    public final Concept conceptualize(@NotNull Termed termed, @NotNull Budgeted b, float conceptActivation, float linkActivation, @Nullable MutableFloat conceptOverflow) {
        Concept c = concept(termed, true);
        if (c != null)
            core.conceptualize(c, b, conceptActivation, linkActivation, conceptOverflow);
        return c;
    }


    @NotNull
    @Override
    public NAR forEachConcept(@NotNull Consumer<Concept> recip) {
        core.concepts.forEachKey(recip);
        return this;
    }


}

package nars.nar;

import nars.NAR;
import nars.Param;
import nars.budget.Budgeted;
import nars.concept.Concept;
import nars.index.Indexes;
import nars.index.TermIndex;
import nars.link.BLink;
import nars.nal.meta.PremiseEval;
import nars.nar.util.DefaultCore;
import nars.term.Term;
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


    public Default(int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept, @NotNull Random random, @NotNull TermIndex index, @NotNull Clock clock) {
        super(clock,
                index,
                random,
                Param.DEFAULT_SELF);

        the("reasoner", matcher = newMatcher());

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


    protected @NotNull DefaultCore newCore(int activeConcepts, int conceptsFirePerCycle, int termLinksPerConcept, int taskLinksPerConcept, @NotNull PremiseEval matcher) {

        DefaultCore c = new DefaultCore(this, matcher);
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
        BLink<Concept> c = core.concepts.get(termed);
        return c != null ? c.priIfFiniteElseZero() : 0;
    }


    @Nullable
    @Override
    public final Concept activate(@NotNull Termed termed, @NotNull Budgeted b, float conceptActivation, float linkActivation, @Nullable MutableFloat conceptOverflow) {
        Concept c = concept(termed, true);
        if (c != null)
            core.conceptualize(c, b, conceptActivation, linkActivation, conceptOverflow);
        return c;
    }


    @NotNull
    @Override
    public NAR forEachActiveConcept(@NotNull Consumer<Concept> recip) {
        core.concepts.forEachKey(recip);
        return this;
    }

    @Override
    public void clear() {
        core.concepts.clear();
    }

    /** faster access from active concept bag than the index
     * TODO faster access by providing a Concept instance, instead of its key.
     * this indicates the term is already known to be a key and does not need
     * atemporalized etc
     * */
    @Nullable @Override public final Concept concept(@NotNull Term t, boolean createIfMissing) {
        @Nullable BLink<Concept> activeLink = core.concepts.get(t);
        if (activeLink!=null) {
            return activeLink.get();
        }
        return super.concept(t, createIfMissing);
    }
}

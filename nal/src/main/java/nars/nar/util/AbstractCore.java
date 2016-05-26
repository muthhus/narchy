package nars.nar.util;

import nars.Memory;
import nars.NAR;
import nars.bag.BLink;
import nars.bag.Bag;
import nars.bag.impl.ArrayBag;
import nars.bag.impl.AutoBag;
import nars.budget.Budgeted;
import nars.budget.forget.BudgetForget;
import nars.budget.forget.Forget;
import nars.concept.Concept;
import nars.concept.Reasoner;
import nars.data.Range;
import nars.nar.Default;
import nars.task.Task;
import nars.term.Termed;
import nars.util.data.MutableInteger;
import nars.util.event.Active;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The original deterministic memory cycle implementation that is currently used as a standard
 * for development and testing.
 */
public abstract class AbstractCore {

    final @NotNull Active handlers;


    /**
     * How many concepts to fire each cycle; measures degree of parallelism in each cycle
     */
    @Range(min = 0, max = 64, unit = "Concept")
    public final @NotNull MutableInteger conceptsFiredPerCycle;



    @Range(min = 0.01f, max = 8, unit = "Duration")
    public final MutableFloat conceptRemembering;




    public final Forget.ExpForget taskLinkForget;

    public final @NotNull BudgetForget termLinkForget;


    //public final MutableFloat activationFactor = new MutableFloat(1.0f);

//        final Function<Task, Task> derivationPostProcess = d -> {
//            return LimitDerivationPriority.limitDerivation(d);
//        };


    /**
     * concepts active in this cycle
     */
    public final Bag<Concept> active;

    @Deprecated
    public final transient @NotNull NAR nar;

//        @Range(min=0,max=8192,unit="Concept")
//        public final MutableInteger capacity = new MutableInteger();


//        @NotNull
//        @Deprecated @Range(min = 0, max = 1f, unit = "Perfection")
//        public final MutableFloat perfection;





    public final @NotNull Reasoner premiser;
    private final AutoBag<Concept> activeAuto;

    private float cyclesPerFrame;
    private int cycleNum;

//        @Deprecated
//        int tasklinks = 2; //TODO use MutableInteger for this
//        @Deprecated
//        int termlinks = 3; //TODO use MutableInteger for this


    protected AbstractCore(@NotNull NAR nar, Reasoner reasoner) {

        this.nar = nar;

        this.premiser = reasoner;

        this.conceptRemembering = nar.conceptRemembering;

        this.conceptsFiredPerCycle = new MutableInteger(1);
        this.active = newConceptBag();
        this.activeAuto = new AutoBag<>((ArrayBag<Concept>) active);

        this.handlers = new Active(
                nar.eventFrameStart.on(this::frame),
                nar.eventCycleEnd.on(this::cycle),
                nar.eventReset.on(this::reset)
        );

        this.termLinkForget = new Forget.ExpForget(nar.termLinkRemembering, nar.perfection);
        this.taskLinkForget = new Forget.ExpForget(nar.taskLinkRemembering, nar.perfection);

    }

    protected abstract Bag<Concept> newConceptBag();

    protected void frame(@NotNull NAR nar) {
        cyclesPerFrame = nar.cyclesPerFrame.floatValue();
        cycleNum = 0;

        taskLinkForget.update(nar);
        termLinkForget.update(nar);

        premiser.frame(nar);
    }

    protected final void cycle(Memory memory) {

        float subCycle = cycleNum++ / cyclesPerFrame;

        termLinkForget.cycle(subCycle);
        taskLinkForget.cycle(subCycle);

        //active.forEach(conceptForget); //TODO use downsampling % of concepts not TOP
        //active.printAll();

        activeAuto.autocommit(nar);

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
    protected final void activate(@NotNull Concept c, @NotNull Budgeted b, float scale, @Nullable MutableFloat overflowing) {
        active.put(c, b, scale, overflowing);
    }

    public void conceptualize(Concept c, Budgeted b, float conceptActivation, float linkActivation, MutableFloat conceptOverflow) {
            activate(c, b, conceptActivation, conceptOverflow);
            if (linkActivation > 0)
                c.link(b, linkActivation, nar, conceptOverflow);
    }


    //try to implement some other way, this is here because of serializability

}

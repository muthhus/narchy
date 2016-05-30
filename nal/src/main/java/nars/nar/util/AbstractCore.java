package nars.nar.util;

import nars.Memory;
import nars.NAR;
import nars.bag.Bag;
import nars.bag.impl.AutoBag;
import nars.budget.Budgeted;
import nars.concept.Concept;
import nars.data.Range;
import nars.link.BLink;
import nars.nal.Reasoner;
import nars.task.Task;
import nars.term.Termed;
import nars.util.data.MutableInteger;
import nars.util.event.Active;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

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





    public final @NotNull AutoBag<Task> tasklinkUpdate;

    public final @NotNull AutoBag<Termed> termlinkUpdate;


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


    @Range(min = 0, max = 16, unit = "TaskLink") //TODO use float percentage
    public final MutableInteger tasklinksFiredPerFiredConcept = new MutableInteger(1);

    @Range(min = 0, max = 16, unit = "TermLink")
    public final MutableInteger termlinksFiredPerFiredConcept = new MutableInteger(1);




    public final @NotNull Reasoner reasoner;
    private final AutoBag<Concept> conceptUpdate;

    private float cyclesPerFrame;
    private int cycleNum;

//        @Deprecated
//        int tasklinks = 2; //TODO use MutableInteger for this
//        @Deprecated
//        int termlinks = 3; //TODO use MutableInteger for this


    protected AbstractCore(@NotNull NAR nar, Reasoner reasoner) {

        this.nar = nar;

        this.reasoner = reasoner;


        this.conceptsFiredPerCycle = new MutableInteger(1);

        this.active = newConceptBag();
        this.conceptUpdate = new AutoBag<>(nar.perfection);

        this.handlers = new Active(
                nar.eventFrameStart.on(this::frame),
                nar.eventCycleEnd.on(this::cycle),
                nar.eventReset.on(this::reset)
        );

        this.termlinkUpdate = new AutoBag(nar.perfection);
        this.tasklinkUpdate = new AutoBag(nar.perfection);

    }

    protected abstract Bag<Concept> newConceptBag();

    protected void frame(@NotNull NAR nar) {
        cyclesPerFrame = nar.cyclesPerFrame.floatValue();
        cycleNum = 0;

        tasklinkUpdate.update(nar);
        termlinkUpdate.update(nar);
        conceptUpdate.update(nar);

        reasoner.frame(nar);
    }

    protected final void cycle(Memory memory) {

        float subCycle = cycleNum++ / cyclesPerFrame;

        termlinkUpdate.cycle(subCycle);
        tasklinkUpdate.cycle(subCycle);
        conceptUpdate.cycle(subCycle);

        //active.forEach(conceptForget); //TODO use downsampling % of concepts not TOP
        //active.printAll();

        conceptUpdate.update(active, false);

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

        tasklinkUpdate.update(concept.tasklinks(), true);
        termlinkUpdate.update(concept.termlinks(), false);

        reasoner.firePremiseSquared(
                conceptLink,
                tasklinksFiredPerFiredConcept.intValue(),
                termlinksFiredPerFiredConcept.intValue()
        );
    }


    public void conceptualize(Concept c, Budgeted b, float conceptActivation, float linkActivation, MutableFloat conceptOverflow) {
        active.put(c, b, conceptActivation, conceptOverflow);
        if (b.isDeleted())
            throw new RuntimeException("Concept rejected: " + b);
        if (linkActivation > 0)
            c.link(b, linkActivation, nar, conceptOverflow);
    }


    //try to implement some other way, this is here because of serializability

}

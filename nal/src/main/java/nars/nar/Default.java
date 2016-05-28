package nars.nar;

import nars.Global;
import nars.NAR;
import nars.bag.BLink;
import nars.budget.Budgeted;
import nars.concept.Concept;
import nars.index.Indexes;
import nars.nal.Deriver;
import nars.nal.Reasoner;
import nars.nar.util.DefaultCore;
import nars.nar.util.DefaultReasoner;
import nars.task.Task;
import nars.term.TermIndex;
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

    public final @NotNull Reasoner reasoner;

    @Deprecated
    public Default() {
        this(1024, 1, 1, 3);
    }

    public Default(int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept) {
        this(activeConcepts, conceptsFirePerCycle, taskLinksPerConcept, termLinksPerConcept, new XorShift128PlusRandom(1));
    }

    public Default(int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept, @NotNull Random random) {
        this(activeConcepts, conceptsFirePerCycle, taskLinksPerConcept, termLinksPerConcept, random,
                new Indexes.DefaultTermIndex(activeConcepts * 4, random), new FrameClock());
    }


    public Default(int activeConcepts, int conceptsFirePerCycle, int taskLinksPerConcept, int termLinksPerConcept, @NotNull Random random, TermIndex index, @NotNull Clock clock) {
        super(clock,
                index,
                random,
                Global.DEFAULT_SELF);


        the("reasoner", reasoner = newReasoner());


        the("core", core = newCore(
                activeConcepts,
                conceptsFirePerCycle,
                termLinksPerConcept, taskLinksPerConcept,
                reasoner
        ));

        runLater(this::initHigherNAL);


        //new QueryVariableExhaustiveResults(this.memory());

        /*
        the("memory_sharpen", new BagForgettingEnhancer(memory, core.active));
        */

    }


    /**
     * process a Task through its Concept
     */
    @Nullable @Override
    public final Concept process(@NotNull Task input, float activation) {

        Concept c = concept(input, true);
        if (c == null) {

            input.delete("Inconceivable");


        /*if (Global.DEBUG_DERIVATION_STACKTRACES && Global.DEBUG_TASK_LOG)
            task.log(Premise.getStack());*/

            //eventTaskRemoved.emit(task);

        /* else: a more destructive cleanup of the discarded task? */

            return null;
        }

        float business = input.pri() * activation;
        emotion.busy(business);


        Task t = c.process(input, this);
        if (t != null && !t.isDeleted()) {
            //TaskProcess succeeded in affecting its concept's state (ex: not a duplicate belief)

            t.onConcept(c);

            //propagate budget
            MutableFloat overflow = new MutableFloat();

            conceptualize(c, t, activation, activation, overflow);

            if (overflow.floatValue() > 0) {
                emotion.stress(overflow.floatValue());
            }

            eventTaskProcess.emit(t); //signal any additional processes

        } else {
            emotion.frustration(business);
        }

        return c;
    }


    protected @NotNull DefaultCore newCore(int activeConcepts, int conceptsFirePerCycle, int termLinksPerConcept, int taskLinksPerConcept, Reasoner pg) {

        DefaultCore c = new DefaultCore(this, pg, conceptWarm, conceptCold);
        c.active.setCapacity(activeConcepts);

        //TODO move these to a PremiseGenerator which supplies
        c.termlinksFiredPerFiredConcept.set(termLinksPerConcept);
        c.tasklinksFiredPerFiredConcept.set(taskLinksPerConcept);

        c.conceptsFiredPerCycle.set(conceptsFirePerCycle);

        return c;
    }

    protected @NotNull Reasoner newReasoner() {
        return new DefaultReasoner(this, newDeriver());
    }

    protected @NotNull Deriver newDeriver() {
        return Deriver.getDefaultDeriver();
    }

    @Override
    public final float conceptPriority(@NotNull Termed termed) {
        if (termed!=null) {
            //Concept cc = concept(termed);
            //if (cc != null) {
                BLink<Concept> c = core.active.get(termed);
                if (c != null)
                    return c.priIfFiniteElseZero();
            //}
        }
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
        core.active.forEachKey(recip);
        return this;
    }


}

package nars.nar.util;

import nars.$;
import nars.Memory;
import nars.NAR;
import nars.bag.Bag;
import nars.bag.impl.AutoBag;
import nars.budget.Budgeted;
import nars.concept.Concept;
import nars.data.Range;
import nars.link.BLink;
import nars.nal.PremiseBuilder;
import nars.nal.meta.PremiseEval;
import nars.task.Task;
import nars.term.Term;
import nars.util.data.MutableInteger;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The original deterministic memory cycle implementation that is currently used as a standard
 * for development and testing.
 */
public abstract class AbstractCore {
    /**
     * How many concepts to fire each cycle; measures degree of parallelism in each cycle
     */
    @Range(min = 0, max = 64, unit = "Concept")
    public final @NotNull MutableInteger conceptsFiredPerCycle;





    public final @NotNull AutoBag<Task> tasklinkUpdate;

    public final @NotNull AutoBag<Term> termlinkUpdate;


    //public final MutableFloat activationFactor = new MutableFloat(1.0f);

//        final Function<Task, Task> derivationPostProcess = d -> {
//            return LimitDerivationPriority.limitDerivation(d);
//        };


    /**
     * concepts active in this cycle
     */
    @NotNull
    public final Bag<Concept> concepts;

    @Deprecated
    public final transient @NotNull NAR nar;


    @Range(min = 0, max = 16, unit = "TaskLink") //TODO use float percentage
    public final MutableInteger tasklinksFiredPerFiredConcept = new MutableInteger(1);

    @Range(min = 0, max = 16, unit = "TermLink")
    public final MutableInteger termlinksFiredPerFiredConcept = new MutableInteger(1);



    @NotNull private final AutoBag<Concept> conceptUpdate;

    @NotNull private final PremiseEval matcher;

    /**
     * temporary re-usable array for batch firing
     */
    transient private final List<Term> terms = $.newArrayList();
    transient private final List<Task> tasks = $.newArrayList();

    protected final boolean queueTaskLink(BLink<Task> b) {
        tasks.add(b.get());
        return true;
    }
    protected final boolean queueTermLink(BLink<Term> b) {
        terms.add(b.get());
        return true;
    }

    protected AbstractCore(@NotNull NAR nar, @NotNull PremiseEval matcher) {

        this.nar = nar;

        this.matcher = matcher;


        this.conceptsFiredPerCycle = new MutableInteger(1);

        this.concepts = newConceptBag();
        this.conceptUpdate = new AutoBag<>();


        this.termlinkUpdate = new AutoBag();
        this.tasklinkUpdate = new AutoBag();

    }

    @NotNull
    protected abstract Bag<Concept> newConceptBag();

    public void frame(@NotNull NAR nar) {

        tasklinkUpdate.update(nar);
        termlinkUpdate.update(nar);
        conceptUpdate.update(nar);


        run(nar.cyclesPerFrame.intValue());
    }

    protected final void run(int cycles) {

        int cpf = conceptsFiredPerCycle.intValue();
        float dCycle = 1f / cycles;

        for (int cycleNum = 0; cycleNum < cycles; cycleNum++) {
            float subCycle = dCycle * cycleNum;

            termlinkUpdate.cycle(subCycle);
            tasklinkUpdate.cycle(subCycle);
            conceptUpdate.cycle(subCycle);

            conceptUpdate.commit(concepts);

            concepts.sample(cpf, this::fireConcept);
        }

    }

    public void reset(Memory m) {
        concepts.clear();
    }

    protected final boolean fireConcept(@NotNull BLink<Concept> conceptLink) {
        Concept concept = conceptLink.get();

        tasklinkUpdate.commit(concept.tasklinks());
        termlinkUpdate.commit(concept.termlinks());

        return firePremiseSquared(
                conceptLink,
                tasklinksFiredPerFiredConcept.intValue(),
                termlinksFiredPerFiredConcept.intValue()
        ) > 0;
    }

    /**
     * iteratively supplies a matrix of premises from the next N tasklinks and M termlinks
     * (recycles buffers, non-thread safe, one thread use this at a time)
     */
    public final int firePremiseSquared(@NotNull BLink<? extends Concept> conceptLink, int tasklinks, int termlinks) {

        Concept c = conceptLink.get();

        matcher.init(nar);

        List<Term> termsBuffer = this.terms;
        c.termlinks().sample(termlinks, this::queueTermLink);

        int count = 0;
        if (!termsBuffer.isEmpty()) {

            List<Task> tasksBuffer = this.tasks;

            c.tasklinks().sample(tasklinks, this::queueTaskLink);

            if (!tasksBuffer.isEmpty()) {
                for (int i = 0, tasksBufferSize = tasksBuffer.size(); i < tasksBufferSize; i++) {
                    count += PremiseBuilder.run(
                            nar,
                            c,
                            termsBuffer,
                            tasksBuffer.get(i),
                            matcher);

                }

                tasksBuffer.clear();
            }

            termsBuffer.clear();
        }


        return count;
    }



    public void conceptualize(@NotNull Concept c, @NotNull Budgeted b, float conceptActivation, float linkActivation, MutableFloat conceptOverflow) {

        concepts.put(c, b, conceptActivation, conceptOverflow);
        if (b.isDeleted())
            return;
            //throw new RuntimeException("Concept rejected: " + b);
        if (linkActivation > 0)
            c.link(b, linkActivation, nar, conceptOverflow);
    }


    //try to implement some other way, this is here because of serializability

}

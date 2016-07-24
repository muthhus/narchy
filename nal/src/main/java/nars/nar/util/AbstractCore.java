package nars.nar.util;

import com.gs.collections.impl.factory.Sets;
import com.gs.collections.impl.set.mutable.MultiReaderUnifiedSet;
import nars.$;
import nars.Memory;
import nars.NAR;
import nars.bag.Bag;
import nars.bag.impl.AutoBag;
import nars.budget.Budgeted;
import nars.concept.Concept;
import nars.data.Range;
import nars.link.BLink;
import nars.nal.Conclusion;
import nars.nal.Premise;
import nars.nal.PremiseBuilder;
import nars.nal.meta.PremiseEval;
import nars.task.Task;
import nars.term.Term;
import nars.util.data.MutableInteger;
import nars.util.data.list.FasterList;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * The original deterministic memory cycle implementation that is currently used as a standard
 * for development and testing.
 */
public abstract class AbstractCore implements BiConsumer<Premise, Conclusion> {
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
    transient private final FasterList<Term> terms = $.newArrayList();
    transient private final FasterList<Task> tasks = $.newArrayList();
    transient private final FasterList<Concept> qoncepts = $.newArrayList();

    private static final Logger logger = LoggerFactory.getLogger(AbstractCore.class);


    private final boolean queueConcept(BLink<Concept> b) {
        return qoncepts.addIfNotNull(b.get());
    }
    private final boolean queueTaskLink(BLink<Task> b) {
        return tasks.addIfNotNull(b.get());
    }
    private final boolean queueTermLink(BLink<Term> b) {
        return terms.addIfNotNull(b.get());
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


        int cycles = nar.cyclesPerFrame.intValue();

        int cpf = conceptsFiredPerCycle.intValue();
        float dCycle = 1f / cycles;

        for (int cycleNum = 0; cycleNum < cycles; cycleNum++) {
            float subCycle = dCycle * cycleNum;

            termlinkUpdate.cycle(subCycle);
            tasklinkUpdate.cycle(subCycle);
            conceptUpdate.cycle(subCycle);

            conceptUpdate.commit(concepts);

            concepts.sample(cpf, this::queueConcept);
            //nar.runLater(()->fireConcept(c));
            qoncepts.forEach(this::fireConcept);
            qoncepts.clear();

//            if (!pending.isEmpty()) {
//                //Util.time(logger, "processing " + pending.size() + " derivations", () -> {
//                    //this.nar.input(pending);
//                    nar.inputDrain(pending);
//                //});
//            }

        }

    }


    public void reset(Memory m) {
        concepts.clear();
    }

    protected final boolean fireConcept(@NotNull Concept concept) {
        //Concept concept = conceptLink.get();

        tasklinkUpdate.commit(concept.tasklinks());
        termlinkUpdate.commit(concept.termlinks());

        return firePremiseSquared(
                concept,
                tasklinksFiredPerFiredConcept.intValue(),
                termlinksFiredPerFiredConcept.intValue()
        ) > 0;
    }

    /**
     * iteratively supplies a matrix of premises from the next N tasklinks and M termlinks
     * (recycles buffers, non-thread safe, one thread use this at a time)
     */
    public final int firePremiseSquared(@NotNull Concept c, int tasklinks, int termlinks) {

        //Concept c = conceptLink.get();

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
                            matcher, this);

                }

                tasksBuffer.clear();
            }

            termsBuffer.clear();
        }


        return count;
    }



    public void conceptualize(@NotNull Concept c, @NotNull Budgeted b, float conceptActivation, float linkActivation, MutableFloat conceptOverflow) {

        concepts.put(c, b, conceptActivation, conceptOverflow);
        //if (b.isDeleted())
            //return;
            //throw new RuntimeException("Concept rejected: " + b);
        if (linkActivation > 0)
            c.link(b, linkActivation, nar, conceptOverflow);
    }


    @Override
    public void accept(Premise premise, Conclusion conclusion) {
        //HACK for now just collect all conclusion's tasks into the pending set
        //pending.addAll(conclusion.derive);
        nar.inputLater(conclusion.derive);
    }


    //try to implement some other way, this is here because of serializability

}

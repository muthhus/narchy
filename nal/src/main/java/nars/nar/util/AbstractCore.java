package nars.nar.util;

import nars.$;
import nars.Memory;
import nars.NAR;
import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.concept.Concept;
import nars.data.Range;
import nars.link.BLink;
import nars.nal.Deriver;
import nars.nal.PremiseBuilder;
import nars.nal.meta.PremiseEval;
import nars.task.Task;
import nars.term.Term;
import nars.util.data.MutableInteger;
import nars.util.data.list.FasterList;
import nars.util.data.random.XorShift128PlusRandom;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

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


    private static final ThreadLocal<@NotNull PremiseEval> matcher = ThreadLocal.withInitial(
            ()->new PremiseEval(new XorShift128PlusRandom((int)Thread.currentThread().getId()), Deriver.getDefaultDeriver())
    );

    /**
     * temporary re-usable array for batch firing
     */
    transient private final FasterList<Concept> qoncepts = $.newArrayList();

    private static final Logger logger = LoggerFactory.getLogger(AbstractCore.class);


    private final boolean queueConcept(BLink<Concept> b) {
        return qoncepts.addIfNotNull(b.get());
    }

    protected AbstractCore(@NotNull NAR nar, @NotNull PremiseEval matcher) {

        this.nar = nar;

//        this.matcher = new ThreadLocal<PremiseEval>() {
//            @Override
//            protected PremiseEval initialValue() {
//                return matcher;
//            }
//        };


        this.conceptsFiredPerCycle = new MutableInteger(1);

        this.concepts = newConceptBag();


    }

    @NotNull
    protected abstract Bag<Concept> newConceptBag();

    public void frame(@NotNull NAR nar) {


        int cycles = nar.cyclesPerFrame.intValue();

        int cpf = conceptsFiredPerCycle.intValue();
        float dCycle = 1f / cycles;

        for (int cycleNum = 0; cycleNum < cycles; cycleNum++) {
            float subCycle = dCycle * cycleNum;

            concepts.commit();

            concepts.sample(cpf, this::queueConcept);
            qoncepts.forEach((Concept c) -> nar.runLater(()->fireConcept(c)));
            //qoncepts.forEach(this::fireConcept);
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

        concept.tasklinks().commit();
        concept.termlinks().commit();

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


        Set<Task> cccc = $.newHashSet(16);


        int count = 0;
        FasterList<BLink<Term>> termsBuffer = $.newArrayList();;
        c.termlinks().sample(termlinks, termsBuffer::addIfNotNull);
        if (!termsBuffer.isEmpty()) {


            FasterList<BLink<Task>> tasksBuffer = $.newArrayList();;
            c.tasklinks().sample(tasklinks, tasksBuffer::addIfNotNull);

            if (!tasksBuffer.isEmpty()) {

                PremiseEval mm = matcher.get();
                mm.init(nar);

                for (int i = 0, tasksBufferSize = tasksBuffer.size(); i < tasksBufferSize; i++) {
                    count += PremiseBuilder.run(
                            nar,
                            termsBuffer,
                            tasksBuffer.get(i),
                            mm,
                            (premise,conclusion)->{
                                cccc.addAll(conclusion.derive);
                            });

                }

                tasksBuffer.clear();
            }

            termsBuffer.clear();
        }

        nar.inputLater(cccc);

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



    //try to implement some other way, this is here because of serializability

}

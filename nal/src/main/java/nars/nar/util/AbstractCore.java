package nars.nar.util;

import nars.$;
import nars.Memory;
import nars.NAR;
import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.concept.Concept;
import nars.data.Range;
import nars.link.BLink;
import nars.nal.Conclusion;
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
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

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

//
//    private static final Logger logger = LoggerFactory.getLogger(AbstractCore.class);


    private final boolean queueConcept(BLink<Concept> b) {
        @Nullable Concept c = b.get();
        if (c!=null) {
            nar.runLater(new FireConcept(c, nar,
                (short)tasklinksFiredPerFiredConcept.intValue(),
                (short)termlinksFiredPerFiredConcept.intValue())
            );
            return true;
        }
        return false;
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

        for (int cycleNum = 0; cycleNum < cycles; cycleNum++) {

            concepts.commit();

            concepts.sample(cpf, this::queueConcept);

        }

    }


    public void reset(Memory m) {
        concepts.clear();
    }

    /** shared combined conclusion */
    public static class FireConcept extends Conclusion implements Runnable, Supplier<Conclusion> {

        private final Concept concept;
        private final NAR nar;
        private final short tasklinks;
        private final short termlinks;

        public FireConcept(Concept c, NAR nar, short tasklinks, short termlinks) {
            super($.newHashSet(8 * tasklinks * termlinks));
            this.concept = c;
            this.nar = nar;
            this.tasklinks = tasklinks;
            this.termlinks = termlinks;
        }

        @Override
        public final Conclusion get() {
            return this;
        }

        @Override
        public void run() {
            //Concept concept = conceptLink.get();

            concept.tasklinks().commit();
            concept.termlinks().commit();

            firePremiseSquared(
                    concept,
                    tasklinks,
                    termlinks
            );

            nar.input(derive);

        }

        /**
         * iteratively supplies a matrix of premises from the next N tasklinks and M termlinks
         * (recycles buffers, non-thread safe, one thread use this at a time)
         */
        public final int firePremiseSquared(@NotNull Concept c, int tasklinks, int termlinks) {

            FasterList<BLink<Term>> termsBuffer = $.newArrayList(termlinks);
            c.termlinks().sample(termlinks, termsBuffer::addIfNotNull);
            int count = 0;
            if (!termsBuffer.isEmpty()) {


                FasterList<BLink<Task>> tasksBuffer = $.newArrayList(tasklinks);
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
                                this
                                );

                    }

                    tasksBuffer.clear();
                }

                termsBuffer.clear();
            }


            return count;
        }


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

package nars.control;

import jcog.bag.Bag;
import jcog.bag.PLink;
import jcog.bag.impl.HijackBag;
import jcog.data.FloatParam;
import jcog.data.MutableIntRange;
import jcog.data.MutableInteger;
import jcog.data.Range;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.bag.impl.TaskHijackBag;
import nars.budget.BudgetMerge;
import nars.concept.Concept;
import nars.derive.Deriver;
import nars.premise.MatrixPremiseBuilder;
import nars.task.DerivedTask;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReferenceArray;


abstract public class DefaultConceptBagControl extends ConceptBagControl {
    /**
     * How many concepts to fire each cycle; measures degree of parallelism in each cycle
     */
    @Range(min = 0, max = 64, unit = "Concept")
    public final @NotNull MutableInteger conceptsFiredPerCycle;
    /** size of each sampled concept batch that adds up to conceptsFiredPerCycle.
     *  reducing this value should provide finer-grained / higher-precision concept selection
     *  since results between batches can affect the next one.
     */
    public final @NotNull MutableInteger conceptsFiredPerBatch;
    public final MutableInteger tasklinksFiredPerFiredConcept = new MutableInteger(1);
    public final MutableIntRange termlinksFiredPerFiredConcept = new MutableIntRange(1, 1);
    public final MutableInteger derivationsInputPerCycle;


    /** directly inptus each result upon derive, for single-thread  */
    public static class DirectConceptBagControl extends DefaultConceptBagControl {

        public DirectConceptBagControl(@NotNull NAR nar, @NotNull Deriver deriver, @NotNull Bag<Concept, PLink<Concept>> conceptBag, MatrixPremiseBuilder premiseBuilder) {
            super(nar, deriver, conceptBag, premiseBuilder);
        }

        @Override
        protected void cycle() {
            new PremiseMatrix(derivationsInputPerCycle.intValue(), tasklinksFiredPerFiredConcept.intValue(), termlinksFiredPerFiredConcept).accept(nar);
        }

        @Override
        public void accept(DerivedTask derivedTask) {
            nar.input(derivedTask);
        }
    }

    /**
     * Multithread safe concept firer; uses Bag to buffer derivations before choosing some or all of them for input
     */
    public static class BufferedConceptBagControl extends DefaultConceptBagControl {

        /** pending derivations to be input after this cycle */
        final TaskHijackBag pending;

        public BufferedConceptBagControl(@NotNull NAR nar, @NotNull Deriver deriver, @NotNull Bag<Concept, PLink<Concept>> conceptBag, MatrixPremiseBuilder premiseBuilder) {
            super(nar, deriver, conceptBag, premiseBuilder);

            this.pending = new TaskHijackBag(3, BudgetMerge.maxBlend, nar.random);

            nar.onReset((n)->{
                pending.clear();
            });
        }

        @Override
        protected void cycle() {


            AtomicReferenceArray<Task> all = pending.reset();
            if (all!=null) {
                nar.input(HijackBag.stream(all));
            }

//        int toInput = pending.capacity() / 2;
//        for (int i = 0; i < toInput; i++) {
//            Task t = pending.pop();
//            if (t == null)
//                break;
//            nar.input(t);
//        }

            //update concept bag
            pending.capacity( derivationsInputPerCycle.intValue() );
            // * 1f/((float)Math.sqrt(active.capacity()))
            ;



            //float load = nar.exe.load();
            int cpf = Math.round(conceptsFiredPerCycle.floatValue());
            ///Math.round(conceptsFiredPerCycle.floatValue() * (1f - load));

            //int cbs = nar.exe.concurrent() ? conceptsFiredPerBatch.intValue() : cpf /* all of them in one go if non-concurrent */;
            int cbs = conceptsFiredPerBatch.intValue();

            //logger.info("firing {} concepts (exe load={})", cpf, load);

            while (cpf > 0) {

                int batchSize = Math.min(cpf, cbs);
                cpf -= cbs;

                //List<PLink<Concept>> toFire = $.newArrayList(batchSize);

                nar.runLater(
                    new PremiseMatrix(batchSize, tasklinksFiredPerFiredConcept.intValue(), termlinksFiredPerFiredConcept)
                );

            }


        }

        @Override
        public void accept(DerivedTask d) {
            pending.put(d);

//        if (nar.input(d)!=null) {
//            meter.hit();
//        } else {
//            meter.miss();
//        }
        }

    }

    public DefaultConceptBagControl(@NotNull  NAR nar, @NotNull  Deriver deriver, @NotNull  Bag<Concept, PLink<Concept>> conceptBag, MatrixPremiseBuilder premiseBuilder) {
        super(nar, deriver, conceptBag, premiseBuilder);

        this.conceptsFiredPerCycle = new MutableInteger(1);
        this.conceptsFiredPerBatch = new MutableInteger(1);
        this.derivationsInputPerCycle = new MutableInteger(Param.TASKS_INPUT_PER_CYCLE);


    }

}

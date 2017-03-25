package nars.control;

import jcog.Texts;
import jcog.bag.Bag;
import jcog.bag.PLink;
import jcog.bag.impl.HijackBag;
import jcog.data.FloatParam;
import jcog.data.MutableIntRange;
import jcog.data.MutableInteger;
import jcog.data.Range;
import jcog.meter.event.PeriodMeter;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.bag.impl.TaskHijackBag;
import nars.budget.BudgetMerge;
import nars.concept.Concept;
import nars.premise.MatrixPremiseBuilder;
import nars.task.DerivedTask;
import org.jetbrains.annotations.NotNull;

import java.util.List;
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

        public DirectConceptBagControl(@NotNull NAR nar, @NotNull Bag<Concept, PLink<Concept>> conceptBag, MatrixPremiseBuilder premiseBuilder) {
            super(nar, conceptBag, premiseBuilder);
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



        public BufferedConceptBagControl(@NotNull NAR nar, @NotNull Bag<Concept, PLink<Concept>> conceptBag, MatrixPremiseBuilder premiseBuilder) {
            super(nar, conceptBag, premiseBuilder);

            this.pending = new TaskHijackBag(3, BudgetMerge.maxBlend, nar.random) {
                @Override
                public float pri(@NotNull Task key) {
                    return (1f + key.priSafe(0)) * (1f + key.qua());
                }

//                @Override
//                public void onRemoved(@NotNull Task value) {
//                    System.out.println(value);
//                }
            };

            nar.onReset((n)->{
                pending.clear();
                active.clear();
            });
        }

        @Override
        protected void cycle() {


//            AtomicReferenceArray<Task> all = pending.reset();
//            if (all!=null) {
//                nar.input(HijackBag.stream(all));
//            }


            //update concept bag
            int inputsPerCycle = derivationsInputPerCycle.intValue();

            final List<Task> ready = $.newArrayList(inputsPerCycle);
            int input = pending.pop(inputsPerCycle, ready::add);
            //System.out.println(input + " (" + Texts.n2(100f * input/((float)inputsPerCycle)) + "%) load, " + pending.size() + " remain");

            nar.runLater(ready, nar::input, 16);
            //ready.clear();

            //pending.commit();

            pending.capacity( inputsPerCycle * 8 );
            // * 1f/((float)Math.sqrt(active.capacity()))

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

    public static class ThrottledConceptBagControl extends BufferedConceptBagControl {

        final static int WINDOW_SIZE = 16;
        final PeriodMeter timing = new PeriodMeter("", WINDOW_SIZE);

        public final FloatParam fps = new FloatParam();
        long lastCycle;

        public ThrottledConceptBagControl(@NotNull NAR nar, @NotNull Bag<Concept, PLink<Concept>> conceptBag, MatrixPremiseBuilder premiseBuilder, float fps) {
            super(nar, conceptBag, premiseBuilder);
            this.fps.setValue(fps);
            lastCycle = System.nanoTime();
        }

        public float actualFPS() {
            double meanNS = timing.mean();
            double fps = 1E9/meanNS;
            return (float)fps;
        }


        @Override
        protected void cycle() {
            super.cycle();
            long end = System.nanoTime();

            timing.hit(end-lastCycle);


            //System.out.println(this + " actualFPS = " + actualFPS()  + ", target=" + fps.floatValue());


            lastCycle = end;

        }

    }


        public DefaultConceptBagControl(@NotNull NAR nar, @NotNull Bag<Concept, PLink<Concept>> conceptBag, MatrixPremiseBuilder premiseBuilder) {
        super(nar, conceptBag, premiseBuilder);

        this.conceptsFiredPerCycle = new MutableInteger(1);
        this.conceptsFiredPerBatch = new MutableInteger(1);
        this.derivationsInputPerCycle = new MutableInteger(Param.TASKS_INPUT_PER_CYCLE);


    }

}

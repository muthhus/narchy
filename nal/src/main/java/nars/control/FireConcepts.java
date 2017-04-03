package nars.control;

import jcog.bag.BagFlow;
import jcog.bag.PLink;
import jcog.data.MutableIntRange;
import jcog.data.MutableInteger;
import jcog.data.Range;
import jcog.event.On;
import jcog.list.FasterList;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.bag.impl.TaskHijackBag;
import nars.budget.BLink;
import nars.budget.BudgetMerge;
import nars.concept.Concept;
import nars.premise.MatrixPremiseBuilder;
import nars.task.DerivedTask;
import nars.term.Term;
import nars.util.data.Mix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;


abstract public class FireConcepts implements Consumer<DerivedTask>, Runnable {


    final MatrixPremiseBuilder premiser;



    /**
     * How many concepts to fire each cycle; measures degree of parallelism in each cycle
     */
    @Range(min = 0, max = 64, unit = "Concept")
    public final @NotNull MutableInteger conceptsFiredPerCycle;
    /**
     * size of each sampled concept batch that adds up to conceptsFiredPerCycle.
     * reducing this value should provide finer-grained / higher-precision concept selection
     * since results between batches can affect the next one.
     */
    public final @NotNull MutableInteger conceptsFiredPerBatch;
    public final MutableIntRange termlinksFiredPerFiredConcept = new MutableIntRange(1, 1);
    public final MutableInteger derivationsInputPerCycle;
    protected final NAR nar;
    private final On on;

    class PremiseVectorBatch implements Consumer<BLink<Concept>>{

        public PremiseVectorBatch(int batchSize, NAR nar) {
            nar.focus().sample(batchSize, c -> {
                if (premiseVector(nar, c.get(), FireConcepts.this)) return true; //continue

                return true;
            });
        }

        @Override
        public void accept(BLink<Concept> conceptBLink) {

        }
    }

    public boolean premiseVector(NAR nar, Concept c, Consumer<DerivedTask> target) {

        c.tasklinks().commit();

        @Nullable BLink<Task> taskLink = c.tasklinks().sample();
        if (taskLink == null)
            return true;

        c.termlinks().commit();

        int termlinksPerForThisTask = termlinksFiredPerFiredConcept.lerp(taskLink.priSafe(0));

        FasterList<BLink<Term>> termLinks = new FasterList(termlinksPerForThisTask);
        c.termlinks().sample(termlinksPerForThisTask, termLinks::add);

        if (!termLinks.isEmpty())
            premiser.newPremiseVector(c, taskLink, termlinksFiredPerFiredConcept,
                    target, termLinks, nar);
        return false;
    }

    class PremiseMatrixBatch implements Consumer<NAR> {
        private final int _tasklinks;
        private final int batchSize;
        private final MutableIntRange _termlinks;

        public PremiseMatrixBatch(int batchSize, int _tasklinks, MutableIntRange _termlinks) {
            this.batchSize = batchSize;
            this._tasklinks = _tasklinks;
            this._termlinks = _termlinks;
        }

        @Override
        public void accept(NAR nar) {
            nar.focus().sample(batchSize, c -> {
                premiser.newPremiseMatrix(c.get(),
                        _tasklinks, _termlinks,
                        FireConcepts.this, //input them within the current thread here
                        nar
                );
                return true;
            });
        }

    }


//    /**
//     * directly inptus each result upon derive, for single-thread
//     */
//    public static class DirectConceptBagFocus extends FireConcepts {
//        public final MutableInteger tasklinksFiredPerFiredConcept = new MutableInteger(1);
//
//        public DirectConceptBagFocus(@NotNull NAR nar, @NotNull Bag<Concept, PLink<Concept>> conceptBag, MatrixPremiseBuilder premiseBuilder) {
//            super(nar, premiseBuilder);
//        }
//
//        @Override public void run() {
//            //new PremiseVector(derivationsInputPerCycle.intValue(), termlinksFiredPerFiredConcept).accept(nar);
//            new PremiseMatrixBatch(derivationsInputPerCycle.intValue(), tasklinksFiredPerFiredConcept.intValue(), termlinksFiredPerFiredConcept).accept(nar);
//        }
//
//        @Override
//        public void accept(DerivedTask derivedTask) {
//            nar.input(derivedTask);
//        }
//
//    }

    /**
     * Multithread safe concept firer; uses Bag to buffer derivations before choosing some or all of them for input
     */
    public static class FireConceptsBufferDerivations extends FireConcepts {

        /** flow from concept bag to derived task bag */
        final BagFlow flow;

        /**
         * pending derivations to be input after this cycle
         */
        final TaskHijackBag pending;
        private final Mix.MixStream in;


        public FireConceptsBufferDerivations(@NotNull NAR nar, @NotNull MatrixPremiseBuilder premiseBuilder) {
            super(nar, premiseBuilder);


            this.pending = new TaskHijackBag(3, BudgetMerge.maxBlend, nar.random) {


                //                @Override
//                public float pri(@NotNull Task key) {
//                    //return (1f + key.priSafe(0)) * (1f + key.qua());
//                    //return (1f + key.priSafe(0)) * (1f + key.qua());
//                }

//                @Override
//                public void onRemoved(@NotNull Task value) {
//                    System.out.println(value);
//                }
            };

            nar.onReset((n) -> {
                pending.clear();
            });

            this.in = nar.mix.stream("FireConcepts");

            this.flow = new BagFlow<PLink<Concept>,Task>(
                ((ConceptBagFocus)nar.focus()).active,
                pending,
                nar.exe, (concept, target) -> {
                    premiseVector(nar, concept.get(), target::put);
                    return true;
                },
            nar::input);

        }

        @Override
        public void run() {

            int inputsPerCycle = derivationsInputPerCycle.intValue();
            pending.capacity(inputsPerCycle * 8);
            pending.commit();
            this.flow.update();

////            AtomicReferenceArray<Task> all = pending.reset();
////            if (all!=null) {
////                nar.input(HijackBag.stream(all));
////            }
//
//
//            //update concept bag
//
//            final List<Task> ready = $.newArrayList(inputsPerCycle);
//            int input = pending.pop(inputsPerCycle, ready::add);
//            //System.out.println(input + " (" + Texts.n2(100f * input/((float)inputsPerCycle)) + "%) load, " + pending.size() + " remain");
//
//            if (!ready.isEmpty()) {
//                nar.runLater((n) -> {
//                    n.input(in.input(ready.stream()));
//                });
//            }
//
//            //nar.runLater(ready, nar::input, 32);
//            //ready.clear();
//
//            pending.commit();
//
//            // * 1f/((float)Math.sqrt(active.capacity()))
//
//            //float load = nar.exe.load();
//            int cpf = Math.round(conceptsFiredPerCycle.floatValue());
//            ///Math.round(conceptsFiredPerCycle.floatValue() * (1f - load));
//
//            //int cbs = nar.exe.concurrent() ? conceptsFiredPerBatch.intValue() : cpf /* all of them in one go if non-concurrent */;
//            int cbs = conceptsFiredPerBatch.intValue();
//
//            //logger.info("firing {} concepts (exe load={})", cpf, load);
//
//            while (cpf > 0) {
//
//                int batchSize = Math.min(cpf, cbs);
//                cpf -= cbs;
//
//                //List<PLink<Concept>> toFire = $.newArrayList(batchSize);
//
//                nar.runLater((n) ->
//                        new FireConcepts.PremiseVectorBatch(batchSize, n)
//                        //new PremiseMatrixBatch(batchSize, tasklinksFiredPerFiredConcept.intValue(), termlinksFiredPerFiredConcept)
//                );
//
//            }


        }

        @Override
        public void accept(DerivedTask d) {
            pending.put(d);
        }

    }




    public FireConcepts(@NotNull NAR nar, MatrixPremiseBuilder premiseBuilder) {

        this.nar = nar;
        this.premiser = premiseBuilder;

        this.conceptsFiredPerCycle = new MutableInteger(1);
        this.conceptsFiredPerBatch = new MutableInteger(1);
        this.derivationsInputPerCycle = new MutableInteger(Param.TASKS_INPUT_PER_CYCLE);


        this.on = nar.onCycle(this);
    }


}

package nars.control;

import jcog.bag.Bag;
import jcog.bag.control.BagFlow;
import jcog.data.MutableIntRange;
import jcog.data.MutableInteger;
import jcog.data.Range;
import jcog.event.On;
import jcog.pri.PLink;
import nars.Focus;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.bag.TaskHijackBag;
import nars.concept.Concept;
import nars.premise.Derivation;
import nars.premise.MatrixPremiseBuilder;
import nars.task.DerivedTask;
import nars.term.Term;
import nars.util.data.Mix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


/** controls an active focus of concepts */
abstract public class FireConcepts implements Consumer<DerivedTask>, Runnable {


    public final AtomicBoolean clear = new AtomicBoolean(false);

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
    public final Focus source;

//    class PremiseVectorBatch implements Consumer<BLink<Concept>>{
//
//        public PremiseVectorBatch(int batchSize, NAR nar) {
//            nar.focus().sample(batchSize, c -> {
//                if (premiseVector(nar, c.get(), FireConcepts.this)) return true; //continue
//
//                return true;
//            });
//        }
//
//        @Override
//        public void accept(BLink<Concept> conceptBLink) {
//
//        }
//    }

    int premiseVector(NAR nar, PLink<Concept> pc, Consumer<DerivedTask> target, int numPremises) {

        Concept c = pc.get();

        Bag<Task, PLink<Task>> taskLinks = c.tasklinks();
        taskLinks.commit();
        Bag<Term, PLink<Term>> termLinks = c.termlinks();
        termLinks.commit();

        long now = nar.time();
        int count = 0;

        for (int i = 0; i < numPremises; i++) {
            final @Nullable PLink<Task> taskLink = taskLinks.sample();
            if (taskLink == null)
                continue;

            PLink<Term> termLink = termLinks.sample();
            if (termLink==null)
                continue;

            Derivation d = premiser.premise(c, taskLink, termLink, now, nar, -1f, target);
            if (d != null) {
                premiser.deriver.accept(d);
                count++;
            }


//            int termlinksPerForThisTask = termlinksFiredPerFiredConcept
//                    .hi();
                    //.lerp( pc.pri()  );

//            FasterList<PLink<Term>> termLinks = new FasterList(termlinksPerForThisTask);
//            c.termlinks().sample(termlinksPerForThisTask, (h,v) -> {
//                termLinks.add(v);
//                return 1;
//            });


        }
        return count;
    }



    /**
     * directly inptus each result upon derive, for single-thread
     */
    public static class FireConceptsDirect extends FireConcepts {

        public FireConceptsDirect(@NotNull MatrixPremiseBuilder premiseBuilder, @NotNull NAR nar) {
            this(nar.focus(), premiseBuilder, nar);
        }

        public FireConceptsDirect(@NotNull Focus focus, @NotNull MatrixPremiseBuilder premiseBuilder, @NotNull NAR nar) {
            super(focus, premiseBuilder, nar);
        }

        @Override public void fire() {
            source.sample(conceptsFiredPerCycle.intValue(), (h,c) -> {
                return premiseVector(nar, c, nar::input, h);
            });
        }

        @Override
        public void accept(DerivedTask derivedTask) {
            nar.input(derivedTask);
        }

    }

    /**
     * Multithread safe concept firer; uses Bag to buffer derivations before choosing some or all of them for input
     */
    public static class FireConceptsBuffered extends FireConcepts {

        /** flow from concept bag to derived task bag */
        final BagFlow flow;

        /**
         * pending derivations to be input after this cycle
         */
        final TaskHijackBag pending;

        private final Mix.MixStream<Object,Task> in;

        public FireConceptsBuffered(@NotNull MatrixPremiseBuilder premiseBuilder, @NotNull NAR nar) {
            this(nar.focus(), premiseBuilder, nar);
        }

        public FireConceptsBuffered(@NotNull Focus focus, @NotNull MatrixPremiseBuilder premiseBuilder, @NotNull NAR nar) {
            super(focus, premiseBuilder, nar);


            this.pending = new TaskHijackBag(3) {

                @Override
                public float temperature() {
                    return 0.1f; //forget slowly
                }

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

            this.in = nar.mix.stream("Derive");

            this.flow = new BagFlow<PLink<Concept>,Task>(
                ((ConceptBagFocus)nar.focus()).active,
                pending,
                nar.exe, (concept, h, target) -> {
                    premiseVector(nar, concept, target::put, h);
                },
                x->nar.input(in.apply(x)));

        }

        @Override
        public void fire() {

            int inputsPerCycle = derivationsInputPerCycle.intValue();
            pending.capacity(inputsPerCycle * 4);
            pending.commit();
            this.flow.update(0.05f, 0.75f);


        }

        @Override
        public void accept(DerivedTask d) {
            pending.put(d);
        }

    }




    public FireConcepts(@NotNull Focus source, MatrixPremiseBuilder premiseBuilder, NAR nar) {

        this.nar = nar;
        this.source = source;
        this.premiser = premiseBuilder;

        this.conceptsFiredPerCycle = new MutableInteger(1);
        this.conceptsFiredPerBatch = new MutableInteger(1);
        this.derivationsInputPerCycle = new MutableInteger(Param.TASKS_INPUT_PER_CYCLE_MAX);


        this.on = nar.onCycle(this);
    }

    @Override
    public void run() {
        ConceptBagFocus f = (ConceptBagFocus) this.source;

        //while clear is enabled, keep active clear
        if (clear.get()) {
            f.active.clear();
            clear.set(false);
        } else {
            f.active.commit();
        }

        fire();
    }

    abstract protected void fire();
}
//    class PremiseMatrixBatch implements Consumer<NAR> {
//        private final int _tasklinks;
//        private final int batchSize;
//        private final MutableIntRange _termlinks;
//
//        public PremiseMatrixBatch(int batchSize, int _tasklinks, MutableIntRange _termlinks) {
//            this.batchSize = batchSize;
//            this._tasklinks = _tasklinks;
//            this._termlinks = _termlinks;
//        }
//
//        @Override
//        public void accept(NAR nar) {
//            source.sample(batchSize, c -> {
//                premiser.newPremiseMatrix(c.get(),
//                        _tasklinks, _termlinks,
//                        FireConcepts.this, //input them within the current thread here
//                        nar
//                );
//                return true;
//            });
//        }
//
//    }

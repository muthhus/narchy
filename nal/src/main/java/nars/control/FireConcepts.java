package nars.control;

import jcog.bag.Bag;
import jcog.data.FloatParam;
import jcog.data.MutableIntRange;
import jcog.event.On;
import jcog.pri.PLink;
import nars.Focus;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.premise.Derivation;
import nars.premise.MatrixPremiseBuilder;
import nars.task.DerivedTask;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


/** controls an active focus of concepts */
abstract public class FireConcepts implements Consumer<DerivedTask>, Runnable {


    public final AtomicBoolean clear = new AtomicBoolean(false);

    final MatrixPremiseBuilder premiser;



    /**
     *
     */
    public final @NotNull FloatParam rate = new FloatParam(1f);
    /**
     * size of each sampled concept batch that adds up to conceptsFiredPerCycle.
     * reducing this value should provide finer-grained / higher-precision concept selection
     * since results between batches can affect the next one.
     */
    public final MutableIntRange taskLinksFiredPerConcept = new MutableIntRange(1, 1);
    public final MutableIntRange termLinksFiredPerTaskLink = new MutableIntRange(1, 1);

//    public final MutableInteger derivationsInputPerCycle;
//    this.derivationsInputPerCycle = new MutableInteger(Param.TASKS_INPUT_PER_CYCLE_MAX);
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

    /** returns # of derivations processed */
    int premiseVector(NAR nar, PLink<Concept> pc, Consumer<DerivedTask> target) {

        Concept c = pc.get();
        float cPri = pc.priSafe(0);

        int numTaskLinks = taskLinksFiredPerConcept.lerp(cPri);

        Bag<Task, PLink<Task>> taskLinks = c.tasklinks().commit();
        numTaskLinks = Math.min(numTaskLinks, taskLinks.size());
        if (numTaskLinks==0)
            return 0;


        Bag<Term, PLink<Term>> termLinks = c.termlinks().commit();
        int availTermLinks = termLinks.size();
        if (availTermLinks==0)
            return 0;

        long now = nar.time();
        final int[] count = {0};

        taskLinks.sample(numTaskLinks, taskLink -> {

            int numTermLinks = Math.min(
                    availTermLinks,
                    termLinksFiredPerTaskLink.lerp(
                        cPri * taskLink.pri()
                    ));

            termLinks.sample(numTermLinks, termLink -> {

                Derivation d = premiser.premise(c, taskLink, termLink, now, nar, -1f, target);
                if (d != null) {
                    premiser.deriver.accept(d);
                    count[0]++;
                }


                //            int termlinksPerForThisTask = termlinksFiredPerFiredConcept
                //                    .hi();
                //.lerp( pc.pri()  );

                //            FasterList<PLink<Term>> termLinks = new FasterList(termlinksPerForThisTask);
                //            c.termlinks().sample(termlinksPerForThisTask, (h,v) -> {
                //                termLinks.add(v);
                //                return 1;
                //            });

            });
        });

        return count[0];
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
            ConceptBagFocus csrc = (ConceptBagFocus) source;
            int count = Math.min(csrc.active.size(), (int) Math.ceil(rate.floatValue() * csrc.active.capacity()));
            if (count == 0)
                return; //idle

            final int[] num = { count };
            source.sample((c) -> {
                int derivations = premiseVector(nar, c, nar::input);
                num[0]--;
                return (num[0] > 0) ? Bag.BagCursorAction.Next : Bag.BagCursorAction.Stop;
            });
        }

        @Override
        public void accept(DerivedTask derivedTask) {
            nar.input(derivedTask);
        }

    }

    public FireConcepts(@NotNull Focus source, MatrixPremiseBuilder premiseBuilder, NAR nar) {

        this.nar = nar;
        this.source = source;
        this.premiser = premiseBuilder;

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

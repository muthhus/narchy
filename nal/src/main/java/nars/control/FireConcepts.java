package nars.control;

import jcog.Util;
import jcog.data.FloatParam;
import jcog.data.MutableIntRange;
import jcog.data.MutableInteger;
import jcog.data.sorted.SortedArray;
import jcog.event.On;
import jcog.pri.PLink;
import jcog.pri.PriMerge;
import nars.Focus;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.derive.Deriver;
import nars.premise.Derivation;
import nars.premise.DerivationBudgeting;
import nars.premise.Premise;
import nars.premise.PremiseBuilder;
import nars.task.DerivedTask;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


/**
 * controls an active focus of concepts
 */
abstract public class FireConcepts implements Consumer<DerivedTask>, Runnable {


    public final AtomicBoolean clear = new AtomicBoolean(false);

    public final DerivationBudgeting budgeting;
    public final Deriver deriver;


    /**
     *
     */
    public final @NotNull FloatParam rate = new FloatParam(1f);

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

    /**
     * returns # of derivations processed
     */
    int premiseVector(PLink<Concept> pc, Derivation d) {

        float cPri = pc.priSafe(0);
        int ttl = Util.lerp(cPri, Param.UnificationTTL, Param.UnificationTTLMin);

        Concept c = pc.get();
        c.tasklinks().commit();
        c.termlinks().commit();
        nar.terms.commit(c);

        long now = nar.time();
        int count = 0;

        int premiseCost = Param.BeliefMatchTTL;
        int linkSampleCost = 1;

        Random rng = nar.random();

        @Nullable PLink<Task> tasklink = null;
        @Nullable PLink<Term> termlink = null;
        float taskPri = -1f, termPri = -1f;

        while (ttl > 0) {
            if (tasklink == null || rng.nextFloat() > taskPri) { //sample a new link inversely probabalistically in proportion to priority
                tasklink = c.tasklinks().sample();
                ttl -= linkSampleCost;
                if (tasklink == null)
                    break;

                taskPri = tasklink.priSafe(0);
                d.restart(tasklink.get());
            }


            if (termlink == null || rng.nextFloat() > termPri) { //sample a new link inversely probabalistically in proportion to priority
                termlink = c.termlinks().sample();
                ttl -= linkSampleCost;
                if (termlink == null)
                    break;
                termPri = termlink.priSafe(0);
            }

            if (ttl <= premiseCost)
                break; //not enough remaining to create premise

            Premise p = PremiseBuilder.premise(c, tasklink, termlink, now, nar, -1f);
            ttl -= premiseCost; //failure of premise generation still causes cost

            if (p != null) {

                int start = ttl;

                int ttlRemain = deriver.run(d, p, ttl);

                assert (start >= ttlRemain);

                count++;

                ttl -= (start - ttlRemain);
            }
        }


        return count;
    }

    /**
     * returns # of derivations processed
     */
    int premiseVector0(PLink<Concept> pc, Derivation d, MutableIntRange taskLinksFiredPerConcept, MutableIntRange termLinksFiredPerConcept) {

        Concept c = pc.get();
        float cPri = pc.priSafe(0);

        List<PLink<Task>> tasklinks = c.tasklinks().commit().sampleToList(taskLinksFiredPerConcept.lerp(cPri));
        if (tasklinks.isEmpty())
            return 0;

        List<PLink<Term>> termlinks = c.termlinks().commit().sampleToList(termLinksFiredPerConcept.lerp(cPri));
        if (termlinks.isEmpty())
            return 0;

        int count = 0;

        long now = nar.time();
        for (int i = 0, tasklinksSize = tasklinks.size(); i < tasklinksSize; i++) {
            PLink<Task> tasklink = tasklinks.get(i);

            float tlPri = tasklink.pri();

            for (int j = 0, termlinksSize = termlinks.size(); j < termlinksSize; j++) {
                PLink<Term> termlink = termlinks.get(j);

                Premise p = PremiseBuilder.premise(c, tasklink, termlink, now, nar, -1f);
                if (p != null) {

                    float invest = Util.or(tlPri, termlink.pri());
                    int ttl = Util.lerp(invest, Param.UnificationTTL, Param.UnificationTTLMin);

                    if (deriver.test(d.restart(p, ttl)))
                        count++;
                }
            }
        }

        return count;

    }


    /**
     * directly inptus each result upon derive, for single-thread
     */
    public static class FireConceptsDirect extends FireConcepts {

        private final ThreadLocal<FireConceptsDirect.MyDerivation> derivation =
                ThreadLocal.withInitial(()->
                    new MyDerivation(budgeting, nar)
                );

        public FireConceptsDirect(Deriver deriver, DerivationBudgeting budgeting, @NotNull NAR nar) {
            this(nar.focus(), deriver, budgeting, nar);
        }

        public FireConceptsDirect(Focus focus, Deriver deriver, DerivationBudgeting budgeting, @NotNull NAR nar) {
            super(focus, deriver, budgeting, nar);
        }

        @Override
        public void fire() {
            ConceptBagFocus csrc = (ConceptBagFocus) source;
            int count = /*Math.min(csrc.active.size(), */(int) Math.ceil(rate.floatValue() * csrc.active.capacity());
            if (count == 0)
                return; //idle

            if (nar.exe.concurrent()) {
                int remain = count;
                float granularity = 8;
                float batchDivisor = nar.exe.concurrency() * granularity;
                int batchSize = (int) Math.ceil(remain / batchDivisor);
                while (remain > 0) {
                    int nextBatchSize = Math.min(remain, batchSize);
                    nar.runLater(() -> {

                        fire(csrc, nextBatchSize);

                    });
                    remain -= nextBatchSize;
                }

            } else {
                fire(csrc, count);
            }
        }


        private static class MyDerivation extends Derivation {
            final Map<Task, Task> buffer = new LinkedHashMap();

            private final MutableInteger maxInputTasksPerDerivation = new MutableInteger(-1);

            public MyDerivation(DerivationBudgeting b, NAR nar) {
                super(nar, b, Param.UnificationStackMax);
            }

            @Override
            public void derive(Task x) {
                buffer.merge(x, x, (prev, next) -> {
                    PriMerge.max.merge(prev, next);
                    return prev;
                });
            }

            void commit(int derivations) {
                if (!buffer.isEmpty()) {
                    int mPerDeriv = maxInputTasksPerDerivation.intValue();
                    int max = mPerDeriv * derivations;
                    if (mPerDeriv == -1 || buffer.size() <= max) {
                        nar.input(buffer.values());
                    } else {
                        nar.input(top(buffer, max));
                    }


                    buffer.clear();
                }
            }
        }

        static Iterable<Task> top(Map<Task, Task> m, int max) {
            SortedArray<Task> sa = new SortedArray<>(new Task[max]);
            m.values().forEach(x -> {
                if (sa.size() >= max) {
                    if (sa.last().pri() > x.pri()) {
                        return; //too low priority
                    } else {
                        sa.removeLast(); //remove current last
                    }
                }
                sa.add(x, z -> -z.pri());
//                while (sa.size() > max)
//                    sa.removeLast();
            });
            assert (sa.size() <= max);
            return sa;
        }

        //long start = nanoTime();
        //long end = nanoTime();
        //double dt = (end - start) / ((float)nextBatchSize);
        //rate.hitNano(dt);

        public void fire(ConceptBagFocus csrc, int count) {
            MyDerivation d = derivation.get();

            final int[] derivations = {0};
            MyDerivation dd = d;
            csrc.active.sample(count, p -> {
                derivations[0] += premiseVector(p, dd);
            });
            d.commit(derivations[0]);
        }

        @Override
        public void accept(DerivedTask derivedTask) {
            nar.input(derivedTask);
        }

    }

    public FireConcepts(@NotNull Focus source, Deriver dderiver, DerivationBudgeting bbudgeting, NAR nar) {

        this.deriver = dderiver;
        this.budgeting = bbudgeting;

        this.nar = nar;
        this.source = source;

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

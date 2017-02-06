package nars.control;

import jcog.data.MutableInteger;
import jcog.data.random.XorShift128PlusRandom;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.attention.Activation;
import nars.bag.Bag;
import nars.bag.CurveBag;
import nars.budget.BudgetMerge;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.conceptualize.DefaultConceptBuilder;
import nars.conceptualize.state.ConceptState;
import nars.conceptualize.state.DefaultConceptState;
import nars.derive.DefaultDeriver;
import nars.derive.Deriver;
import nars.index.task.TaskIndex;
import nars.index.term.map.CaffeineIndex;
import nars.link.BLink;
import nars.premise.DefaultPremiseBuilder;
import nars.premise.Derivation;
import nars.time.FrameTime;
import nars.time.Time;
import nars.truth.TruthDelta;
import nars.util.UtilityFunctions;
import nars.util.exe.Executioner;
import nars.util.exe.MultiThreadExecutioner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static nars.Op.*;
import static nars.bag.CurveBag.power2BagCurve;
import static nars.bag.CurveBag.power4BagCurve;

/**
 * heuristic task-driven NAR model without concept link network
 */
public class TaskNAR extends NAR {

    final static Logger logger = LoggerFactory.getLogger(TaskNAR.class);

    public final CurveBag<Task> tasks;
    final Deriver deriver = new DefaultDeriver();

    final MutableInteger derivationsPerCycle = new MutableInteger(1024);

    static class SimpleConceptBuilder extends DefaultConceptBuilder {

        public SimpleConceptBuilder() {
            this(new DefaultConceptState("x",
                    16, 16,
                    3,
                    0, 0));
        }

        public SimpleConceptBuilder(ConceptState s) {
            super( s,s );
        }

        @NotNull
        @Override
        public <X> Bag<X> newBag(@NotNull Map m) {
            return Bag.EMPTY;
        }

    }

    @Override
    protected TaskIndex newTaskIndex() {
        return new TaskBagIndex();
    }


    public TaskNAR(int capacity) {
        this(capacity, new MultiThreadExecutioner(2, 4096), new FrameTime());
    }

    public TaskNAR(int capacity, Executioner exe, Time time) {
        super(time, new CaffeineIndex(new SimpleConceptBuilder(), -1, false, exe),
                new XorShift128PlusRandom(1), exe);


        tasks = new CurveBag<Task>(capacity, new CurveBag.NormalizedSampler(power4BagCurve, random), BudgetMerge.maxBlend, new ConcurrentHashMap<>(capacity)) {

            @Override
            protected void sortAfterUpdate() {
                //do nothing here = only sort on commit
                //super.sortAfterUpdate();
            }

            @Override
            public void onAdded(BLink<Task> value) {
                //value.get().state(nar.concepts.conceptBuilder().awake(), nar);

            }

            @Override
            public void onRemoved(@NotNull BLink<Task> value) {
                Task x = value.get();
                runLater(()-> {
                    CompoundConcept c = (CompoundConcept) x.concept(TaskNAR.this);
                    if (c != null) {
                        c.tableFor(x.punc()).remove(x);

                        if (!(c instanceof PermanentConcept)) {
                            //synchronized (c) {
                            if (c.taskCount() == 0) {
                                concepts.remove(c.term());
                                c.delete(TaskNAR.this);
                            }
                            //}
                        }
                    }
                });

                //value.get().state(nar.concepts.conceptBuilder().sleep(), nar);
            }
        };

        onCycle(this::cycle);


    }

    @Override
    protected Activation process(@NotNull Task x, Concept cc) {

        //TODO this duplicates CompoundConcept.process code

        boolean accepted = false;
        TruthDelta delta = null;
        CompoundConcept c = (CompoundConcept) cc;


        switch (x.punc()) {
            case BELIEF:
                delta = c.processBelief(x, this);
                break;

            case GOAL:
                delta = c.processGoal(x, this);
                break;

            case QUESTION:
                accepted = c.processQuestion(x, this);
                break;

            case QUEST:
                accepted = c.processQuest(x, this);
                break;

            default:
                throw new RuntimeException("Invalid sentence type: " + x);
        }


        if (accepted || delta != null)
            return new MyActivation(x, c);
        else
            return null;
    }


    public void cycle() {
        tasks.commit();

        float load = exe.load();

        int cpf = Math.round(derivationsPerCycle.floatValue() * (1f - load));
        if (cpf > 0) {

            int cbs = 8; //conceptsFiredPerBatch.intValue();

            //logger.info("firing {} concepts (exe load={})", cpf, load);

            while (cpf > 0) {

                int batchSize = Math.min(cpf, cbs);
                cpf -= cbs;

                runLater(() -> {
                    List<BLink<Task>> sampled = $.newArrayList(batchSize*2);
                    tasks.sample(batchSize*2, sampled::add);
                    int n = sampled.size();
                    for (int i = 0; i < n; )
                        derive(sampled.get(i++), i < n ? sampled.get(i++) : null);
                });
            }
        }
    }

    protected void processDuplicate(@NotNull Task input, Task existing) {
        /* n/a */
    }

    public void derive(BLink<Task> ba, BLink<Task> bb) {

        if (ba == null || bb == null)
            return;

        Task a = ba.get();
        Task b = bb.get();

        float p = UtilityFunctions.and(a.priSafe(0),b.priSafe(0));
        if (p < tasks.priMin()) {
            tasks.pressure += p;
            return; //useless
        }

        float q = UtilityFunctions.aveAri(a.qua(),b.qua());

        DefaultPremiseBuilder.PreferConfidencePremise c = new DefaultPremiseBuilder.PreferConfidencePremise(
                a /* not necessary */,
                a, b.term(), (b != a && b.isBelief()) ? b : null,
                p, q
        );

        deriver.accept(new Derivation(this, c,
//                t -> {
//                    //if (t.pri() > a.pri() || t.pri() > b.pri())
//                        logger.info("{} {}\n\t{}", a, b, t);
//                    input(t);
//                }
                this::input
        ));

    }


    public static void main(String[] args) {

        TaskNAR n = new TaskNAR(1024) {
        };

        n.log();

        n.input("(a-->b).");
        n.input("(b-->c).");
        n.input("(c-->d).");
        n.input("(a-->d)?");
        n.run(1000);

        System.out.println();

        n.tasks.print();

    }

    private class MyActivation extends Activation {
        public MyActivation(@NotNull Task t, Concept c) {
            super(t, 1f, c, TaskNAR.this);
        }
    }

    private final class TaskBagIndex implements TaskIndex {

        @Override
        public @Nullable Task addIfAbsent(@NotNull Task t) {
            BLink<Task> r = tasks.put(t);
            if (r == null)
                return t; //rejected

            Task t2 = r.get();
            if (t2 != t)
                return t2; //duplicate

            return null; //accepted
        }

        @Override
        public void removeInternal(@NotNull Task tt) {
            tasks.remove(tt);
        }

        @Override
        public void clear() {
            tasks.clear();
        }

        @Override
        public void forEach(@NotNull Consumer<Task> each) {
            tasks.forEachKey(each);
        }

        @Override
        public boolean contains(@NotNull Task t) {
            return tasks.contains(t);
        }
    }
}

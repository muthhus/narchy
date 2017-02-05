package nars.control;

import jcog.data.MutableInteger;
import jcog.data.random.XorShift128PlusRandom;
import nars.NAR;
import nars.Task;
import nars.attention.Activation;
import nars.bag.Bag;
import nars.bag.CurveBag;
import nars.budget.BudgetMerge;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.conceptualize.DefaultConceptBuilder;
import nars.conceptualize.state.ConceptState;
import nars.conceptualize.state.DefaultConceptState;
import nars.derive.DefaultDeriver;
import nars.derive.Deriver;
import nars.index.task.MapTaskIndex;
import nars.index.task.TaskIndex;
import nars.index.term.map.CaffeineIndex;
import nars.link.BLink;
import nars.premise.DefaultPremiseBuilder;
import nars.premise.Derivation;
import nars.time.FrameTime;
import nars.truth.TruthDelta;
import nars.util.exe.SynchronousExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

import static nars.Op.*;
import static nars.bag.CurveBag.power2BagCurve;

/**
 * heuristic task-driven NAR model without concept link network
 */
public class TaskNAR extends NAR {

    final static Logger logger = LoggerFactory.getLogger(TaskNAR.class);

    public final CurveBag<Task> tasks;
    final Deriver deriver = new DefaultDeriver();

    final MutableInteger derivationsPerCycle = new MutableInteger(16);

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
        super(new FrameTime(), new CaffeineIndex(new SimpleConceptBuilder(), -1, false, ForkJoinPool.commonPool()),
                new XorShift128PlusRandom(1), new SynchronousExecutor());


        tasks = new CurveBag<Task>(capacity, new CurveBag.NormalizedSampler(power2BagCurve, random), BudgetMerge.maxBlend, new ConcurrentHashMap<>(capacity)) {

            @Override
            public void onAdded(BLink<Task> value) {
                //value.get().state(nar.concepts.conceptBuilder().awake(), nar);

            }

            @Override
            public void onRemoved(@NotNull BLink<Task> value) {
                Task x = value.get();
                CompoundConcept c = (CompoundConcept) x.concept(TaskNAR.this);
                if (c != null) {
                    synchronized (c) {
                        c.tableFor(x.punc()).remove(x);

                        if (c.taskCount() == 0) {
                            concepts.remove(c.term());
                        }
                    }
                }

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

        for (int i = 0; i < derivationsPerCycle.intValue(); i++)
            derive();
    }

    public void derive() {

        BLink<Task> ba = tasks.sample();
        if (ba == null)
            return;

        BLink<Task> bb = tasks.sample(); //TODO add learning/filtering heuristics which depend on 'a'
        if (bb == null)
            return;

        Task a = ba.get();
        Task b = bb.get();

        DefaultPremiseBuilder.PreferConfidencePremise p = new DefaultPremiseBuilder.PreferConfidencePremise(
                a /* not necessary */,
                a, b.term(), (b != a && b.isBelief()) ? b : null,
                1f, 1f
        );

        deriver.accept(new Derivation(this, p, this::input));

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

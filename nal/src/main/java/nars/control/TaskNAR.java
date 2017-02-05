package nars.control;

import jcog.data.MutableInteger;
import jcog.data.random.XorShift128PlusRandom;
import nars.NAR;
import nars.Task;
import nars.bag.Bag;
import nars.bag.CurveBag;
import nars.budget.BudgetMerge;
import nars.conceptualize.DefaultConceptBuilder;
import nars.derive.DefaultDeriver;
import nars.derive.Deriver;
import nars.index.task.MapTaskIndex;
import nars.index.term.map.CaffeineIndex;
import nars.link.BLink;
import nars.premise.DefaultPremiseBuilder;
import nars.premise.Derivation;
import nars.time.FrameTime;
import nars.util.exe.SynchronousExecutor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

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

        @NotNull
        @Override
        public <X> Bag<X> newBag(@NotNull Map m) {
            return Bag.EMPTY;
        }

    }

    public TaskNAR(int capacity) {
        super(new FrameTime(), new CaffeineIndex(new SimpleConceptBuilder(), -1, false, ForkJoinPool.commonPool()),
                new MapTaskIndex(true) /* deprecated */,
                new XorShift128PlusRandom(1), new SynchronousExecutor());


        tasks = new CurveBag<Task>(capacity, new CurveBag.NormalizedSampler(power2BagCurve, random), BudgetMerge.maxBlend, new ConcurrentHashMap<>(capacity)) {

            @Override
            public void onAdded(BLink<Task> value) {
                //value.get().state(nar.concepts.conceptBuilder().awake(), nar);
            }

            @Override
            public void onRemoved(@NotNull BLink<Task> value) {
                //value.get().state(nar.concepts.conceptBuilder().sleep(), nar);
            }
        };

        onCycle(this::cycle);


    }

    @Override
    protected Task addIfAbsent(@NotNull Task t) {
        BLink<Task> r = tasks.put(t);
        if (r == null)
            return t; //rejected

        Task t2 = r.get();
        if (t2!=t)
            return t2; //duplicate

        return null; //accepted
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
            a, b.term(), (b!=a && b.isBelief()) ? b : null,
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

}

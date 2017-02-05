package nars.control;

import nars.NAR;
import nars.Narsese;
import nars.Param;
import nars.Task;
import nars.bag.CurveBag;
import nars.budget.BudgetMerge;
import nars.derive.DefaultDeriver;
import nars.derive.Deriver;
import nars.derive.TrieDeriver;
import nars.link.BLink;
import nars.nar.Terminal;
import nars.premise.DefaultPremiseBuilder;
import nars.premise.Derivation;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

import static nars.bag.CurveBag.power2BagCurve;

/**
 * Created by me on 2/4/17.
 */
public class TaskBagControl {

    final static Logger logger = LoggerFactory.getLogger(TaskBagControl.class);

    public final CurveBag<Task> tasks;
    final NAR nar;
    final Deriver deriver = new DefaultDeriver();


    public TaskBagControl(NAR nar, int capacity) {

        this.nar = nar;

        tasks = new CurveBag<Task>(capacity, new CurveBag.NormalizedSampler(power2BagCurve, nar.random), BudgetMerge.maxBlend, new ConcurrentHashMap<>(capacity)) {

            @Override
            public void onAdded(BLink<Task> value) {
                //value.get().state(nar.concepts.conceptBuilder().awake(), nar);
            }

            @Override
            public void onRemoved(@NotNull BLink<Task> value) {
                //value.get().state(nar.concepts.conceptBuilder().sleep(), nar);
            }
        };

        nar.onCycle((Runnable)tasks::commit);


    }


    public Task input(String task) throws Narsese.NarseseException {
        return input( nar.task(task) );
    }

    public Task input(Task t) {
        if (t.isCommand())
            return t;

        try {
            t.normalize(nar);
            BLink<Task> r = tasks.put(t);
            if (r == null)
                return null; //rejected

            Task t2 = r.get();
            if (t2!=t) {
                output(t2);
            }
            return t2;

        } catch (Exception e) {
            if (Param.DEBUG)
                logger.error("{}", e);
            return null;
        }
    }

    protected void output(Task t) {

    }

    public void cycle() {

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

        deriver.accept(new Derivation(nar, p, this::input));

    }

    public void run(int j) {
        for (int i = 0; i < j; i++)
            cycle();
    }

    public static void main(String[] args) throws Narsese.NarseseException {

        TaskBagControl n = new TaskBagControl(new Terminal(), 1024) {
            @Override
            protected void output(Task t) {
                nar.onTask(System.out::println);
            }
        };

        n.input("(a-->b).");
        n.input("(b-->c).");
        n.input("(c-->d).");
        n.input("(a-->d)?");
        n.run(1000);

        System.out.println();

        n.tasks.print();

    }

}

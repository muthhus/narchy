package nars.control;

import nars.NAR;
import nars.Narsese;
import nars.Task;
import nars.bag.CurveBag;
import nars.budget.BudgetMerge;
import nars.derive.DefaultDeriver;
import nars.derive.Deriver;
import nars.link.BLink;
import nars.nar.Terminal;
import nars.premise.DefaultPremiseBuilder;
import nars.premise.Derivation;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

import static nars.bag.CurveBag.power2BagCurve;

/**
 * Created by me on 2/4/17.
 */
public class TaskBagControl {

    public final CurveBag<Task> tasks;
    final NAR nar = new Terminal();
    final Deriver deriver = new DefaultDeriver();

    public TaskBagControl(int capacity) {
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

    }

    public Task input(String task) throws Narsese.NarseseException {
        return input( nar.task(task) );
    }

    public Task input(Task t) {
        try {
            t.normalize(nar);
            BLink<Task> r = tasks.put(t);
            if (r == null)
                return null;
            else {
                tasks.commit();
                return r.get();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void cycle() {
        BLink<Task> a = tasks.sample();
        BLink<Task> b = tasks.sample(); //TODO add learning/filtering heuristics which depend on 'a'

        Task at = a.get();
        Task bt = b.get();

        DefaultPremiseBuilder.PreferConfidencePremise p = new DefaultPremiseBuilder.PreferConfidencePremise(
            at.term() /* not necessary */,
            at, bt.term(), bt,
            1f, 1f
        );
        deriver.accept(new Derivation(nar, p, this::input));

    }

    public static void main(String[] args) throws Narsese.NarseseException {

        TaskBagControl n = new TaskBagControl(1024);
        n.input("(a-->b).");
        n.input("(b-->c).");
        n.run(10);

        n.tasks.print();
    }

    private void run(int j) {
        for (int i = 0; i < j; i++)
            cycle();
    }

}

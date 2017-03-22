package nars.table;

import nars.NAR;
import nars.Task;
import nars.bag.impl.TaskHijackBag;
import nars.budget.BudgetMerge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Created by me on 2/16/17.
 */
public class HijackQuestionTable extends TaskHijackBag implements QuestionTable {


    public HijackQuestionTable(int cap, int reprobes, BudgetMerge merge, Random random) {
        super(reprobes, merge, random);

        capacity(cap);
    }


    @Override
    public float pri(@NotNull Task key) {
        return (1f + key.priSafe(0)) * (1f * key.qua());
    }

    @Override
    public @Nullable Task add(@NotNull Task t, @NotNull BeliefTable answers, @NotNull NAR n) {

        return add(t, n);
    }


    @Override
    public void capacity(int newCapacity, NAR nar) {
        setCapacity(newCapacity); //hijackbag
        capacity(newCapacity); //question table
    }


}

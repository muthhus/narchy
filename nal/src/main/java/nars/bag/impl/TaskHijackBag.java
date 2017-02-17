package nars.bag.impl;

import jcog.bag.impl.HijackBag;
import nars.Task;
import nars.attention.Forget;
import nars.budget.BudgetMerge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Consumer;

/**
 * Created by me on 2/17/17.
 */
public class TaskHijackBag extends BudgetHijackBag<Task,Task> {

    public TaskHijackBag(int reprobes, BudgetMerge merge, Random random) {
        super(random, merge, reprobes);
    }

    @Override
    public float pri(@NotNull Task key) {
        return key.pri();
    }

    @NotNull
    @Override
    public Task key(Task value) {
        return value;
    }

    @Override
    protected Consumer<Task> forget(float rate) {
        return new Forget(rate);
    }

}

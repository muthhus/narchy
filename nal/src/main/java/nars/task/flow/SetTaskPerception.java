package nars.task.flow;

import nars.Memory;
import nars.budget.BudgetMerge;
import nars.task.Task;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Buffer for tasks which is duplicate-free but unsorted.
 * It only supports inputting all its contents at once
 * to fairly apply what it contains.  This makes it faster
 * than SortedTaskPerception since the input Tasks should
 * theoretically have the same ultimate outcome even if
 * they are inserted in different orders.
 */
public final class SetTaskPerception extends TaskPerception {


    @NotNull
    private final BudgetedSet<Task> data;

    public SetTaskPerception(@NotNull Memory m, Consumer<Task[]> receiver, BudgetMerge merge) {
        super(m, receiver);
        this.data = new BudgetedSet(merge, Task[]::new);
    }

// this isnt safe
//    @Override
//    public void forEach(@NotNull Consumer<? super Task> each) {
//        table.forEach(each);
//    }

    @Override
    public void accept(@NotNull Task t) {
        data.put(t);
    }

    @Override
    protected final void nextFrame(@NotNull Consumer<Task[]> receiver) {

        data.flushAll(receiver);
    }

    @Override
    public void clear() {
        data.clear();
    }
}

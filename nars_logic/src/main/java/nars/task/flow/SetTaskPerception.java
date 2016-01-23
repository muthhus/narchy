package nars.task.flow;

import nars.Memory;
import nars.budget.BudgetMerge;
import nars.task.Task;
import nars.util.data.map.UnifriedMap;
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

    final UnifriedMap<Task,Task> table = new UnifriedMap<>();
    final BudgetMerge merge;


    public SetTaskPerception(@NotNull Memory m, Consumer<Task> receiver, BudgetMerge merge) {
        super(m, receiver);
        this.merge = merge;
    }

// this isnt safe
//    @Override
//    public void forEach(@NotNull Consumer<? super Task> each) {
//        table.forEach(each);
//    }

    @Override
    public void accept(@NotNull Task t) {
        if (t.getDeleted()) {
            throw new RuntimeException("deleted: " + t);
        }
        Task existing = table.put(t, t);
        if ((existing!=null) && (existing!=t) && (!existing.getDeleted())) {
            merge.merge(t.getBudget(), existing.getBudget(), 1f);
        }
    }

    @Override
    protected final void nextFrame(@NotNull Consumer<Task> receiver) {


        //create an array copy in case the table is modified as a result of executing a task
        Task[] aa = table.toArray(new Task[table.size()]);
        table.clear();
        for (Task x: aa) {
            receiver.accept(x);
        }

    }

    @Override
    public void clear() {
        table.clear();
    }
}

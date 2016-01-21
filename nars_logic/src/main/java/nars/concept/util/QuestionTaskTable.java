package nars.concept.util;

import nars.Memory;
import nars.budget.BudgetMerge;
import nars.task.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;

/** task table used for storing Questions and Quests.
 *  simpler than Belief/Goal tables
 * */
public interface QuestionTaskTable extends TaskTable {

    /**
     * attempt to insert a task.
     *
     * @return: the input task itself, it it was added to the table
     * an existing equivalent task if this was a duplicate
     */
    @NotNull
    Task add(Task t, BiPredicate<Task, Task> equality, BudgetMerge duplicateMerge, Memory m);


    /**
     * @return null if no duplicate was discovered, or the first Task that matched if one was
     */
    @Nullable
    Task getFirstEquivalent(Task t, @NotNull BiPredicate<Task,Task>  e);
//    {
//        for (Task a : this) {
//            if (e.test(a, t))
//                return a;
//        }
//        return null;
//    }
}

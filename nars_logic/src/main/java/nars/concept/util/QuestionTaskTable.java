package nars.concept.util;

import nars.Memory;
import nars.budget.BudgetMerge;
import nars.task.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    Task add(Task t, BudgetMerge duplicateMerge, Memory m);


    /**
     * @return null if no duplicate was discovered, or the first Task that matched if one was
     */
    @Nullable
    Task getFirstEquivalent(Task t);
//    {
//        for (Task a : this) {
//            if (e.test(a, t))
//                return a;
//        }
//        return null;
//    }
}

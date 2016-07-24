package nars.concept.table;

import nars.NAR;
import nars.task.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/** task table used for storing Questions and Quests.
 *  simpler than Belief/Goal tables
 * */
public interface QuestionTable extends TaskTable {

    /**
     * attempt to insert a task.
     *
     * @return: the input task itself, it it was added to the table
     * an existing equivalent task if this was a duplicate
     */
    Task add(@NotNull Task t, @NotNull BeliefTable answers, @NotNull List<Task> displ, @NotNull NAR n);

    void capacity(int newCapacity, @NotNull List<Task> displ);



    /** called when a new answer appears */
    void answer(@NotNull Task result, @NotNull NAR nar, @NotNull List<Task> displ);

//    {
//        for (Task a : this) {
//            if (e.test(a, t))
//                return a;
//        }
//        return null;
//    }

    @Nullable QuestionTable EMPTY = new QuestionTable() {

        @Override
        public Iterator<Task> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public Task add(Task t, BeliefTable answers, List<Task> displ, NAR n) {
            return null;
        }

        @Override
        public void capacity(int newCapacity, List<Task> onRemoval) {

        }

        @Override
        public int capacity() {
            return 0;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }



        @Override
        public void answer(Task result, NAR nar, List<Task> displ) {

        }
    };
}

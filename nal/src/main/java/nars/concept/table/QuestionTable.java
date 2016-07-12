package nars.concept.table;

import nars.NAR;
import nars.task.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
    Task add(Task t, BeliefTable answers, List<Task> displ, NAR n);

    void capacity(int newCapacity);

    /**
     * @return null if no duplicate was discovered, or the first Task that matched if one was
     */
    @Override
    @Nullable
    Task get(Task t);

    /** called when a new answer appears */
    void answer(Task result, NAR nar, List<Task> displ);

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
        public void capacity(int newCapacity) {

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
        public void clear() {

        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public void remove(@NotNull Task belief, List<Task> displ) {

        }

        @Override
        public @Nullable Task get(Task t) {
            return null;
        }

        @Override
        public void answer(Task result, NAR nar, List<Task> displ) {

        }
    };
}

package nars.table;

import nars.NAR;
import nars.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;

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
    Task add(@NotNull Task t, @NotNull NAR n);

    void capacity(int newCapacity, NAR nar);



//    /** called when a new answer appears */
//    void answer(@NotNull Task result, Concept answerConcept, @NotNull NAR nar, @NotNull List<Task> displ);

//    {
//        for (Task a : this) {
//            if (e.test(a, t))
//                return a;
//        }
//        return null;
//    }

    @Nullable QuestionTable EMPTY = new QuestionTable() {



        @Override
        public Iterator<Task> taskIterator() {
            return Collections.emptyIterator();
        }
//
//        @Override
//        public Spliterator<Task> spliterator() {
//            return Spliterators.emptySpliterator();
//        }


        @Override
        public boolean removeTask(Task x) {
            return false;
        }

        @Override
        public Task add(Task t, NAR n) {
            return null;
        }

        @Override
        public void capacity(int newCapacity, NAR n) {

        }

        @Override
        public int capacity() {
            return 0;
        }

        @Override
        public int size() {
            return 0;
        }



    };


}

package nars.table;

import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;

/** task table used for storing Questions and Quests.
 *  simpler than Belief/Goal tables
 * */
public interface QuestionTable extends TaskTable {


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

    @NotNull QuestionTable EMPTY = new QuestionTable() {



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
        public void clear() {

        }

        @Override
        public void forEachTask(Consumer<? super Task> x) {

        }

        @Override
        public boolean removeTask(Task x) {
            return false;
        }

        @Override
        public void add(@NotNull Task t, TaskConcept c, NAR n) {

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

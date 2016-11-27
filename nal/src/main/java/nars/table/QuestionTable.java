package nars.table;

import nars.NAR;
import nars.Task;
import nars.concept.Concept;
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
    @Nullable Task add(@NotNull Task t, @NotNull BeliefTable answers, @NotNull NAR n);

    void capacity(int newCapacity, NAR nar);



    /** called when a new answer appears */
    void answer(@NotNull Task result, Concept answerConcept, @NotNull NAR nar, @NotNull List<Task> displ);

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

        @Nullable
        @Override
        public Task add(Task t, BeliefTable answers, NAR n) {
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

        @Override
        public boolean isEmpty() {
            return true;
        }



        @Override
        public void answer(Task result, Concept answerConcept, NAR nar, List<Task> displ) {

        }
    };

    default float priSum() {
        final float[] p = {0};
        forEach(t -> p[0] +=t.pri());
        return p[0];
    }

}

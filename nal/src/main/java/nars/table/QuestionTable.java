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


    void capacity(int newCapacity);


    /** allows question to pass through it to the link activation phase, but
     * otherwise does not store it
     */
    @NotNull QuestionTable Unstored = new EmptyQuestionTable();

    @NotNull QuestionTable Null = new NullQuestionTable();

    static class NullQuestionTable implements QuestionTable {

        @Override
        public void add(@NotNull Task t, TaskConcept c, NAR n) {

        }

        @Override
        public Iterator<Task> taskIterator() {
            return Collections.emptyIterator();
        }

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

    }

    class EmptyQuestionTable extends QuestionTable.NullQuestionTable {

        @Override
        public void add(@NotNull Task t, TaskConcept c, NAR n) {
            TaskTable.activate(t, t.pri(), n);
        }

        @Override
        public int capacity() {
            return Integer.MAX_VALUE;
        }


    }
}

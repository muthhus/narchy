package nars.table;

import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;
import nars.control.Activate;
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
    //@NotNull QuestionTable Unstored = new EmptyQuestionTable();

    @NotNull QuestionTable Null = new NullQuestionTable();

    class NullQuestionTable implements QuestionTable {

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

    /** untested */
    class EmptyQuestionTable extends QuestionTable.NullQuestionTable {

        final static HijackQuestionTable common = new HijackQuestionTable(1024, 3);

        @Override
        public void add(@NotNull Task t, TaskConcept c, NAR n) {
            Task e = common.get(t);
            float activation = t.priElseZero();
            if (e ==null) {
                common.put(t);


                //TaskTable.activate(t, t.priElseZero(), n);
            } else {
                activation -= e.priElseZero();
            }

            Activate.activate(t, activation, n);
        }

        @Override
        public int capacity() {
            return Integer.MAX_VALUE;
        }

        @Override
        public void capacity(int newCapacity) {
            super.capacity(newCapacity);
        }
    }
}

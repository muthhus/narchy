package nars.table;

import jcog.list.ArrayIterator;
import jcog.map.MRUCache;
import jcog.pri.Pri;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.BaseConcept;
import nars.control.Activate;
import nars.task.NALTask;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * task table used for storing Questions and Quests.
 * simpler than Belief/Goal tables
 */
public interface QuestionTable extends TaskTable {


    void capacity(int newCapacity);


    /**
     * allows question to pass through it to the link activation phase, but
     * otherwise does not store it
     */
    //@NotNull QuestionTable Unstored = new EmptyQuestionTable();

    @NotNull QuestionTable Empty = new NullQuestionTable();

    class NullQuestionTable implements QuestionTable {

        @Override
        public Stream<Task> stream() {
            return Stream.empty();
        }

        @Override
        public void add(@NotNull Task t, BaseConcept c, NAR n) {

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

    /** unsorted, MRU policy */
    static class DefaultQuestionTable extends MRUCache<Task, Task> implements QuestionTable {

        final Object lock = new Object();

        public DefaultQuestionTable() {
            super(0);
        }

        @Override
        public void capacity(int newCapacity) {
            setCapacity(newCapacity);
        }

        @Override
        protected void onEvict(Map.Entry<Task, Task> entry) {
            Task x = entry.getKey();
            x.delete();
            Task y = entry.getValue();
            if (y != x)
                y.delete();
        }

        @Override
        public void add(@NotNull Task t, BaseConcept c, NAR n) {
            final float[] act = new float[1];
            Task u;
            synchronized (lock) {
                u = merge(t, t, (prev, next) -> {
                    if (prev == next)
                        act[0] = 0;
                    else {
                        float pBefore = prev.priElseZero();
                        float overage = Param.taskMerge.merge(prev, next);
                        float pAfter = prev.priElseZero();
                        act[0] = pAfter - pBefore;
                    }
                    return prev;
                });
            }
            if (u != t) {
                ((NALTask) u).causeMerge(t);
                t.delete();
            } else {
                act[0] = u.priElseZero();
            }

            if (act[0] > Pri.EPSILON) {
                Activate.activate(u, act[0], n, c);
            }
        }

        @Override
        public int capacity() {
            return capacity;
        }


        @Override
        public Iterator<Task> taskIterator() {
            Task[] t = toArray();
            return new ArrayIterator(t);
        }

        @Override
        public Stream<Task> stream() {
            Task[] t = toArray();
            return Stream.of(t);
        }

        public Task[] toArray() {
            synchronized (lock) {
                int s = size();
                return s == 0 ? Task.EmptyArray : values().toArray(new Task[s]);
            }
        }

        @Override
        public void forEachTask(Consumer<? super Task> x) {
            Task[] t = toArray();
            for (Task y : t)
                x.accept(y);
        }

        @Override
        public boolean removeTask(Task x) {
            synchronized (lock) {
                return remove(x) != null;
            }
        }

        @Override
        public void clear() {
            synchronized (lock) {
                super.clear();
            }
        }

    }

//    /** untested */
//    class EmptyQuestionTable extends QuestionTable.NullQuestionTable {
//
//        final static HijackQuestionTable common = new HijackQuestionTable(1024, 3);
//
//        @Override
//        public void add(@NotNull Task t, BaseConcept c, NAR n) {
//            Task e = common.get(t);
//            float activation = t.priElseZero();
//            if (e ==null) {
//                common.put(t);
//
//
//                //TaskTable.activate(t, t.priElseZero(), n);
//            } else {
//                activation -= e.priElseZero();
//            }
//
//            Activate.activate(t, activation, n);
//        }
//
//        @Override
//        public int capacity() {
//            return Integer.MAX_VALUE;
//        }
//
//
//    }
}

package nars.table;

import nars.NAR;
import nars.Task;
import nars.attention.Activate;
import nars.concept.TaskConcept;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * holds a set of ranked question/quests tasks
 * top ranking items are stored in the lower indexes so they will be first iterated
 */
public interface TaskTable  {


    /**
     * attempt to insert a task.
     *
     * @return: the input task itself, it it was added to the table
     * an existing equivalent task if this was a duplicate
     */
    void add(@NotNull Task t, TaskConcept c, NAR n);


    static void activate(@NotNull Task t, float activation, @NotNull NAR n) {
        n.eventTaskProcess.emit(/*post*/t);
        n.input(new Activate(t, activation));
    }

    int capacity();


    /**
     * number of items in this collection
     */
    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    Iterator<Task> taskIterator();





//    @Nullable
//    default BivariateGridInterpolator getWaveFrequencyConfidenceTime() {
//        return null;
//    }
//
//    @Nullable
//    default UnivariateInterpolator getWaveFrequencyConfidence() {
//        return null;
//    }
//
//    @Nullable
//    default UnivariateInterpolator getWaveConfidenceTime() {
//        return null;
//    }

    void forEachTask(Consumer<? super Task> x);
//    default void forEachTask(Consumer<? super Task> x) {
//        //TODO filter deleted tasks
//        taskIterator().forEachRemaining(x);
//    }

    default void forEach(int _maxPerConcept, @NotNull Consumer<? super Task> recip) {
        int s = size();
        final int[] maxPerConcept = {Math.min(s, _maxPerConcept)};
        forEachTask(t -> {
            if ((maxPerConcept[0]--) >= 0)
                recip.accept(t);
            //if (--maxPerConcept == 0) break; //TODO use a forEachWhile w/ Predicate or something
        });
    }

    /** returns true if the task was removed */
    boolean removeTask(Task x);

    void clear();


    //boolean contains(Task t);

//    @Nullable
//    QuestionTable EMPTY = new QuestionTable() {
//
//        @Override
//        public
//        @Nullable
//        Task add(Task t, Memory m) {
//            return t;
//        }
//
//        @Override
//        public
//        @Nullable
//        Task get(Task t) {
//            return null;
//        }
//
//        @Override
//        public Iterator<Task> iterator() {
//            return Iterators.emptyIterator();
//        }
//
//        @Override
//        public int capacity() {
//            return 0;
//        }
//
//
//        @Override
//        public void setCapacity(int newCapacity) {
//
//        }
//
//        @Override
//        public int size() {
//            return 0;
//        }
//
//        @Override
//        public void clear() {
//
//        }
//
//        @Override
//        public boolean isEmpty() {
//            return true;
//        }
//
//        @Override
//        public void remove(@NotNull Task belief, @NotNull NAR nar) {
//            throw new UnsupportedOperationException();
//        }
//
//
//
//    };

//    /** forcibly remove a held Task
//     *  should eventually invoke TaskTable.removeTask() */
//    void remove(@NotNull Task belief, List<Task> displ);


    //void add(Task incoming, List<Task> displaced);

    //Task put(Task incoming);
}

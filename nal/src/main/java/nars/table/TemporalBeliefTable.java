package nars.table;

import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.function.Consumer;


public interface TemporalBeliefTable extends TaskTable, Iterable<Task> {



    /** finds or generates the strongest match to the specified parameters.
     * Task against is an optional argument which can be used to compare internal temporal dt structure for similarity */
    Task match(long when, long now, int dur, @Nullable Task against, Random rng);

    /** estimates the truth value for the provided time.
     * the eternal table's top value, if existent, contributes a 'background'
     * level in interpolation.
     * */
    Truth truth(long when, int dur, EternalTable eternal);

    @Override
    void clear();

    void setCapacity(int temporals);

    @Override
    default Iterator<Task> iterator() {
        return taskIterator();
    }

    TemporalBeliefTable EMPTY = new TemporalBeliefTable() {

        @Override
        public void add(@NotNull Task t, TaskConcept c, NAR n) {

        }

        @Override
        public void setCapacity(int c) {

        }

        @Override
        public int capacity() {
            //throw new UnsupportedOperationException();
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean removeTask(Task x) {
            return false;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Iterator<Task> taskIterator() {
            return Collections.emptyIterator();
        }

        @Override
        public void forEachTask(Consumer<? super Task> x) {

        }

        @Override
        public Task match(long when, long now, int dur, @Nullable Task against, Random rng) {
            return null;
        }

        @Override
        public PreciseTruth truth(long when, int dur, EternalTable eternal) {
            return null;
        }

        @Override
        public void clear() {

        }
    };


}

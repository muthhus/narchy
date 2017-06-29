package nars.table;

import nars.NAR;
import nars.Task;
import nars.concept.TaskConcept;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;


public interface TemporalBeliefTable extends TaskTable, Iterable<Task> {


    static float temporalTaskPriority(Task t, long now, int dur) {
        return /*t.originality() * */ t.conf(now, dur) * (1f + t.dtRange()/((float)dur))/(1+Math.abs(now - t.nearestStartOrEnd(now)/dur));
    }

    /** finds or generates the strongest match to the specified parameters.
     * Task against is an optional argument which can be used to compare internal temporal dt structure for similarity */
    Task match(long when,  @Nullable Task against, NAR nar);

    /** estimates the truth value for the provided time.
     * the eternal table's top value, if existent, contributes a 'background'
     * level in interpolation.
     * */
    Truth truth(long when, EternalTable eternal, NAR nar);

    default Truth truth(long when,  NAR nar) {
        return truth(when, null, nar);
    }

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
        public Task match(long when, @Nullable Task against, NAR nar) {
            return null;
        }

        @Override
        public Truth truth(long when, EternalTable eternal, NAR nar) {
            return null;
        }

        @Override
        public void clear() {

        }
    };


}

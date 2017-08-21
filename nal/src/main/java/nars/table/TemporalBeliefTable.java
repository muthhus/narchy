package nars.table;

import nars.NAR;
import nars.Task;
import nars.concept.BaseConcept;
import nars.task.SignalTask;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;


public interface TemporalBeliefTable extends TaskTable, Iterable<Task> {


    /**
     * TODO make a version of this which takes as argument min and max (one of which
     * will be infinity) so it can exit early if it will not rank
     */
    static float temporalTaskPriority(Task t, long now, int dur) {
        float fdur = dur;
        return
                t.conf() * //raw because time is considered below. this covers cases where the task eternalizes
                //t.conf(now, dur) *
                //t.evi(now, dur) *
                (1f + t.range()/ fdur)/(1+Math.abs(now - t.nearestTimeTo(now))/fdur);
    }

    /** finds or generates the strongest match to the specified parameters.
     * Task against is an optional argument which can be used to compare internal temporal dt structure for similarity */
    Task match(long when, @Nullable Term against, NAR nar);

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

    default Consumer<Task> stretch(SignalTask changed) {
        throw new UnsupportedOperationException();
    }

    @Override
    default Iterator<Task> iterator() {
        return taskIterator();
    }

    TemporalBeliefTable Empty = new TemporalBeliefTable() {

        @Override
        public void add(@NotNull Task t, BaseConcept c, NAR n) {

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
        public Stream<Task> stream() {
            return Stream.empty();
        }

        @Override
        public void forEachTask(Consumer<? super Task> x) {

        }

        @Override
        public Task match(long when, @Nullable Term against, NAR nar) {
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

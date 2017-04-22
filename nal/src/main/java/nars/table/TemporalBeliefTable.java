package nars.table;

import nars.NAR;
import nars.Task;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Created by me on 5/7/16.
 */
public interface TemporalBeliefTable extends TaskTable, Iterable<Task> {



    /** finds the strongest match to the specified parameters. Task against is an optional argument which can be used to compare internal temporal dt structure for similarity */
    @Nullable Task match(long when, long now, int dur, @Nullable Task against);

    Truth truth(long when, int dur, EternalTable eternal);

    void capacity(int c, NAR nar);

    void clear();

    @Override
    default Iterator<Task> iterator() {
        return taskIterator();
    }

    TemporalBeliefTable EMPTY = new TemporalBeliefTable() {
        @Override
        public int capacity() {
            //throw new UnsupportedOperationException();
            return Integer.MAX_VALUE;
        }

        @Override
        public @Nullable Task add(@NotNull Task input) {
            return null;
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
        public @Nullable Task match(long when, long now, int dur, @Nullable Task against) {
            return null;
        }

        @Override
        public Truth truth(long when, int dur, EternalTable eternal) {
            return null;
        }

        @Override
        public void capacity(int c, NAR nar) { /* N/A */ }


        @Override
        public void clear() {

        }
    };

    @Nullable Task add(@NotNull Task input);

}

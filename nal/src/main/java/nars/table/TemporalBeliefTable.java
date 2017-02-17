package nars.table;

import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.truth.Truth;
import nars.truth.TruthDelta;
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
    @Nullable Task match(long when, long now, float dur, @Nullable Task against);

    @Nullable Truth truth(long when, long now, float dur, EternalTable eternal);

    /** return null if wasnt added */
    @Nullable TruthDelta add(@NotNull Task input, EternalTable eternal, Concept concept, @NotNull NAR nar);

    //boolean removeIf(@NotNull Predicate<? super Task> o, NAR nar);



    void capacity(int c, NAR nar);

    boolean isFull();

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
        public @Nullable Task match(long when, long now, float dur, @Nullable Task against) {
            return null;
        }

        @Override
        public @Nullable Truth truth(long when, long now, float dur, EternalTable eternal) {
            return null;
        }

        @Override
        public @Nullable TruthDelta add(@NotNull Task input, EternalTable eternal, Concept concept, @NotNull NAR nar) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void capacity(int c, NAR nar) { /* N/A */ }

        @Override
        public boolean isFull() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {

        }
    };

    //void range(long[] t);
}

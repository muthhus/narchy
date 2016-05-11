package nars.concept.table;

import nars.NAR;
import nars.bag.impl.ListTable;
import nars.task.Task;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by me on 5/7/16.
 */
public interface TemporalBeliefTable extends ListTable<Task,Task> {


    @Nullable Task top(long when);

    @Nullable TemporalBeliefTable Empty = new TemporalBeliefTable() {

        @Override
        public void clear() {

        }

        @Nullable
        @Override
        public Task get(Object key) {
            return null;
        }

        @Nullable
        @Override
        public Object remove(Task key) {
            return null;
        }

        @Nullable
        @Override
        public Task put(Task task, Task task2) {
            return null;
        }

        @Override
        public void forEachKey(Consumer<? super Task> each) {

        }

        @Override
        public int capacity() {
            return 0;
        }

        @Override
        public void setCapacity(int i) {

        }

        @Override
        public void topWhile(Predicate<Task> each) {

        }

        @Override
        public List<Task> list() {
            return Collections.emptyList();
        }

        @Nullable
        @Override
        public Task top(long when) {
            return null;
        }

        @Nullable
        @Override
        public Truth truth(long when) {
            return null;
        }

        @Nullable
        @Override
        public Task ready(Task input, NAR nar) {
            return null;
        }
    };

    @Nullable Truth truth(long when);

    @Nullable Task ready(Task input, NAR nar);
}

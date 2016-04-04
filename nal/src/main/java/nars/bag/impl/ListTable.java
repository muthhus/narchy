package nars.bag.impl;

import com.sun.xml.internal.xsom.impl.scd.Iterators;
import nars.bag.Table;
import nars.task.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * adds list selection and ranking methods to the Table interface
 */
public interface ListTable<V, L> extends Table<V, L> {

    @NotNull
    ListTable<Task, Task> Empty = new ListTable() {

        @Override
        public Iterator iterator() {
            return Iterators.empty();
        }

        @Override
        public void clear() {

        }

        @Nullable
        @Override
        public Object get(@NotNull Object key) {
            return null;
        }

        @Nullable
        @Override
        public Object remove(@NotNull Object key) {
            return null;
        }

        @Nullable
        @Override
        public Object put(@NotNull Object o, @NotNull Object o2) {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public void forEachKey(@NotNull Consumer each) {

        }

        @Override
        public void topWhile(@NotNull Predicate each) {

        }

        @Override
        public Object top() {
            return null;
        }

        @Override
        public Object bottom() {
            return null;
        }

        @Override
        public List list() {
            return Collections.emptyList();
        }

        @Override
        public int capacity() {
            return 0;
        }

        @Override
        public void setCapacity(int i) {

        }
    };

    /** first (0th) item */
    @Nullable
    L top();

    /** last item */
    @Nullable
    L bottom();


    /** returns a list of the values */
    List<L> list();

    int capacity();

    void setCapacity(int i);
}

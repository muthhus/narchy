package nars.bag.impl;

import nars.bag.Table;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;


public interface SortedTable<V, L> extends Table<V,L> {

    @Nullable SortedTable Empty = new SortedTable() {

        @Override
        public Iterator<Object> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public void clear() {

        }

        @Nullable
        @Override
        public Object get(Object key) {
            return null;
        }

        @Nullable
        @Override
        public Object remove(Object key) {
            return null;
        }

        @Nullable
        @Override
        public Object put(Object Object, Object Object2) {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public void forEachKey(Consumer each) {

        }

        @Override
        public void topWhile(Predicate each) {

        }


        @Override
        public int capacity() {
            return 0;
        }

        @Override
        public void setCapacity(int i) {
            throw new UnsupportedOperationException();
        }

        @Nullable
        @Override
        public Object top() {
            return null;
        }

        @Nullable
        @Override
        public Object bottom() {
            return null;
        }
    };

    @Nullable L top();

    @Nullable L bottom();

}

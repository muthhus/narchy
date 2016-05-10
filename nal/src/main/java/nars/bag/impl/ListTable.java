package nars.bag.impl;

import nars.bag.Table;
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



    /** returns a list of the values */
    List<L> list();


    default L get(int i) {
        return list().get(i);
    }

    @Override default Iterator<L> iterator() {
        return list().iterator();
    }


    @Override
    default int size() {
        return list().size();
    }


    @Nullable ListTable Empty = new ListTable() {


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

        @Override
        public List<Object> list() {
            return Collections.emptyList();
        }
    };




}

package jcog.tree.rtree;

/*
 * #%L
 * Conversant RTree
 * ~~
 * Conversantmedia.com © 2016, Conversant, Inc. Conversant® is a trademark of Conversant, Inc.
 * ~~
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import jcog.tree.rtree.util.Stats;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Created by jcovert on 12/30/15.
 */
public class ConcurrentRTree<T> implements Space<T> {

    public final Space<T> tree;

//  TODO move this to a subclass
//    final QueueLock<T> toAdd, toRemove;
//        if (async) {
//            toAdd = new QueueLock<>(new DisruptorBlockingQueue<T>(8), this::add);
//            toRemove = new QueueLock<>(new DisruptorBlockingQueue<T>(8), this::remove);
//        } else {
//            toAdd = toRemove = null;
//        }

    private final Lock readLock;
    public final Lock writeLock;



    public ConcurrentRTree(RTree<T> tree) {
        this.tree = tree;
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();


    }


    @Override
    public Spatialization<T> model() {
        return tree.model();
    }

    /**
     * Blocking locked search
     *
     * @param rect - HyperRect to search
     * @param t    - array to hold results
     * @return number of entries found
     */
    @Override
    public int containedToArray(HyperRegion rect, T[] t) {
        readLock.lock();
        try {
            return tree.containedToArray(rect, t);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Node<T, ?> root() {
        return tree.root();
    }

    /**
     * Blocking locked add
     *
     * @param t - entry to add
     */
    @Override
    public boolean add(T t) {
        writeLock.lock();
        try {
            return tree.add(t);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void intersectingNodes(HyperRegion start, Predicate<Node<T, ?>> eachWhile) {
        readLock.lock();
        try {
            tree.intersectingNodes(start, eachWhile);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * prefer this instead of add() in multithread environments, because it elides what might ordinarily involve a lock wait
     */
    @Override
    public void addAsync(@NotNull T t) {
//        if (toAdd!=null)
//            toAdd.accept(t);
//        else
            add(t);
    }

    @Override
    public void removeAsync(@NotNull T t) {
//        if (toRemove!=null)
//            toRemove.accept(t);
//        else
            remove(t);
    }

    /**
     * Blocking locked remove
     *
     * @param x - entry to remove
     */
    @Override
    public boolean remove(T x, HyperRegion xBounds) {
        writeLock.lock();
        try {
            return tree.remove(x, xBounds);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean remove(T x) {
        writeLock.lock();
        try {
            return tree.remove(x);
        } finally {
            writeLock.unlock();
        }
    }

    public void removeAll(Iterable<? extends T> t) {
        writeLock.lock();
        try {
            t.forEach(this::remove);
        } finally {
            writeLock.unlock();
        }
    }


    public void read(Consumer<Space<T>> x) {
        readLock.lock();
        try {
            x.accept(tree);
        } finally {
            readLock.unlock();
        }
    }

    public <Y> Y read(Function<Space<T>,Y> x) {
        readLock.lock();
        try {
            return x.apply(tree);
        } finally {
            readLock.unlock();
        }
    }

    public void write(Consumer<Space<T>> x) {
        writeLock.lock();
        try {
            x.accept(tree);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public HyperRegion bounds(T task) {
        return tree.bounds(task);
    }

    //    @Override
//    public void change(T x, T y) {
//        writeLock.lock();
//        try {
//            rTree.remove(x);
//            rTree.add(y);
//        } finally {
//            writeLock.unlock();
//        }
//    }

    /**
     * Blocking locked update
     *
     * @param told - entry to update
     * @param tnew - entry with new value
     */
    @Override
    public void replace(T told, T tnew) {
        writeLock.lock();
        try {
            tree.replace(told, tnew);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Non-blocking locked search
     *
     * @param rect - HyperRect to search
     * @param t    - array to hold results
     * @return number of entries found or -1 if lock was not acquired
     */
    public int trySearch(HyperRegion rect, T[] t) {
        if (readLock.tryLock()) {
            try {
                return tree.containedToArray(rect, t);
            } finally {
                readLock.unlock();
            }
        }
        return -1;
    }

    /**
     * Non-blocking locked add
     *
     * @param t - entry to add
     * @return true if lock was acquired, false otherwise
     */
    public boolean tryAdd(T t) {
        if (writeLock.tryLock()) {
            try {
                tree.add(t);
            } finally {
                writeLock.unlock();
            }
            return true;
        }
        return false;
    }

    /**
     * Non-blocking locked remove
     *
     * @param t - entry to remove
     * @return true if lock was acquired, false otherwise
     */
    public boolean tryRemove(T t) {
        if (writeLock.tryLock()) {
            try {
                tree.remove(t);
            } finally {
                writeLock.unlock();
            }
            return true;
        }
        return false;
    }

    /**
     * Non-blocking locked update
     *
     * @param told - entry to update
     * @param tnew - entry with new values
     * @return true if lock was acquired, false otherwise
     */
    public boolean tryUpdate(T told, T tnew) {
        if (writeLock.tryLock()) {
            try {
                tree.replace(told, tnew);
            } finally {
                writeLock.unlock();
            }
            return true;
        }
        return false;
    }

    @Override
    public int size() {
        return tree.size();
    }

    @Override
    public void clear() {
        writeLock.lock();
        try {
            tree.clear();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void forEach(Consumer<? super T> consumer) {
        readLock.lock();
        try {
            tree.forEach(consumer);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean containing(HyperRegion rect, Predicate<T> consumer) {
        readLock.lock();
        boolean result;
        try {
            result = tree.containing(rect, consumer);
        } finally {
            readLock.unlock();
        }
        return result;
    }

    @Override
    public boolean intersecting(HyperRegion rect, Predicate<T> consumer) {
        readLock.lock();
        boolean result;
        try {
            result = tree.intersecting(rect, consumer);
        } finally {
            readLock.unlock();
        }
        return result;
    }


    @Override
    public Stream<T> stream() {
        return root().stream();
    }

    @Override
    public Iterator<T> iterator() {
        return stream().iterator();
    }

    @Override
    public Stats stats() {
        readLock.lock();
        try {
            return tree.stats();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String toString() {
        return tree.toString();
    }

    @Override
    public void reportSizeDelta(int i) {
        tree.reportSizeDelta(i);
    }

    @Override
    public boolean contains(T t, Spatialization<T> model) {
        return tree.contains(t, model);
    }

}

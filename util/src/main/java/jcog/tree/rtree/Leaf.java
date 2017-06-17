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

import jcog.tree.rtree.util.CounterNode;
import jcog.tree.rtree.util.Stats;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Node that will contain the data entries. Implemented by different type of SplitType leaf classes.
 * <p>
 * Created by jcairns on 4/30/15.
 */
public abstract class Leaf<T> implements Node<T> {

    @Deprecated
    public final RTree.Split splitType;
    protected final short mMax;       // max entries per node
    protected final short mMin;       // least number of entries per node
    protected final T[] data;
    @Deprecated
    protected final Function<T, HyperRect> builder;
    protected short size;
    protected HyperRect bounds;


    protected Leaf(@Deprecated final Function<T, HyperRect> builder, final int mMin, final int mMax, final RTree.Split splitType) {
        this.mMin = (short) mMin;
        this.mMax = (short) mMax;
        this.bounds = null;
        this.builder = builder;
        this.data = (T[]) new Object[mMax];
        this.size = 0;
        this.splitType = splitType;
    }

    @Override
    public Node<T> add(final T t, Nodelike<T> parent) {

        if (!contains(t)) {
            Node<T> next;

            if (size < mMax) {
                final HyperRect tRect = builder.apply(t);
                bounds = bounds != null ? bounds.mbr(tRect) : tRect;

                data[size++] = t;

                next = this;
            } else {
                next = split(t);
            }
            parent.reportSizeDelta(+1);

            return next;
        } else {
            return this;
        }
    }

    @Override
    public void reportSizeDelta(int i) {
        //safely ignored
    }

    @Override
    public boolean AND(Predicate<T> p) {
        for (int i = 0; i < size; i++)
            if (!p.test(data[i]))
                return false;
        return true;
    }

    @Override
    public boolean OR(Predicate<T> p) {
        for (int i = 0; i < size; i++)
            if (p.test(data[i]))
                return true;
        return false;
    }

    public boolean contains(T t) {
        return size>0 && OR(e -> e == t || e.equals(t));
    }


    @Override
    public Node<T> remove(final T t, Nodelike<T> parent) {

        int i = 0;

        while (i < size && (data[i] != t) && (!data[i].equals(t))) {
            i++;
        }

        int j = i;

        while (j < size && ((data[j] == t) || data[j].equals(t))) {
            j++;
        }

        if (i < j) {
            final int nRemoved = j - i;
            if (j < size) {
                final int nRemaining = size - j;
                System.arraycopy(data, j, data, i, nRemaining);
                Arrays.fill(data, size-nRemoved, size, null);
            } else {
                Arrays.fill(data, i, size, null);
            }

            size -= nRemoved;
            parent.reportSizeDelta(-nRemoved);

            bounds = size > 0 ? HyperRect.mbr(data, builder) : null;

        }

        return this;

    }


    @Override
    public Node<T> update(final T told, final T tnew) {
        if (size <= 0)
            return this;

        for (int i = 0; i < size; i++) {
            if (data[i].equals(told)) {
                data[i] = tnew;
            }

            bounds = i == 0 ? builder.apply(data[0]) : bounds.mbr(builder.apply(data[i]));
        }

        return this;
    }


    @Override
    public boolean containing(HyperRect R, Predicate<T> t) {
        for (int i = 0; i < size; i++) {
            T d = data[i];
            if (R.contains(builder.apply(d))) {
                if (!t.test(d))
                    return false;
            }
        }
        return true;
    }

    @Override
    public int containing(final HyperRect R, final T[] t, int n) {
        final int tLen = t.length;
        final int n0 = n;

        for (int i = 0; i < size && n < tLen; i++) {
            T d = data[i];
            if (R.contains(builder.apply(d))) {
                t[n++] = d;
            }
        }
        return n - n0;
    }

    @Override
    public int size() {
        return size;
    }


    @Override
    public boolean isLeaf() {
        return true;
    }

    @NotNull
    @Override
    public HyperRect bounds() {
        return bounds;
    }

    /**
     * Splits a lead node that has the maximum number of entries into 2 leaf nodes of the same type with half
     * of the entries in each one.
     *
     * @param t entry to be added to the full leaf node
     * @return newly created node storing half the entries of this node
     */
    protected abstract Node<T> split(final T t);

    @Override
    public void forEach(Consumer<? super T> consumer) {
        for (int i = 0; i < size; i++) {
            consumer.accept(data[i]);
        }
    }

    @Override
    public boolean intersecting(HyperRect rect, Predicate<T> t) {
        for (int i = 0; i < size; i++) {
            T d = data[i];
            if (rect.intersects(this.builder.apply(d))) {
                if (!t.test(d))
                    return false;
            }
        }
        return true;
    }

    @Override
    public void collectStats(Stats stats, int depth) {
        if (depth > stats.getMaxDepth()) {
            stats.setMaxDepth(depth);
        }
        stats.countLeafAtDepth(depth);
        stats.countEntriesAtDepth(size, depth);
    }

    /**
     * Figures out which newly made leaf node (see split method) to add a data entry to.
     *
     * @param l1Node left node
     * @param l2Node right node
     * @param t      data entry to be added
     */
    protected final void classify(final Node<T> l1Node, final Node<T> l2Node, final T t) {

        final HyperRect tRect = builder.apply(t);
        final HyperRect l1Mbr = l1Node.bounds().mbr(tRect);

        double tCost = tRect.cost();

        double l1c = l1Mbr.cost();
        final double l1CostInc = Math.max(l1c - (l1Node.bounds().cost() + tCost), 0.0);
        final HyperRect l2Mbr = l2Node.bounds().mbr(tRect);
        double l2c = l2Mbr.cost();
        final double l2CostInc = Math.max(l2c - (l2Node.bounds().cost() + tCost), 0.0);
        if (l2CostInc > l1CostInc) {
            l1Node.add(t, this);
        } else if (RTree.equals(l1CostInc, l2CostInc)) {
            if (l1c < l2c) {
                l1Node.add(t, this);
            } else if (RTree.equals(l1c, l2c)) {
                final double l1MbrMargin = l1Mbr.perimeter();
                final double l2MbrMargin = l2Mbr.perimeter();
                if (l1MbrMargin < l2MbrMargin) {
                    l1Node.add(t, this);
                } else if (RTree.equals(l1MbrMargin, l2MbrMargin)) {
                    // break ties with least number
                    ((l1Node.size() < l2Node.size()) ? l1Node : l2Node).add(t, this);

                } else {
                    l2Node.add(t, this);
                }
            } else {
                l2Node.add(t, this);
            }
        } else {
            l2Node.add(t, this);
        }

    }

    @Override
    public Node<T> instrument() {
        return new CounterNode<>(this);
    }

    @Override
    public String toString() {
        return "Leaf" + splitType + '{' + bounds + 'x' + size + '}';
    }
}

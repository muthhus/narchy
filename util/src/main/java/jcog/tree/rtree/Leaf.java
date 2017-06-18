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
import java.util.function.Predicate;

/**
 * Node that will contain the data entries. Implemented by different type of SplitType leaf classes.
 * <p>
 * Created by jcairns on 4/30/15.
 *
 * TODO just extend FastList<>
 */
public class Leaf<T> implements Node<T, T> {

    public final T[] data;
    public short size;
    public HyperRegion bounds;

    protected Leaf(int mMax) {
        this.bounds = null;
        this.data = (T[]) new Object[mMax];
        this.size = 0;
    }

    @Override
    public T get(int i) {
        return data[i];
    }

    @Override
    public Node<T, ?> add(final T t, Nodelike<T> parent, Spatialization<T> model) {

        if (!contains(t, model)) {
            Node<T, ?> next;

            if (size < model.max) {
                final HyperRegion tRect = model.region(t);
                bounds = bounds != null ? bounds.mbr(tRect) : tRect;

                data[size++] = t;

                next = this;
            } else {
                next = model.split(t, this);
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

    @Override
    public boolean contains(T t, Spatialization<T> model) {
        return size>0 && OR(e -> e == t || e.equals(t));
    }


    @Override
    public Node<T, ?> remove(final T t, HyperRegion xBounds, Nodelike<T> parent, Spatialization<T> model) {

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

            bounds = size > 0 ? HyperRegion.mbr(model.bounds, data, size) : null;

        }

        return this;

    }

     @Override
    public double perimeter(Spatialization<T> model) {
        double maxVolume = 0;
        for (int i = 0; i < size; i++) {
            T c = data[i];
            double vol = model.perimeter(c);
            if (vol > maxVolume)
                maxVolume = vol;
        }
        return maxVolume;
    }


    @Override
    public Node<T, ?> update(final T told, final T tnew, Spatialization<T> model) {
        if (size <= 0)
            return this;

        for (int i = 0; i < size; i++) {
            if (data[i].equals(told)) {
                data[i] = tnew;
            }

            bounds = i == 0 ? model.region(data[0]) : bounds.mbr(model.region(data[i]));
        }

        return this;
    }


    @Override
    public boolean containing(HyperRegion R, Predicate<T> t, Spatialization<T> model) {
        for (int i = 0; i < size; i++) {
            T d = data[i];
            if (R.contains(model.region(d))) {
                if (!t.test(d))
                    return false;
            }
        }
        return true;
    }

    @Override
    public void intersectingNodes(HyperRegion rect, Predicate<Node<T, ?>> t, Spatialization<T> model) {

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
    public HyperRegion bounds() {
        return bounds;
    }


    @Override
    public void forEach(Consumer<? super T> consumer) {
        for (int i = 0; i < size; i++) {
            T d = data[i];
            if (d!=null)
                consumer.accept(d);
        }
    }

    @Override
    public boolean intersecting(HyperRegion rect, Predicate<T> t, Spatialization<T> model) {
        for (int i = 0; i < size; i++) {
            T d = data[i];
            if (rect.intersects(model.region(d))) {
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
     *  @param l1Node left node
     * @param l2Node right node
     * @param t      data entry to be added
     * @param model
     */
    public final void classify(final Node<T, T> l1Node, final Node<T, T> l2Node, final T t, Spatialization<T> model) {

        final HyperRegion tRect = model.region(t);
        final HyperRegion l1Mbr = l1Node.bounds().mbr(tRect);

        double tCost = tRect.cost();

        double l1c = l1Mbr.cost();
        final double l1CostInc = Math.max(l1c - (l1Node.bounds().cost() + tCost), 0.0);
        final HyperRegion l2Mbr = l2Node.bounds().mbr(tRect);
        double l2c = l2Mbr.cost();
        final double l2CostInc = Math.max(l2c - (l2Node.bounds().cost() + tCost), 0.0);
        if (l2CostInc > l1CostInc) {
            l1Node.add(t, this, model);
        } else if (RTree.equals(l1CostInc, l2CostInc)) {
            if (l1c < l2c) {
                l1Node.add(t, this, model);
            } else if (RTree.equals(l1c, l2c)) {
                final double l1MbrMargin = l1Mbr.perimeter();
                final double l2MbrMargin = l2Mbr.perimeter();
                if (l1MbrMargin < l2MbrMargin) {
                    l1Node.add(t, this, model);
                } else if (RTree.equals(l1MbrMargin, l2MbrMargin)) {
                    // break ties with least number
                    ((l1Node.size() < l2Node.size()) ? l1Node : l2Node).add(t, this, model);

                } else {
                    l2Node.add(t, this, model);
                }
            } else {
                l2Node.add(t, this, model);
            }
        } else {
            l2Node.add(t, this, model);
        }

    }

    @Override
    public Node<T, Object> instrument() {
        return new CounterNode<>(this);
    }

    @Override
    public String toString() {
        return "Leaf" + '{' + bounds + 'x' + size + '}';
    }
}

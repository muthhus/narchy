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


import com.google.common.base.Joiner;
import jcog.tree.rtree.util.CounterNode;
import jcog.tree.rtree.util.Stats;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * RTree node that contains leaf nodes
 * <p>
 * Created by jcairns on 4/30/15.
 */
public final class Branch<T> implements Node<T> {

    private final Node<T>[] child;
    private HyperRect mbr;
    private int size;


    //TODO move these to a shared builder class
    private final Function<T, HyperRect> builder;
    private final int mMax;
    private final int mMin;
    private final RTree.Split splitType;


    public Branch(final Function<T, HyperRect> builder, final int mMin, final int mMax, final RTree.Split splitType) {
        this.mMin = mMin;
        this.mMax = mMax;
        this.builder = builder;
        this.mbr = null;
        this.size = 0;
        this.child = new Node[mMax];
        this.splitType = splitType;
    }

    /**
     * Add a new node to this branch's list of children
     *
     * @param n node to be added (can be leaf or branch)
     * @return position of the added node
     */
    public int addChild(final Node<T> n) {
        if (size < mMax) {
            child[size++] = n;

            HyperRect nr = n.bounds();
            mbr = mbr != null ? mbr.mbr(nr) : nr;
            return size - 1;
        } else {
            throw new RuntimeException("Too many children");
        }
    }

    @Override
    public final boolean isLeaf() {
        return false;
    }

    @Override
    public HyperRect bounds() {
        return mbr;
    }

    /**
     * Adds a data entry to one of the child nodes of this branch
     *
     * @param t data entry to add
     * @return Node that the entry was added to
     */
    @Override
    public Node<T> add(final T t) {
        final HyperRect tRect = builder.apply(t);
        if (size < mMin) {
            for (int i = 0; i < size; i++) {
                Node ci = child[i];
                if (ci.bounds().contains(tRect)) {
                    child[i] = ci = ci.add(t);
                    grow(ci);
                    return ci;
                }
            }
            // no overlapping node - grow
            final Node<T> nextLeaf = splitType.newLeaf(builder, mMin, mMax);
            nextLeaf.add(t);
            final int nextChild = addChild(nextLeaf);
            grow(nextChild);

            return this;

        } else {
            final int bestLeaf = chooseLeaf(t, tRect);

            child[bestLeaf] = child[bestLeaf].add(t);

            grow(bestLeaf);

            // optimize on split to remove the extra created branch when there
            // is space for the children here
            if (child[bestLeaf].size() == 2 &&
                    size < mMax &&
                    !child[bestLeaf].isLeaf()) {
                final Branch<T> branch = (Branch<T>) child[bestLeaf];
                child[bestLeaf] = branch.child[0];
                child[size++] = branch.child[1];
            }

            return this;
        }
    }

    private void grow(int i) {
        grow(child[i]);
    }

    private void grow(Node<T> node) {
        mbr = mbr.mbr(node.bounds());
    }

    @Override
    public Node<T> remove(final T t) {
        final HyperRect tRect = builder.apply(t);

        for (int i = 0; i < size; i++) {
            if (child[i].bounds().intersects(tRect)) {
                child[i] = child[i].remove(t);

                if (child[i].size() == 0) {
                    System.arraycopy(child, i + 1, child, i, size - i - 1);
                    child[--size] = null;
                    if (size > 0) i--;
                }
            }
        }

        if (size > 0) {

            if (size == 1) {
                // unsplit branch
                return child[0];
            }

            mbr = child[0].bounds();
            for (int i = 1; i < size; i++) {
                grow(child[i]);
            }
        }

        return this;
    }

    public int childSize(int i) {
        Node<T> cc = child[i];
        if (cc == null)
            return 0;
        return cc.size();
    }

    @Override
    public Node<T> update(final T told, final T tnew) {
        final HyperRect tRect = builder.apply(told);
        for (int i = 0; i < size; i++) {
            if (tRect.intersects(child[i].bounds())) {
                child[i] = child[i].update(told, tnew);
            }
            if (i == 0) {
                mbr = child[0].bounds();
            } else {
                grow(child[i]);
            }
        }
        return this;

    }

    @Override
    public int containing(final HyperRect rect, final T[] t, int n) {
        final int tLen = t.length;
        final int n0 = n;
        for (int i = 0; i < size && n < tLen; i++) {
            Node c = child[i];
            if (rect.intersects(c.bounds())) {
                n += c.containing(rect, t, n);
            }
        }
        return n - n0;
    }

    @Override
    public boolean containing(final HyperRect rect, final Predicate<T> t) {

        for (int i = 0; i < size; i++) {
            Node c = child[i];
            if (rect.intersects(c.bounds())) {
                if (!c.containing(rect, t))
                    return false;
            }
        }
        return true;
    }

    /**
     * @return number of child nodes
     */
    @Override
    public int size() {
        return size;
    }

    private int chooseLeaf(final T t, final HyperRect tRect) {
        if (size > 0) {
            int bestNode = 0;
            HyperRect childMbr = child[0].bounds().mbr(tRect);
            double tCost = tRect.cost();
            double leastEnlargement = childMbr.cost() - (child[0].bounds().cost() + tCost);
            double leastPerimeter = childMbr.perimeter();

            for (int i = 1; i < size; i++) {
                childMbr = child[i].bounds().mbr(tRect);
                final double nodeEnlargement = childMbr.cost() - (child[i].bounds().cost() + tCost);
                if (nodeEnlargement < leastEnlargement) {
                    leastEnlargement = nodeEnlargement;
                    leastPerimeter = childMbr.perimeter();
                    bestNode = i;
                } else if (RTree.equals(nodeEnlargement, leastEnlargement)) {
                    final double childPerimeter = childMbr.perimeter();
                    if (childPerimeter < leastPerimeter) {
                        leastEnlargement = nodeEnlargement;
                        leastPerimeter = childPerimeter;
                        bestNode = i;
                    }
                } // else its not the least

            }
            return bestNode;
        } else {
            final Node<T> n = splitType.newLeaf(builder, mMin, mMax);
            n.add(t);
            child[size++] = n;

            mbr = (mbr == null) ? n.bounds() : mbr.mbr(n.bounds());

            return size - 1;
        }
    }

    /**
     * Return child nodes of this branch.
     *
     * @return array of child nodes (leaves or branches)
     */
    public Node<T>[] children() {
        return child;
    }

    @Override
    public void forEach(Consumer<T> consumer) {
        for (int i = 0; i < size; i++) {
            child[i].forEach(consumer);
        }
    }

    @Override
    public boolean AND(Predicate<T> p) {
        for (int i = 0; i < size; i++) {
            if (!child[i].AND(p))
                return false;
        }
        return true;
    }

    public boolean nodeAND(Predicate<Node<T>> p) {
        for (int i = 0; i < size; i++) {
            if (!p.test(child[i]))
                return false;
        }
        return true;
    }

    @Override
    public boolean OR(Predicate<T> p) {
        for (int i = 0; i < size; i++)
            if (child[i].OR(p))
                return true;
        return false;
    }

    @Override
    public boolean intersecting(HyperRect rect, Predicate<T> consumer) {
        return nodeAND(ci -> !(rect.intersects(ci.bounds()) && !ci.intersecting(rect, consumer)));
    }

    @Override
    public void collectStats(Stats stats, int depth) {
        for (int i = 0; i < size; i++)
            child[i].collectStats(stats, depth + 1);
        stats.countBranchAtDepth(depth);
    }

    @Override
    public Node<T> instrument() {
        for (int i = 0; i < size; i++)
            child[i] = child[i].instrument();
        return new CounterNode<>(this);
    }

    @Override
    public String toString() {
        return "Branch" + splitType + "{" + mbr + "x" + size + ":\n\t" +
                (child != null ? Joiner.on("\n\t").skipNulls().join(child) : "null") + "\n}";
    }
}

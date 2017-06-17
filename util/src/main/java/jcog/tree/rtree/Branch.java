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
import org.jetbrains.annotations.NotNull;
import org.roaringbitmap.RoaringBitmap;

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

    //TODO move these to a shared builder class
    @Deprecated private final Function<T, HyperRect> builder;
    @Deprecated private final int mMax;
    @Deprecated private final int mMin;
    @Deprecated private final RTree.Split splitType;

    short childDiff;
    private HyperRect mbr;
    short size;

    public Branch(final Function<T, HyperRect> builder, final int mMin, final int mMax, final RTree.Split splitType) {
        this.mMin = mMin;
        this.mMax = mMax;
        this.builder = builder;
        this.mbr = null;
        this.size = 0;
        this.child = new Node[mMax];
        this.splitType = splitType;
    }

    @Override
    public boolean contains(T t) {
        if (!bounds().contains(builder.apply(t)))
            return false;
        for (int i = 0; i < size; i++) {
            if (child[i].contains(t))
                return true;
        }
        return false;
    }

    @Override
    public void reportSizeDelta(int i) {
        childDiff += i;
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

    @NotNull
    @Override
    public HyperRect bounds() {
        return mbr;
    }

    /**
     * Adds a data entry to one of the child nodes of this branch
     *
     * @param t      data entry to add
     * @param parent
     * @return Node that the entry was added to
     */
    @Override
    public Node<T> add(final T t, Nodelike<T> parent) {
        assert (childDiff == 0);

        final HyperRect tRect = builder.apply(t);

        for (int i = 0; i < size; i++) {
            Node ci = child[i];
            if (ci.bounds().contains(tRect)) {
                //check for existing item
                if (ci.contains(t))
                    return this; // duplicate detected (subtree not changed)

                Node<T> m = ci.add(t, this);
                if (reportNextSizeDelta(parent)) {
                    child[i] = m;
                    grow(m); //subtree was changed
                    return this;
                }
            }
        }

        if (size < mMin) {

            // no overlapping node - grow
            grow(addChild(splitType.newLeaf(builder, mMin, mMax).add(t, parent)));

            return this;

        } else {

            final int bestLeaf = chooseLeaf(t, tRect, parent);

            child[bestLeaf] = child[bestLeaf].add(t, this);

            if (reportNextSizeDelta(parent)) {
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

            } else {
                //duplicate was found in sub-tree but we checked for duplicates above
                assert(false);
            }

            return this;
        }
    }

    private boolean reportNextSizeDelta(Nodelike<T> parent) {
        int x = childDiff;
        if (x == 0)
            return false; //nothing changed

        this.childDiff = 0; //clear
        parent.reportSizeDelta(x);
        return true;
    }

    private void grow(int i) {
        grow(child[i]);
    }

    private void grow(Node<T> node) {
        mbr = mbr.mbr(node.bounds());
    }

    @Override
    public Node<T> remove(final T t, Nodelike<T> parent) {
        final HyperRect tRect = builder.apply(t);

        for (int i = 0; i < size; i++) {
            if (child[i].bounds().intersects(tRect)) {
                child[i] = child[i].remove(t, this);
                if (reportNextSizeDelta(parent)) {
                    if (child[i].size() == 0) {
                        System.arraycopy(child, i + 1, child, i, size - i - 1);
                        child[--size] = null;
                        if (size > 0) i--;
                    }
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

//    public int childSize(int i) {
//        Node<T> cc = child[i];
//        if (cc == null)
//            return 0;
//        return cc.size();
//    }

    @Override
    public Node<T> update(final T OLD, final T NEW) {
        final HyperRect tRect = builder.apply(OLD);

        //TODO may be able to avoid recomputing bounds if the old was not found
        boolean found = false;
        for (int i = 0; i < size; i++) {
            if (!found && tRect.intersects(child[i].bounds())) {
                child[i] = child[i].update(OLD, NEW);
                found = true;
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

    private int chooseLeaf(final T t, final HyperRect tRect, Nodelike<T> parent) {
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
            child[size++] = n;
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
    public void forEach(Consumer<? super T> consumer) {
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

    boolean nodeAND(Predicate<Node<T>> p) {
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
        return "Branch" + splitType + '{' + mbr + 'x' + size + ":\n\t" + Joiner.on("\n\t").skipNulls().join(child) + "\n}";
    }
}

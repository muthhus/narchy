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

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * RTree node that contains leaf nodes
 * <p>
 * Created by jcairns on 4/30/15.
 */
public final class Branch<T> implements Node<T, Node<T,?>> {

    private final Node<T, ?>[] child;

    short childDiff;
    private HyperRegion region;
    short size;

    public Branch(int cap) {
        this.region = null;
        this.size = 0;
        this.child = new Node[cap];
    }

    @Override
    public boolean contains(T t, Spatialization<T> model) {
        if (!region().contains(model.region(t)))
            return false;
        for (int i = 0; i < size; i++) {
            if (child[i].contains(t, model))
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
    public int addChild(@NotNull final Node<T, ?> n) {
        if (size < child.length) {
            child[size++] = n;

            HyperRegion nr = n.region();
            region = region != null ? region.mbr(nr) : nr;
            return size - 1;
        } else {
            throw new RuntimeException("Too many children");
        }
    }

    @Override
    public Node<T, ?> get(int i) {
        return child[i];
    }

    @Override
    public final boolean isLeaf() {
        return false;
    }

    @NotNull
    @Override
    public HyperRegion region() {
        return region;
    }

    /**
     * Adds a data entry to one of the child nodes of this branch
     *
     * @param t      data entry to add
     * @param parent
     * @param model
     * @return Node that the entry was added to
     */
    @Override
    public Node<T, ?> add(@NotNull final T t, Nodelike<T> parent, Spatialization<T> model) {
        assert (childDiff == 0);

        final HyperRegion tRect = model.region(t);

        //MERGE STAGE:
        for (int i = 0; i < size; i++) {
            Node ci = child[i];
            if (ci.region().contains(tRect)) {
                //check for existing item
//                if (ci.contains(t, model))
//                    return this; // duplicate detected (subtree not changed)

                Node<T, ?> m = ci.add(t, null, model);
                if (m == null)
                    return null; //merged
//                if (reportNextSizeDelta(parent)) {
//                    child[i] = m;
//                    grow(m); //subtree was changed
//                    return this;
//                }
            }
        }

        if (parent == null)
            return this; //done for this stage


        //INSERTION STAGE:

        if (size < child.length) {

            // no overlapping node - grow
            grow(addChild(model.newLeaf().add(t, parent, model)));

            return this;

        } else {

            final int bestLeaf = chooseLeaf(t, tRect, parent, model);

            Node<T, ?> nextBest = child[bestLeaf].add(t, this, model);
            if (nextBest == null) {
                return null; //merged
            }

            child[bestLeaf] = nextBest;

            if (reportNextSizeDelta(parent)) {
                grow(bestLeaf);

                // optimize on split to remove the extra created branch when there
                // is space for the children here
                if (child[bestLeaf].size() == 2 &&
                        size < child.length &&
                        !child[bestLeaf].isLeaf()) {
                    final Branch<T> branch = (Branch<T>) child[bestLeaf];
                    child[bestLeaf] = branch.child[0];
                    child[size++] = branch.child[1];
                }

            } else {
                //? duplicate was found in sub-tree but we checked for duplicates above
                assert(false): "what to do with: " + t + " in " + parent;
                //probably ok, just merged with a subbranch?
                //return null;
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

    private void grow(Node<T, ?> node) {
        region = region.mbr(node.region());
    }

    @Override
    public Node<T, ?> remove(final T x, HyperRegion xBounds, Nodelike<T> parent, Spatialization<T> model) {

        for (int i = 0; i < size; i++) {
            if (child[i].region().intersects(xBounds)) {
                child[i] = child[i].remove(x, xBounds, this, model);
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

            region = child[0].region();
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
    public Node<T, ?> update(final T OLD, final T NEW, Spatialization<T> model) {
        final HyperRegion tRect = model.region(OLD);

        //TODO may be able to avoid recomputing bounds if the old was not found
        boolean found = false;
        for (int i = 0; i < size; i++) {
            if (!found && tRect.intersects(child[i].region())) {
                child[i] = child[i].update(OLD, NEW, model);
                found = true;
            }
            if (i == 0) {
                region = child[0].region();
            } else {
                grow(child[i]);
            }
        }

        return this;

    }



    @Override
    public boolean containing(final HyperRegion rect, final Predicate<T> t, Spatialization<T> model) {

        for (int i = 0; i < size; i++) {
            Node c = child[i];
            if (rect.intersects(c.region())) {
                if (!c.containing(rect, t, model))
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

    private int chooseLeaf(final T t, final HyperRegion tRect, Nodelike<T> parent, Spatialization<T> model) {
        if (size > 0) {
            int bestNode = -1;
            double tCost = Double.POSITIVE_INFINITY;
            double leastEnlargement = Double.POSITIVE_INFINITY;
            double leastPerimeter = Double.POSITIVE_INFINITY;

            for (int i = 0; i < size; i++) {
                HyperRegion cir = child[i].region();
                HyperRegion childMbr = cir.mbr(tRect);
                final double nodeEnlargement = childMbr.cost() - (cir.cost() + tCost);
                double perimeter = childMbr.perimeter();
                if (nodeEnlargement < leastEnlargement) {
                    leastEnlargement = nodeEnlargement;
                    leastPerimeter = perimeter;
                    bestNode = i;
                } else if (RTree.equals(nodeEnlargement, leastEnlargement)) {
                    final double childPerimeter = perimeter;
                    if (childPerimeter < leastPerimeter) {
                        leastEnlargement = nodeEnlargement;
                        leastPerimeter = childPerimeter;
                        bestNode = i;
                    }
                } // else its not the least

            }
            assert(bestNode != -1);
            return bestNode;
        } else {
            final Node<T, ?> n = model.newLeaf();
            child[size++] = n;
            return size - 1;
        }
    }

    /**
     * Return child nodes of this branch.
     *
     * @return array of child nodes (leaves or branches)
     */
    public Node<T, ?>[] children() {
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

    boolean nodeAND(Predicate<Node<T, ?>> p) {
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
    public boolean intersecting(HyperRegion rect, Predicate<T> t, Spatialization<T> model) {
        return nodeAND(ci -> !(rect.intersects(ci.region()) && !ci.intersecting(rect, t, model)));
    }

    @Override
    public void intersectingNodes(HyperRegion rect, Predicate<Node<T, ?>> t, Spatialization<T> model) {
        if (!region.intersects(rect) || !t.test(this))
            return;

        Node<T, ?>[] children = this.child;
        short s = this.size;
        for (int i = 0; i < s; i++) {
            Node<T, ?> c = children[i];
            if (c!=null)
                c.intersectingNodes(rect, t, model);
        }
    }

    @Override
    public void collectStats(Stats stats, int depth) {
        for (int i = 0; i < size; i++)
            child[i].collectStats(stats, depth + 1);
        stats.countBranchAtDepth(depth);
    }

    @Override
    public Node<T, ?> instrument() {
        for (int i = 0; i < size; i++)
            child[i] = child[i].instrument();
        return new CounterNode<>(this);
    }

    @Override
    public String toString() {
        return "Branch" + '{' + region + 'x' + size + ":\n\t" + Joiner.on("\n\t").skipNulls().join(child) + "\n}";
    }



    @Override
    public double perimeter(Spatialization<T> model) {
        double maxVolume = 0;
        for (int i = 0; i < size; i++) {
            Node<T, ?> c = child[i];
            double vol = c.perimeter(model);
            if (vol > maxVolume)
                maxVolume = vol;
        }
        return maxVolume;
    }
}

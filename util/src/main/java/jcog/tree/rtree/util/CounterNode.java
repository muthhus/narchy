package jcog.tree.rtree.util;

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

import jcog.tree.rtree.HyperRegion;
import jcog.tree.rtree.Node;
import jcog.tree.rtree.Nodelike;
import jcog.tree.rtree.Spatialization;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by jcovert on 6/18/15.
 */
public final class CounterNode<T> implements Node<T, Object> {
    public static int searchCount;
    public static int bboxEvalCount;
    private final Node<T, ?> node;

    public CounterNode(final Node<T, ?> node) {
        this.node = node;
    }

    @Override
    public Object get(int i) {
        return node.get(i);
    }

    @Override
    public boolean isLeaf() {
        return this.node.isLeaf();
    }

    @Override
    public HyperRegion bounds() {
        return this.node.bounds();
    }

    @Override
    public Node<T, ?> add(T t, Nodelike<T> parent, Spatialization<T> model) {
        return this.node.add(t, this, model);
    }

    @Override
    public Node<T, ?> remove(T x, HyperRegion xBounds, Nodelike<T> parent, Spatialization<T> model) {
        return this.node.remove(x, xBounds, this, model);
    }

    @Override
    public Node<T, ?> update(T told, T tnew, Spatialization<T> model) {
        return this.node.update(told, tnew, model);
    }

    @Override
    public boolean AND(Predicate<T> p) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public boolean OR(Predicate<T> p) {
        throw new UnsupportedOperationException("TODO");
    }


    @Override
    public boolean containing(HyperRegion rect, Predicate<T> t, Spatialization<T> model) {
        searchCount++;
        bboxEvalCount += this.node.size();
        return this.node.containing(rect, t, model);
    }

    @Override
    public void intersectingNodes(HyperRegion rect, Predicate<Node<T, ?>> t, Spatialization<T> model) {
        node.intersectingNodes(rect, t, model);
    }

    @Override
    public int size() {
        return this.node.size();
    }

    @Override
    public void forEach(Consumer<? super T> consumer) {
        this.node.forEach(consumer);
    }

    @Override
    public boolean intersecting(HyperRegion rect, Predicate<T> t, Spatialization<T> model) {
        this.node.intersecting(rect, t, model);
        return false;
    }

    @Override
    public void collectStats(Stats stats, int depth) {
        this.node.collectStats(stats, depth);
    }

    @Override
    public Node<T, ?> instrument() {
        return this;
    }

    @Override
    public double perimeter(Spatialization<T> model) {
        return node.perimeter(model);
    }

    @Override
    public void reportSizeDelta(int i) {
        node.reportSizeDelta(i);
    }

    @Override
    public boolean contains(T t, Spatialization<T> model) {
        return node.contains(t, model);
    }
}

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

import jcog.tree.rtree.HyperRect;
import jcog.tree.rtree.Node;
import jcog.tree.rtree.Nodelike;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by jcovert on 6/18/15.
 */
public final class CounterNode<T> implements Node<T> {
    public static int searchCount;
    public static int bboxEvalCount;
    private final Node<T> node;

    public CounterNode(final Node<T> node) {
        this.node = node;
    }

    @Override
    public boolean isLeaf() {
        return this.node.isLeaf();
    }

    @Override
    public HyperRect bounds() {
        return this.node.bounds();
    }

    @Override
    public Node<T> add(T t, Nodelike<T> parent) {
        return this.node.add(t, this);
    }

    @Override
    public Node<T> remove(T t, Nodelike<T> parent) {
        return this.node.remove(t, this);
    }

    @Override
    public Node<T> update(T told, T tnew) {
        return this.node.update(told, tnew);
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
    public int containing(HyperRect rect, T[] t, int n) {
        searchCount++;
        bboxEvalCount += this.node.size();
        return this.node.containing(rect, t, n);
    }

    @Override
    public boolean containing(HyperRect rect, Predicate<T> t) {
        searchCount++;
        bboxEvalCount += this.node.size();
        return this.node.containing(rect, t);
    }

    @Override
    public int size() {
        return this.node.size();
    }

    @Override
    public void forEach(Consumer<T> consumer) {
        this.node.forEach(consumer);
    }

    @Override
    public boolean intersecting(HyperRect rect, Predicate<T> consumer) {
        this.node.intersecting(rect, consumer);
        return false;
    }

    @Override
    public void collectStats(Stats stats, int depth) {
        this.node.collectStats(stats, depth);
    }

    @Override
    public Node<T> instrument() {
        return this;
    }

    @Override
    public void reportSizeDelta(int i) {
        node.reportSizeDelta(i);
    }

    @Override
    public boolean contains(T t) {
        return node.contains(t);
    }
}

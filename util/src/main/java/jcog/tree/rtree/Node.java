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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by jcairns on 4/30/15.
 */
public interface Node<T> {
    /**
     * @return boolean - true if this node is a leaf
     */
    boolean isLeaf();

    /**
     * @return Rect - the bounding rectangle for this node
     */
    @NotNull HyperRect bounds();

    /**
     * Add t to the index
     *
     * @param t - value to add to index
     */
    @NotNull Node<T> add(T t);

    /**
     * Remove t from the index
     *
     * @param t - value to remove from index
     */
    @NotNull Node<T> remove(T t);

    /**
     * update an existing t in the index
     *
     * @param told - old index to be updated
     * @param tnew - value to update old index to
     */
    Node<T> update(T told, T tnew);

    /**
     * Search for rect within this node
     *
     * @param rect - HyperRect to search for
     * @param t    - array of found results
     * @param n    - total result count so far (from recursive call)
     * @return result count from search of this node
     */
    int containing(HyperRect rect, T[] t, int n);


    /**
     * The number of entries in the node
     *
     * @return entry count
     */
    int size();

    /**
     * Consumer "accepts" every node in the entire index
     *
     * @param consumer
     */
    void forEach(Consumer<T> consumer);

    boolean AND(Predicate<T> p);
    boolean OR(Predicate<T> p);

    /**
     * Consumer "accepts" every node in the given rect
     *  @param rect     - limiting rect
     *  @param consumer
     *  @return whether to continue visit
     */
    boolean intersecting(HyperRect rect, Predicate<T> consumer);

    boolean containing(HyperRect rect, Predicate<T> t);

    default <C extends Collection> C containing(HyperRect rect, C t) {
        containing(rect, (Predicate)x -> {t.add(x); return true; });
        return t;
    }
    default Set<T> containedSet(HyperRect rect) {
        return containing(rect, new HashSet());
    }

    /**
     * Recurses over index collecting stats
     *
     * @param stats - Stats object being populated
     * @param depth - current depth in tree
     */
    void collectStats(Stats stats, int depth);

    /**
     * Visits node, wraps it in an instrumented node, (see CounterNode)
     *
     * @return instrumented node wrapper
     */
    Node<T> instrument();
}

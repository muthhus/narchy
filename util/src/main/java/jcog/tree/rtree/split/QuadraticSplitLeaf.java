package jcog.tree.rtree.split;

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

import jcog.tree.rtree.*;

/**
 * Guttmann's Quadratic split
 * <p>
 * Created by jcairns on 5/5/15.
 */
public final class QuadraticSplitLeaf<T> implements Split<T> {

    @Override
    public Node<T> split(T t, Leaf<T> leaf, Spatialization<T> model) {

        final Branch<T> pNode = model.newBranch();

        final Node<T> l1Node = model.newLeaf();
        final Node<T> l2Node = model.newLeaf();

        // find the two rectangles that are most wasteful
        double minCost = Double.MIN_VALUE;
        short size = leaf.size;
        int r1Max = 0, r2Max = size - 1;
        T[] data = leaf.data;
        for (int i = 0; i < size; i++) {
            HyperRegion ii = model.region(data[i]);
            for (int j = i + 1; j < size; j++) {
                HyperRegion jj = model.region(data[j]);
                final HyperRegion mbr = ii.mbr(jj);
                final double cost = mbr.cost() - (ii.cost() + jj.cost());
                if (cost > minCost) {
                    r1Max = i;
                    r2Max = j;
                    minCost = cost;
                }
            }
        }

        // two seeds
        l1Node.add(data[r1Max], leaf, model);
        l2Node.add(data[r2Max], leaf, model);

        for (int i = 0; i < size; i++) {
            if ((i != r1Max) && (i != r2Max)) {
                // classify with respect to nodes
                leaf.classify(l1Node, l2Node, data[i], model);
            }
        }

        leaf.classify(l1Node, l2Node, t, model);

        pNode.addChild(l1Node);
        pNode.addChild(l2Node);

        return pNode;
    }

}

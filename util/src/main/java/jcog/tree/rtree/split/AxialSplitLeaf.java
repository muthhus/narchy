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

import java.util.Arrays;
import java.util.Comparator;

/**
 * Fast RTree split suggested by Yufei Tao taoyf@cse.cuhk.edu.hk
 * <p>
 * Perform an axial split
 * <p>
 * Created by jcairns on 5/5/15.
 */
public final class AxialSplitLeaf<T> implements Split<T> {


    @Override
    public Node<T> split(T t, Leaf<T> leaf, Spatialization<T> model) {
        final Branch<T> pNode = model.newBranch();

        final int nD = model.region(leaf.data[0]).dim(); //TODO builder.dim()

        // choose axis to split
        int axis = 0;
        double rangeD = leaf.bounds.range(0);
        for (int d = 1; d < nD; d++) {
            // split along the greatest range extent
            final double dr = leaf.bounds.rangeIfFinite(d, 0);
            if (dr > rangeD) {
                axis = d;
                rangeD = dr;
            }
        }

        // sort along split dimension
        final int splitDimension = axis;

        short size = leaf.size;
        final HyperRegion[] sortedMbr = HyperRegion.toArray(leaf.data, size, model.bounds);
        Arrays.sort(sortedMbr, Comparator.comparingDouble(o -> o.center(splitDimension)));

        // divide sorted leafs
        final Node<T> l1Node = model.newLeaf();
        transfer(leaf, sortedMbr, l1Node, 0, size / 2, model);

        final Node<T> l2Node = model.newLeaf();
        transfer(leaf, sortedMbr, l2Node, size / 2, size, model);

        leaf.classify(l1Node, l2Node, t, model);

        pNode.addChild(l1Node);
        pNode.addChild(l2Node);

        return pNode;
    }


    private static <T> void transfer(Leaf<T> leaf, HyperRegion[] sortedSrc, Node<T> target, int from, int to, Spatialization<T> model) {

        for (int j = 0; j < leaf.size; j++) {
            T jd = leaf.data[j];
            HyperRegion jr = model.region(jd);

            for (int i = from; i < to; i++) {
                HyperRegion si = sortedSrc[i];

                if (si!=null && jr.equals(si)) {
                    target.add(jd, leaf, model);
                    sortedSrc[i] = null;
                    break;
                }
            }
        }
        assert (target.size() == (to - from)) : target.size() + " isnt " + (to - from) + " " + Arrays.toString(leaf.data) + " -> " + Arrays.toString(sortedSrc);
    }


}

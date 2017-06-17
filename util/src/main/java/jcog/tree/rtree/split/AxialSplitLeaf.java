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
public final class AxialSplitLeaf<T> extends Leaf<T> {

    public AxialSplitLeaf(int cap) {
        super(cap);
    }

    @Override
    protected Node<T> split(final T t, RTreeModel<T> model) {
        final Branch<T> pNode = model.newBranch();

        final int nD = model.builder.apply(data[0]).dim(); //TODO builder.dim()

        // choose axis to split
        int axis = 0;
        double rangeD = bounds.getRange(0);
        for (int d = 1; d < nD; d++) {
            // split along the greatest range extent
            final double dr = bounds.getRangeFinite(d, 0);
            if (dr > rangeD) {
                axis = d;
                rangeD = dr;
            }
        }

        // sort along split dimension
        final int splitDimension = axis;
        final HyperRect[] sortedMbr = HyperRect.toArray(data, size, model.builder);
        Arrays.sort(sortedMbr, Comparator.comparingDouble(o -> o.center(splitDimension)));

        // divide sorted leafs
        final Node<T> l1Node = model.newLeaf();
        transfer(sortedMbr, l1Node, 0, size / 2, model);

        final Node<T> l2Node = model.newLeaf();
        transfer(sortedMbr, l2Node, size / 2, size, model);

        classify(l1Node, l2Node, t, model);

        pNode.addChild(l1Node);
        pNode.addChild(l2Node);

        return pNode;
    }


    private void transfer(HyperRect[] sortedSrc, Node<T> target, int from, int to, RTreeModel<T> model) {

        for (int j = 0; j < size; j++) {
            T jd = data[j];
            HyperRect jr = model.builder.apply(jd);

            for (int i = from; i < to; i++) {
                HyperRect si = sortedSrc[i];

                if (si!=null && jr.equals(si)) {
                    target.add(jd, this, model);
                    sortedSrc[i] = null;
                    break;
                }
            }
        }
        assert (target.size() == (to - from)) : target.size() + " isnt " + (to - from) + " " + Arrays.toString(data) + " -> " + Arrays.toString(sortedSrc);
    }

}

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
import java.util.function.Function;

/**
 * Fast RTree split suggested by Yufei Tao taoyf@cse.cuhk.edu.hk
 * <p>
 * Perform an axial split
 * <p>
 * Created by jcairns on 5/5/15.
 */
public final class AxialSplitLeaf<T> extends Leaf<T> {

    public AxialSplitLeaf(final Function<T, HyperRect> builder, final int mMin, final int mMax) {
        super(builder, mMin, mMax, RTree.Split.AXIAL);
    }

    @Override
    protected Node<T> split(final T t) {
        final Branch<T> pNode = new Branch<>(builder, mMin, mMax, splitType);


        final int nD = builder.apply(data[0]).dim(); //TODO builder.dim()

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
        final HyperRect[] sortedMbr = HyperRect.toArray(data, size, builder);
        Arrays.sort(sortedMbr, Comparator.comparingDouble(o -> o.center(splitDimension)));

        // divide sorted leafs
        final Node<T> l1Node = splitType.newLeaf(builder, mMin, mMax);
        transfer(sortedMbr, l1Node, 0, size / 2);

        final Node<T> l2Node = splitType.newLeaf(builder, mMin, mMax);
        transfer(sortedMbr, l2Node, size / 2, size);

        classify(l1Node, l2Node, t);

        pNode.addChild(l1Node);
        pNode.addChild(l2Node);

        return pNode;
    }


    private void transfer(HyperRect[] sortedSrc, Node<T> target, int from, int to) {
        for (int i = from; i < to; i++) {
            HyperRect si = sortedSrc[i];

            outerLoop:
            for (int j = 0; j < size; j++) {
                T d = data[j];
                if (builder.apply(d).equals( si )) {
                    target.add(d, this);
                    break outerLoop;
                }
            }
        }
    }

}

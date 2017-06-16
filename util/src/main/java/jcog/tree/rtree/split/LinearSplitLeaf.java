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

import java.util.function.Function;

/**
 * Guttmann's Linear split
 * <p>
 * Created by jcairns on 5/5/15.
 */
public final class LinearSplitLeaf<T> extends Leaf<T> {

    public LinearSplitLeaf(final Function<T, HyperRect> builder, final int mMin, final int mMax) {
        super(builder, mMin, mMax, RTree.Split.LINEAR);
    }

    @Override
    protected Node<T> split(final T t) {
        final Branch<T> pNode = new Branch<>(builder, mMin, mMax, splitType);
        final Node<T> l1Node = splitType.newLeaf(builder, mMin, mMax);
        final Node<T> l2Node = splitType.newLeaf(builder, mMin, mMax);

        final int MIN = 0;
        final int MAX = 1;
        final int NRANGE = 2;
        final int nD = rect[0].dim();
        final int[][][] rIndex = new int[nD][NRANGE][NRANGE];
        // separation between min and max extremes
        final double[] separation = new double[nD];

        for (int d = 0; d < nD; d++) {

//            rIndex[d][MIN][MIN] = 0;
//            rIndex[d][MIN][MAX] = 0;
//            rIndex[d][MAX][MIN] = 0;
//            rIndex[d][MAX][MAX] = 0;

            for (int j = 1; j < size; j++) {
                int[][] rd = rIndex[d];

                HyperRect rj = rect[j];
                Comparable rjMin = rj.min().coord(d);
                if (rect[rd[MIN][MIN]].min().coord(d).compareTo(rjMin) > 0) {
                    rd[MIN][MIN] = j;
                }

                if (rect[rd[MIN][MAX]].min().coord(d).compareTo(rjMin) < 0) {
                    rd[MIN][MAX] = j;
                }

                Comparable rjMax = rj.max().coord(d);
                if (rect[rd[MAX][MIN]].max().coord(d).compareTo(rjMax) > 0) {
                    rd[MAX][MIN] = j;
                }

                if (rect[rd[MAX][MAX]].max().coord(d).compareTo(rjMax) < 0) {
                    rd[MAX][MAX] = j;
                }
            }

            // highest max less lowest min
            final double width = rect[rIndex[d][MAX][MAX]].max().distance(rect[rIndex[d][MIN][MIN]].min(), d);

            // lowest max less highest min (normalized)
            separation[d] = rect[rIndex[d][MAX][MIN]].max().distance(rect[rIndex[d][MIN][MAX]].min(), d) / width;
        }

        int r1Ext = rIndex[0][MAX][MIN], r2Ext = rIndex[0][MIN][MAX];
        double highSep = separation[0];
        for (int d = 1; d < nD; d++) {
            if (highSep < separation[d]) {
                highSep = separation[d];
                r1Ext = rIndex[d][MAX][MIN];
                r2Ext = rIndex[d][MIN][MAX];
            }
        }

        if (r1Ext == r2Ext) {
            // they are not separated - arbitrarily choose the first and the last
            r1Ext = 0;
            r2Ext = size - 1;
        }

        // two seeds
        l1Node.add(data[r1Ext], this);
        l2Node.add(data[r2Ext], this);

        for (int i = 0; i < size; i++) {
            if ((i != r1Ext) && (i != r2Ext)) {
                // classify with respect to nodes
                classify(l1Node, l2Node, data[i]);
            }
        }

        classify(l1Node, l2Node, t);

        pNode.addChild(l1Node);
        pNode.addChild(l2Node);

        return pNode;
    }
}

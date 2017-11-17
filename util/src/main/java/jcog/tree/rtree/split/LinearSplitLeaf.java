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
 * Guttmann's Linear split
 * <p>
 * Created by jcairns on 5/5/15.
 */
public final class LinearSplitLeaf<T> implements Split<T> {

    @Override
    public Node<T, ?> split(T t, Leaf<T> leaf, Spatialization<T> model) {

        boolean[] dummy = new boolean[1];

        final Branch<T> pNode = model.newBranch();
        final Node<T, T> l1Node = model.newLeaf();
        final Node<T, T> l2Node = model.newLeaf();

        final int MIN = 0;
        final int MAX = 1;
        final int NRANGE = 2;
        T[] data = leaf.data;
        final int nD = model.region(data[0]).dim();
        final int[][][] rIndex = new int[nD][NRANGE][NRANGE];
        // separation between min and max extremes
        final double[] separation = new double[nD];

        short size = leaf.size;
        for (int d = 0; d < nD; d++) {

//            rIndex[d][MIN][MIN] = 0;
//            rIndex[d][MIN][MAX] = 0;
//            rIndex[d][MAX][MIN] = 0;
//            rIndex[d][MAX][MAX] = 0;


            for (int j = 1; j < size; j++) {
                int[][] rd = rIndex[d];

                HyperRegion rj = model.region(data[j]);
                double rjMin = rj.coord(false, d);
                if (model.region(data[rd[MIN][MIN]]).coord(false, d) > rjMin) { //TODO comparison order
                    rd[MIN][MIN] = j;
                }

                if (model.region(data[rd[MIN][MAX]]).coord(false, d) < rjMin) { //TODO comparison order (opposite previous)
                    rd[MIN][MAX] = j;
                }

                double rjMax = rj.coord(true, d);
                if (model.region(data[rd[MAX][MIN]]).coord(true, d) > rjMax) {
                    rd[MAX][MIN] = j;
                }

                if (model.region(data[rd[MAX][MAX]]).coord(true, d) < rjMax) {
                    rd[MAX][MAX] = j;
                }
            }

//                        // highest max less lowest min
//            final double width = model.bounds(data[rIndex[d][MAX][MAX]]).max().distance(model.bounds(data[rIndex[d][MIN][MIN]]).min(), d);
//
//            // lowest max less highest min (normalized)
//            separation[d] = model.bounds(data[rIndex[d][MAX][MIN]]).max().distance(model.bounds(data[rIndex[d][MIN][MAX]]).min(), d) / width;

            // highest max less lowest min
            final double width = model.region(data[rIndex[d][MAX][MAX]]).
                    distance(model.region(data[rIndex[d][MIN][MIN]]), d, true, false);


            // lowest max less highest min (normalized)
            separation[d] = model.region(data[rIndex[d][MAX][MIN]]).distance(model.region(data[rIndex[d][MIN][MAX]]), d, true, false) / width;
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
        l1Node.add(data[r1Ext], leaf, model, dummy);
        l2Node.add(data[r2Ext], leaf, model, dummy);

        for (int i = 0; i < size; i++) {
            if ((i != r1Ext) && (i != r2Ext)) {
                // classify with respect to nodes
                leaf.transfer(l1Node, l2Node, data[i], model);
            }
        }

        leaf.transfer(l1Node, l2Node, t, model);

        pNode.addChild(l1Node);
        pNode.addChild(l2Node);

        return pNode;
    }


}

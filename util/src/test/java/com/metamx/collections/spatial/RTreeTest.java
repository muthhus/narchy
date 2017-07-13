/*
 * Copyright 2011 - 2015 Metamarkets Group Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metamx.collections.spatial;

import com.metamx.collections.bitmap.BitmapFactory;
import com.metamx.collections.bitmap.RoaringBitmapFactory;
import com.metamx.collections.spatial.split.LinearGutmanSplitStrategy;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 */
public class RTreeTest {
    private RTree R;

    @Before
    public void setUp() throws Exception {
        BitmapFactory rbf = new RoaringBitmapFactory();
        R = new RTree(2, new LinearGutmanSplitStrategy(0, 50, rbf), rbf);

    }

//    @Test
//    public void testInsertNoSplit() {
//        float[] elem = new float[]{5, 5};
//        tree.insert(elem, 1);
//        Assert.assertTrue(Arrays.equals(elem, tree.getRoot().getMinCoordinates()));
//        Assert.assertTrue(Arrays.equals(elem, tree.getRoot().getMaxCoordinates()));
//
//        tree.insert(new float[]{6, 7}, 2);
//        tree.insert(new float[]{1, 3}, 3);
//        tree.insert(new float[]{10, 4}, 4);
//        tree.insert(new float[]{8, 2}, 5);
//
//        Assert.assertEquals(tree.getRoot().getChildren().size(), 5);
//
//        float[] expectedMin = new float[]{1, 2};
//        float[] expectedMax = new float[]{10, 7};
//
//        Assert.assertTrue(Arrays.equals(expectedMin, tree.getRoot().getMinCoordinates()));
//        Assert.assertTrue(Arrays.equals(expectedMax, tree.getRoot().getMaxCoordinates()));
//        Assert.assertEquals(tree.getRoot().getArea(), 45.0d);
//    }
//
//    @Test
//    public void testInsertDuplicatesNoSplit() {
//        tree.insert(new float[]{1, 1}, 1);
//        tree.insert(new float[]{1, 1}, 1);
//        tree.insert(new float[]{1, 1}, 1);
//
//        Assert.assertEquals(tree.getRoot().getChildren().size(), 3);
//    }

    @Test
    public void testInsertDuplicatesNoSplitRoaring() {
        R.insert(new float[]{1, 1}, 1);
        R.insert(new float[]{1, 1}, 1);
        R.insert(new float[]{1, 1}, 1);

        Assert.assertEquals(R.root().children.size(), 3);
    }
    @Test
    public void testRemoval() {
        R.insert(new float[]{1, 2}, 1);
        R.insert(new float[]{3, 2}, 2);
        R.insert(new float[]{1, 3}, 3);

        Assert.assertEquals(3, R.root().children.size());

        RTreeUtils.print(R);

        assertTrue( R.remove(new float[]{1, 3}, 3) );

        RTreeUtils.print(R);

        Assert.assertEquals(2, R.root().children.size());
        assertFalse(R.root().contains(new float[]{1, 3}));

        assertEquals(2, R.root().max[1], 0.01f); //y coord = 2, not 3 (which was just removed)

    }

//    @Test
//    public void testSplitOccurs() {
//        Random rand = new Random();
//        for (int i = 0; i < 100; i++) {
//            tree.insert(new float[]{rand.nextFloat(), rand.nextFloat()}, i);
//        }
//
//        Assert.assertTrue(tree.getRoot().getChildren().size() > 1);
//    }

    @Test
    public void testSplitOccursRoaring() {
        Random rand = new Random();
        for (int i = 0; i < 100; i++) {
            R.insert(new float[]{rand.nextFloat(), rand.nextFloat()}, i);
        }

        Assert.assertTrue(R.root().children.size() > 1);
    }


}

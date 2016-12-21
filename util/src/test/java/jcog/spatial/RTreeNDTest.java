package jcog.spatial;

import org.junit.Assert;
import org.junit.Test;

import static jcog.spatial.RTreeTest.createRect2DTree;
import static jcog.spatial.RTreeTest.createRectNDTree;
import static jcog.spatial.RTreeTest.generateRandomRects;

/**
 * Created by me on 12/21/16.
 */
public class RTreeNDTest {

    /**
     * Use an enormous bounding box to ensure that every rectangle is returned.
     * Verifies the count returned from search AND the number of rectangles results.
     */
    @Test
    public void rect2DSearchAllTest() {

        final int entryCount = 1000;
        int dim = 3;

        final RectND[] rects =  generateRandomRects(dim, entryCount);

        for (RTree.Split type : RTree.Split.values()) {
            RTree<RectND> rTree = createRectNDTree(2, 8, type);
            for (int i = 0; i < rects.length; i++) {
                rTree.add(rects[i]);
            }

            final RectND searchRect = new RectND(new PointND(0, 0, 0), new PointND(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));

            RectND[] results = new RectND[entryCount];

            final int foundCount = rTree.search(searchRect, results);
            int resultCount = 0;
            for(int i = 0; i < results.length; i++) {
                if(results[i] != null) {
                    resultCount++;
                }
            }

            final int expectedCount = entryCount;
            Assert.assertEquals("[" + type + "] Search returned incorrect search result count - expected: " + expectedCount + " actual: " + foundCount, expectedCount, foundCount);
            Assert.assertEquals("[" + type + "] Search returned incorrect number of rectangles - expected: " + expectedCount + " actual: " + resultCount, expectedCount, resultCount);
        }
    }

}

package jcog.tree.rtree;

import jcog.tree.rtree.point.Double2D;
import jcog.tree.rtree.point.FloatND;
import jcog.tree.rtree.rect.RectDouble2D;
import jcog.tree.rtree.rect.RectFloatND;
import jcog.tree.rtree.util.CounterNode;
import jcog.tree.rtree.util.Stats;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 12/21/16.
 */
public class RTreeNDTest {

    @Test
    public void pointSearchTest() {

        final RTree<Double2D> pTree = new RTree<>(new Double2D.Builder(), 2, 8, RTreeModel.DefaultSplits.AXIAL);

        for (int i = 0; i < 10; i++) {
            pTree.add(new Double2D(i, i));
        }

        final RectDouble2D rect = new RectDouble2D(new Double2D(2, 2), new Double2D(8, 8));
        final Double2D[] result = new Double2D[10];

        final int n = pTree.containedToArray(rect, result);
        assertEquals(7, n);

        for (int i = 0; i < n; i++) {
            Assert.assertTrue(result[i].coord(0) >= 2);
            Assert.assertTrue(result[i].coord(0) <= 8);
            Assert.assertTrue(result[i].coord(1) >= 2);
            Assert.assertTrue(result[i].coord(1) <= 8);
        }
    }

    /**
     * Use an small bounding box to ensure that only expected rectangles are returned.
     * Verifies the count returned from search AND the number of rectangles results.
     * 2D but using N-d impl
     */
    @Test
    public void rectNDSearchTest2() {

        final int entryCount = 20;

        System.out.println("rectNDSearchTest2");

        for (RTreeModel.DefaultSplits type : RTreeModel.DefaultSplits.values()) {
            RTree<RectFloatND> rTree = RTree2DTest.createRectNDTree(2, 8, type);
            for (int i = 0; i < entryCount; i++) {
                rTree.add(new RectFloatND(new FloatND(i, i), new FloatND(i + 3, i + 3)));
            }

            final RectFloatND searchRect = new RectFloatND(new FloatND(5, 5), new FloatND(10, 10));
            List<RectFloatND> results = new ArrayList();

            rTree.intersecting(searchRect, results::add);
            int resultCount = 0;
            for (int i = 0; i < results.size(); i++) {
                if (results.get(i) != null) {
                    resultCount++;
                }
            }

            final int expectedCount = 9;
            //Assert.assertEquals("[" + type + "] Search returned incorrect search result count - expected: " + expectedCount + " actual: " + foundCount, expectedCount, foundCount);

            assertEquals("[" + type + "] Search returned incorrect number of rectangles - expected: " + expectedCount + " actual: " + resultCount, expectedCount, resultCount);

            // If the order of nodes in the tree changes, this test may fail while returning the correct results.
            Collections.sort(results);
            for (int i = 0; i < resultCount; i++) {
                assertTrue("Unexpected result found", results.get(i).min.coord(0) == i + 2 && results.get(i).min.coord(1) == i + 2 && results.get(i).max.coord(0) == i + 5 && results.get(i).max.coord(1) == i + 5);
            }

            System.out.println("\t" + rTree.stats());
        }
    }

    @Ignore @Test
    public void testSearchAllWithOneDimensionRandomlyInfinite() {
        System.out.println("\n\nINfinites");
        final int entryCount = 400;
        searchAll(2, 4,
                (dim) -> RTree2DTest.generateRandomRectsWithOneDimensionRandomlyInfinite(dim, entryCount));
    }

    /**
     * Use an enormous bounding box to ensure that every rectangle is returned.
     * Verifies the count returned from search AND the number of rectangles results.
     */
    @Test
    public void RectNDSearchAllTest() {
        System.out.println("\n\nfinites");
        final int entryCount = 400;
        searchAll(1, 6, (dim) -> RTree2DTest.generateRandomRects(dim, entryCount));
    }

    static void searchAll(int minDim, int maxDim, IntFunction<RectFloatND[]> generator) {
        for (int dim = minDim; dim <= maxDim; dim++) {

            final RectFloatND[] rects = generator.apply(dim);
            Set<RectFloatND> input = new HashSet();
            Collections.addAll(input, rects);

            System.out.println("\tRectNDSearchAllTest[dim=" + dim + ']');

            for (RTreeModel.DefaultSplits type : RTreeModel.DefaultSplits.values()) {
                RTree<RectFloatND> rTree = RTree2DTest.createRectNDTree(2, 8, type);
                for (int i = 0; i < rects.length; i++) {
                    rTree.add(rects[i]);
                }

                final RectFloatND searchRect = new RectFloatND(
                        FloatND.fill(dim, Float.NEGATIVE_INFINITY),
                        FloatND.fill(dim, Float.POSITIVE_INFINITY)
                );

                RectFloatND[] results = new RectFloatND[rects.length];

                final int foundCount = rTree.containedToArray(searchRect, results);
                int resultCount = 0;
                for (int i = 0; i < results.length; i++) {
                    if (results[i] != null) {
                        resultCount++;
                    }
                }

                final int expectedCount = rects.length;
                //Assert.assertEquals("[" + type + "] Search returned incorrect search result count - expected: " + expectedCount + " actual: " + foundCount, expectedCount, foundCount);
                Assert.assertTrue("[" + type + "] Search returned incorrect search result count - expected: " + expectedCount + " actual: " + foundCount,
                        Math.abs(expectedCount - foundCount) < 10 /* in case of duplicates */);

                Assert.assertTrue("[" + type + "] Search returned incorrect number of rectangles - expected: " + expectedCount + " actual: " + resultCount,
                        Math.abs(expectedCount - resultCount) < 10 /* in case of duplicates */);

                Set<RectFloatND> output = new HashSet();
                Collections.addAll(output, results);


                //assertEquals( " same content", input, output);


                Stats s = rTree.stats();
                s.print(System.out);
                //System.out.println("\t" + rTree.getRoot());
                assertTrue(s.getMaxDepth() <= 8 /* reasonable */);
            }
        }
    }


    /**
     * Use an small bounding box to ensure that only expected rectangles are returned.
     * Verifies the count returned from search AND the number of rectangles results.
     */
    @Test
    public void RectDouble2DSearchTest() {

        final int entryCount = 20;

        for (RTreeModel.DefaultSplits type : RTreeModel.DefaultSplits.values()) {
            RTree<RectDouble2D> rTree = createRectDouble2DTree(2, 8, type);
            for (int i = 0; i < entryCount; i++) {
                rTree.add(new RectDouble2D(i, i, i + 3, i + 3));
            }

            final RectDouble2D searchRect = new RectDouble2D(5, 5, 10, 10);
            RectDouble2D[] results = new RectDouble2D[3];

            final int foundCount = rTree.containedToArray(searchRect, results);
            int resultCount = 0;
            for (int i = 0; i < results.length; i++) {
                if (results[i] != null) {
                    resultCount++;
                }
            }

            final int expectedCount = 3;
            assertEquals("[" + type + "] Search returned incorrect search result count - expected: " + expectedCount + " actual: " + foundCount, expectedCount, foundCount);
            assertEquals("[" + type + "] Search returned incorrect number of rectangles - expected: " + expectedCount + " actual: " + resultCount, expectedCount, resultCount);

            Arrays.sort(results);
            // If the order of nodes in the tree changes, this test may fail while returning the correct results.
            for (int i = 0; i < resultCount; i++) {
                Assert.assertTrue("Unexpected result found:" + results[i],
                RTree.equals(results[i].min().coord(0), i + 5) &&
                        RTree.equals(results[i].min().coord(1), i + 5) &&
                        RTree.equals(results[i].max().coord(0), i + 8) &&
                        RTree.equals(results[i].max().coord(1), i + 8));
            }
        }
    }

    /**
     * Use an small bounding box to ensure that only expected rectangles are returned.
     * Verifies the count returned from search AND the number of rectangles results.
     */
    @Test
    public void RectDouble2DIntersectTest() {

        final int entryCount = 20;

        for (RTreeModel.DefaultSplits type : RTreeModel.DefaultSplits.values()) {
            RTree<RectDouble2D> rTree = createRectDouble2DTree(2, 8, type);
            for (int i = 0; i < entryCount; i++) {
                rTree.add(new RectDouble2D(i, i, i + 3, i + 3));
            }

            final RectDouble2D searchRect = new RectDouble2D(5, 5, 10, 10);

            final int expectedCount = 9;
            List<RectDouble2D> results = new ArrayList(expectedCount);

            rTree.intersecting(searchRect, results::add);
            final int resultCount = results.size();


            assertEquals("[" + type + "] Search returned incorrect search result count - expected: " + expectedCount + " actual: " + resultCount, expectedCount, resultCount);
            assertEquals("[" + type + "] Search returned incorrect number of rectangles - expected: " + expectedCount + " actual: " + resultCount, expectedCount, resultCount);

            Collections.sort(results);

            // If the order of nodes in the tree changes, this test may fail while returning the correct results.
            for (int i = 0; i < resultCount; i++) {
                Assert.assertTrue("Unexpected result found", RTree.equals(results.get(i).min().coord(0), i + 2) &&
                        RTree.equals(results.get(i).min().coord(1), i + 2) &&
                        RTree.equals(results.get(i).max().coord(0), i + 5) &&
                        RTree.equals(results.get(i).max().coord(1), i + 5));
            }
        }
    }


    static RectDouble2D[] generateRandomRects(int count) {
        final Random rand = new Random(13);

        // changing these values changes the rectangle sizes and consequently the distribution density
        final int minX = 500;
        final int minY = 500;
        final int maxXRange = 25;
        final int maxYRange = 25;

        final double hitProb = 1.0 * count * maxXRange * maxYRange / (minX * minY);

        final RectDouble2D[] rects = new RectDouble2D[count];
        for (int i = 0; i < count; i++) {
            final int x1 = rand.nextInt(minX);
            final int y1 = rand.nextInt(minY);
            final int x2 = x1 + rand.nextInt(maxXRange);
            final int y2 = y1 + rand.nextInt(maxYRange);
            rects[i] = new RectDouble2D(x1, y1, x2, y2);
        }

        return rects;
    }

    /**
     * Use an enormous bounding box to ensure that every rectangle is returned.
     * Verifies the count returned from search AND the number of rectangles results.
     */
    @Test
    public void RectDouble2DSearchAllTest() {

        final int entryCount = 1000;
        final RectDouble2D[] rects = generateRandomRects(entryCount);

        for (RTreeModel.DefaultSplits type : RTreeModel.DefaultSplits.values()) {
            RTree<RectDouble2D> rTree = createRectDouble2DTree(2, 8, type);
            for (int i = 0; i < rects.length; i++) {
                rTree.add(rects[i]);
            }

            final RectDouble2D searchRect = new RectDouble2D(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
            RectDouble2D[] results = new RectDouble2D[entryCount];

            final int foundCount = rTree.containedToArray(searchRect, results);
            int resultCount = 0;
            for (int i = 0; i < results.length; i++) {
                if (results[i] != null) {
                    resultCount++;
                }
            }

            final AtomicInteger visitCount = new AtomicInteger();
            rTree.containing(searchRect, (n) -> {
                visitCount.incrementAndGet();
                return true;
            });
            assertEquals(entryCount, visitCount.get());

            final int expectedCount = entryCount;
            assertEquals("[" + type + "] Search returned incorrect search result count - expected: " + expectedCount + " actual: " + foundCount, expectedCount, foundCount);
            assertEquals("[" + type + "] Search returned incorrect number of rectangles - expected: " + expectedCount + " actual: " + resultCount, expectedCount, resultCount);
        }
    }

    /**
     * Collect stats making the structure of trees of each split type
     * more visible.
     */
    @Ignore
    // This test ignored because output needs to be manually evaluated.
    public void treeStructureStatsTest() {

        final int entryCount = 50_000;

        final RectDouble2D[] rects = generateRandomRects(entryCount);
        for (RTreeModel.DefaultSplits type : RTreeModel.DefaultSplits.values()) {
            RTree<RectDouble2D> rTree = createRectDouble2DTree(2, 8, type);
            for (int i = 0; i < rects.length; i++) {
                rTree.add(rects[i]);
            }

            Stats stats = rTree.stats();
            stats.print(System.out);
        }
    }

    /**
     * Do a search and collect stats on how many nodes we hit and how many
     * bounding boxes we had to evaluate to get all the results.
     * <p>
     * Preliminary findings:
     * - Evals for QUADRATIC tree increases with size of the search bounding box.
     * - QUADRATIC seems to be ideal for small search bounding boxes.
     */
    @Ignore
    // This test ignored because output needs to be manually evaluated.
    public void treeSearchStatsTest() {

        final int entryCount = 5000;

        final RectDouble2D[] rects = generateRandomRects(entryCount);

        for (int j = 0; j < 6; j++) {
            for (RTreeModel.DefaultSplits type : RTreeModel.DefaultSplits.values()) {
                RTree<RectDouble2D> rTree = createRectDouble2DTree(2, 12, type);
                for (int i = 0; i < rects.length; i++) {
                    rTree.add(rects[i]);
                }

                rTree.instrumentTree();

                final RectDouble2D searchRect = new RectDouble2D(100, 100, 120, 120);
                RectDouble2D[] results = new RectDouble2D[entryCount];
                final long start = System.nanoTime();
                int foundCount = rTree.containedToArray(searchRect, results);
                final long end = System.nanoTime() - start;
                CounterNode<RectDouble2D> root = (CounterNode<RectDouble2D>) rTree.root();

                //System.out.println("[" + type + "] searched " + root.containingCount + " nodes, returning " + foundCount + " entries");
                System.out.println("[" + type + "] evaluated " + CounterNode.bboxEvalCount + " b-boxes, returning " + foundCount + " entries");

                System.out.println("Run was " + end / 1000 + " us");
            }
        }
    }

    @Test
    public void treeContainsTest() {
        final RTree<RectDouble2D> rTree = createRectDouble2DTree(RTreeModel.DefaultSplits.QUADRATIC);

        final RectDouble2D[] rects = new RectDouble2D[5];
        for (int i = 0; i < rects.length; i++) {
            rects[i] = new RectDouble2D(i, i, i + 1, i + 1);
            rTree.add(rects[i]);
        }

        assertEquals(rTree.size(), rects.length);

        for (int i = 0; i < rects.length; i++) {
            Assert.assertFalse(rTree.containedAsSet(rects[i]).isEmpty());
        }
    }



    @Test
    public void treeRemovalTest5Entries() {
        final RTree<RectDouble2D> rTree = createRectDouble2DTree(RTreeModel.DefaultSplits.QUADRATIC);

        final RectDouble2D[] rects = new RectDouble2D[5];
        for (int i = 0; i < rects.length; i++) {
            rects[i] = new RectDouble2D(i, i, i + 1, i + 1);
            rTree.add(rects[i]);
        }

        for (int i = 1; i < rects.length; i++) {
            assertTrue(rTree.remove(rects[i]));
            assertEquals(rects.length - i, rTree.size());
        }

        assertEquals(1, rTree.size());

        Assert.assertFalse("Missing hyperRect that should  be found " + rects[0], rTree.containedAsSet(rects[0]).isEmpty());

        for (int i = 1; i < rects.length; i++) {
            Assert.assertTrue("Found hyperRect that should have been removed on search " + rects[i], rTree.containedAsSet(rects[i]).isEmpty());
        }

        final RectDouble2D hr = new RectDouble2D(0, 0, 5, 5);
        rTree.add(hr);
        Assert.assertFalse(rTree.containedAsSet(hr).isEmpty());
        Assert.assertTrue("Found hyperRect that should have been removed on search", rTree.size() != 0);
    }

    @Test
    public void treesize() {

        final int NENTRY = 500;

        final RTree<RectDouble2D> rTree = createRectDouble2DTree(RTreeModel.DefaultSplits.QUADRATIC);

        for (int i = 0; i < NENTRY; i++) {
            final RectDouble2D rect = new RectDouble2D(i, i, i + 1, i + 1);
            rTree.add(rect);
        }

        assertEquals(NENTRY, rTree.size());
    }


    @Test
    public void treeRemovalTestDuplicates() {

        final int NENTRY = 50;

        final RTree<RectDouble2D> rTree = createRectDouble2DTree(RTreeModel.DefaultSplits.QUADRATIC);

        final RectDouble2D[] rect = new RectDouble2D[2];
        for (int i = 0; i < rect.length; i++) {
            rect[i] = new RectDouble2D(i, i, i + 1, i + 1);
            rTree.add(rect[i]);
        }
        assertEquals(2, rTree.size());

        for (int i = 0; i < NENTRY; i++) {
            rTree.add(rect[1]);
        }

        assertEquals(2, rTree.size());

        for (int i = 0; i < rect.length; i++) {
            rTree.remove(rect[i]);
        }
        assertEquals(0, rTree.size());

        for (int i = 0; i < rect.length; i++) {
            Assert.assertTrue("Found hyperRect that should have been removed " + rect[i], rTree.containedAsSet(rect[i]).isEmpty());
        }
    }

    @Test
    public void treeRemovalTest1000Entries() {
        final RTree<RectDouble2D> rTree = createRectDouble2DTree(RTreeModel.DefaultSplits.QUADRATIC);

        int N = 1000;
        final RectDouble2D[] rect = new RectDouble2D[N];
        for (int i = 0; i < rect.length; i++) {
            rect[i] = new RectDouble2D(i, i, i + 1, i + 1);
            rTree.add(rect[i]);
        }

        assertEquals(N, rTree.size());

        for (int i = 0; i < N; i++) {
            boolean removed = rTree.remove(rect[i]);
            assertTrue(removed);
        }

        assertEquals(0, rTree.size());

        for (int i = 0; i < N; i++) {
            Assert.assertTrue("#" + i + " of " + rect.length + ": Found hyperRect that should have been removed" + rect[i], rTree.containedAsSet(rect[i]).isEmpty());
        }

        Assert.assertFalse("Found hyperRect that should have been removed on search ", rTree.size() > 0);
    }

    @Test
    public void treeSingleRemovalTest() {
        final RTree<RectDouble2D> rTree = createRectDouble2DTree(RTreeModel.DefaultSplits.QUADRATIC);

        RectDouble2D rect = new RectDouble2D(0, 0, 2, 2);
        rTree.add(rect);
        Assert.assertTrue("Did not add HyperRect to Tree", rTree.size() > 0);
        rTree.remove(rect);
        Assert.assertTrue("Did not remove HyperRect from Tree", rTree.size() == 0);
        rTree.add(rect);
        Assert.assertTrue("Tree nulled out and could not add HyperRect back in", rTree.size() > 0);
    }

    @Ignore
    // This test ignored because output needs to be manually evaluated.
    public void treeRemoveAndRebalanceTest() {
        final RTree<RectDouble2D> rTree = createRectDouble2DTree(RTreeModel.DefaultSplits.QUADRATIC);

        RectDouble2D[] rect = new RectDouble2D[65];
        for (int i = 0; i < rect.length; i++) {
            if (i < 4) {
                rect[i] = new RectDouble2D(0, 0, 1, 1);
            } else if (i < 8) {
                rect[i] = new RectDouble2D(2, 2, 4, 4);
            } else if (i < 12) {
                rect[i] = new RectDouble2D(4, 4, 5, 5);
            } else if (i < 16) {
                rect[i] = new RectDouble2D(5, 5, 6, 6);
            } else if (i < 20) {
                rect[i] = new RectDouble2D(6, 6, 7, 7);
            } else if (i < 24) {
                rect[i] = new RectDouble2D(7, 7, 8, 8);
            } else if (i < 28) {
                rect[i] = new RectDouble2D(8, 8, 9, 9);
            } else if (i < 32) {
                rect[i] = new RectDouble2D(9, 9, 10, 10);
            } else if (i < 36) {
                rect[i] = new RectDouble2D(2, 2, 4, 4);
            } else if (i < 40) {
                rect[i] = new RectDouble2D(4, 4, 5, 5);
            } else if (i < 44) {
                rect[i] = new RectDouble2D(5, 5, 6, 6);
            } else if (i < 48) {
                rect[i] = new RectDouble2D(6, 6, 7, 7);
            } else if (i < 52) {
                rect[i] = new RectDouble2D(7, 7, 8, 8);
            } else if (i < 56) {
                rect[i] = new RectDouble2D(8, 8, 9, 9);
            } else if (i < 60) {
                rect[i] = new RectDouble2D(9, 9, 10, 10);
            } else if (i < 65) {
                rect[i] = new RectDouble2D(1, 1, 2, 2);
            }
        }
        for (int i = 0; i < rect.length; i++) {
            rTree.add(rect[i]);
        }
        Stats stat = rTree.stats();
        stat.print(System.out);
        for (int i = 0; i < 5; i++) {
            rTree.remove(rect[64]);
        }
        Stats stat2 = rTree.stats();
        stat2.print(System.out);
    }

    @Test
    public void treeUpdateTest() {
        final RTree<RectDouble2D> rTree = createRectDouble2DTree(RTreeModel.DefaultSplits.QUADRATIC);

        RectDouble2D rect = new RectDouble2D(0, 1, 2, 3);
        rTree.add(rect);
        RectDouble2D oldRect = new RectDouble2D(0, 1, 2, 3);
        RectDouble2D newRect = new RectDouble2D(1, 2, 3, 4);
        rTree.replace(oldRect, newRect);
        RectDouble2D[] results = new RectDouble2D[2];
        final int num = rTree.containedToArray(newRect, results);
        Assert.assertTrue("Did not find the updated HyperRect", num == 1);
        System.out.print(results[0]);
    }

    static RTree<RectDouble2D> createRectDouble2DTree(RTreeModel.DefaultSplits splitType) {
        return createRectDouble2DTree(2, 8, splitType);
    }

    static RTree<RectDouble2D> createRectDouble2DTree(int minM, int maxM, RTreeModel.DefaultSplits splitType) {
        return new RTree<>(new RectDouble2D.Builder(), minM, maxM, splitType);
    }

    @Test
    @Ignore public void testAddsubtreeWithSideTree() {
        final RTree<RectDouble2D> rTree = createRectDouble2DTree(3, 6, RTreeModel.DefaultSplits.QUADRATIC);

        final RectDouble2D search;

        rTree.add(new RectDouble2D(2, 2, 4, 4));
        rTree.add(search = new RectDouble2D(5, 2, 6, 3));

        // now make sure root node is a branch
        for (int i = 0; i < 5; i++) {
            rTree.add(new RectDouble2D(3.0 - 1.0 / (10.0 + i), 3.0 - 1.0 / (10.0 + i), 3.0 + 1.0 / (10.0 + i), 3.0 + 1.0 / (10.0 + i)));
        }

        // add subtree/child on first rectangle - fully contained
        rTree.add(new RectDouble2D(2.5, 2.5, 3.5, 3.5));

        assertEquals(8, rTree.size());

        final AtomicInteger hitCount = new AtomicInteger();
        // but 5, 2, 6, 3 must still be found!
        rTree.containing(search, (closure) -> {
            hitCount.incrementAndGet();
            return true;
        });

        assertEquals(1, hitCount.get());

    }

}

package jcog.bag;

import jcog.Util;
import jcog.bag.BagTest;
import jcog.bag.impl.CurveBag;
import jcog.bag.impl.PLinkArrayBag;
import jcog.pri.PLink;
import jcog.pri.Pri;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import jcog.random.XorShift128PlusRandom;
import jcog.tensor.Tensor;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.function.DoubleSupplier;

import static jcog.bag.BagTest.fillLinear;
import static jcog.bag.BagTest.samplingPriDist;
import static jcog.bag.BagTest.testBasicInsertionRemoval;
import static jcog.pri.op.PriMerge.plus;
import static org.junit.Assert.*;

public class ArrayBagTest {

    @NotNull
    CurveBag<PLink<String>> curveBag(int n, PriMerge mergeFunction) {
        return new CurveBag(mergeFunction, new HashMap<>(n), new XorShift128PlusRandom(1), n);
    }

    @Test
    public void testBasicInsertionRemovalArray() {
        testBasicInsertionRemoval(new PLinkArrayBag<>(1, plus, new HashMap<>(1)));
    }


    @Test
    public void testBudgetMerge() {
        PLinkArrayBag<String> a = new PLinkArrayBag<String>(4, plus, new HashMap<>(4));
        assertEquals(0, a.size());

        a.put(new PLink("x", 0.1f));
        a.put(new PLink("x", 0.1f));
        a.commit(null);
        assertEquals(1, a.size());


        PriReference<String> agx = a.get("x");
        Pri expect = new Pri(0.2f);
        assertTrue(agx + "==?==" + expect, Util.equals(expect.priElseNeg1(), agx.priElseNeg1(), 0.01f));

    }
    @NotNull
    private CurveBag<PLink<String>> populated(int n, @NotNull DoubleSupplier random) {


        CurveBag<PLink<String>> a = curveBag(n, plus);


        //fill with uniform randomness
        for (int i = 0; i < n; i++) {
            a.put(new PLink("x" + i, (float) random.getAsDouble()));
        }

        a.commit();
        //a.printAll();

        return a;

    }

    @Test
    public void testSort() {
        PLinkArrayBag a = new PLinkArrayBag(4, plus, new HashMap<>(4));

        a.put(new PLink("x", 0.1f));
        a.put(new PLink("y", 0.2f));

        a.commit(null);

        Iterator<PriReference<String>> ii = a.iterator();
        assertEquals("y", ii.next().get());
        assertEquals("x", ii.next().get());

        assertEquals("[$0.2000 y, $0.1000 x]", a.listCopy().toString());

        System.out.println(a.listCopy());

        a.put(new PLink("x", 0.2f));

        System.out.println(a.listCopy());

        a.commit();

        //x should now be ahead
        assertTrue(a.listCopy().toString().contains("x,")); //x first
        assertTrue(a.listCopy().toString().contains("y]")); //y second

        ii = a.iterator();
        assertEquals("x", ii.next().get());
        assertEquals("y", ii.next().get());

    }

    @Test
    public void testCapacity() {
        PLinkArrayBag a = new PLinkArrayBag(2, plus, new HashMap<>(2));

        a.put(new PLink("x", 0.1f));
        a.put(new PLink("y", 0.2f));
        a.commit(null);
        a.print();
        System.out.println();
        assertEquals(2, a.size());

        assertEquals(0.1f, a.priMin(), 0.01f);

        a.put(new PLink("z", 0.05f));
        a.commit();
        a.print();
        System.out.println();
        assertEquals(2, a.size());
        assertTrue(a.contains("x") && a.contains("y"));
        assertFalse(a.contains("z"));

    }

    @Test
    public void testRemoveByKey() {
        BagTest.testRemoveByKey(new PLinkArrayBag(2, plus, new HashMap<>(2)));
    }

    @Test public void testInsertOrBoostDoesntCauseSort() {
        final int[] sorts = {0};
        @NotNull CurveBag<PLink<String>> x = new CurveBag(PriMerge.plus, new HashMap<>(), new XorShift128PlusRandom(1), 4) {
            @Override
            protected void sort() {
                sorts[0]++;
                super.sort();
            }
        };

        x.put(new PLink("x", 0.2f));
        x.put(new PLink("y", 0.1f));
        x.put(new PLink("z", 0f));

        assertEquals(0, sorts[0]);

        x.commit();

        assertEquals(0, sorts[0]);

    }

    @Test
    public void testCurveBagDistribution() {

        int cap = 64;

        CurveBag<PLink<String>> bag = curveBag(cap, PriMerge.plus);

        fillLinear(bag);

        //bag.forEach(System.out::println);

        Tensor f3 = samplingPriDist(bag, cap / 2, 4, 5);

        Tensor f2 = samplingPriDist(bag, cap * 2, 4, 5);


        int batches = cap * 100;
        int batchSize = 4;
        Tensor f1 = samplingPriDist(bag, batches, batchSize, 10);
        String h = "cap=" + cap + " samples=" + (batches * batchSize);
        System.out.println(h + ":\n\t" + f1.tsv2());
        System.out.println();

        float[] ff = f1.get();

        //monotonically increasing (test only the upper half because it flattens out at the bottom)
        int half = ff.length / 2;
        for (int j = half + 1; j < ff.length; j++) {
            for (int i = half; i < j - 1; i++) {
                assertTrue(ff[j] > ff[i]);
            }
        }


        //TODO verify the histogram resulting from the above execution is relatively flat:
        //ex: [0.21649484536082475, 0.2268041237113402, 0.28865979381443296, 0.26804123711340205]
        //the tests below assume that it begins with a relatively flat distribution
//        System.out.println(Arrays.toString(bag.priHistogram(4)));
//        System.out.println(Arrays.toString(bag.priHistogram(8)));


//        System.out.print("Sampling: " );
//        printDist(samplingPriDistribution((CurveBag) n.core.concepts, 1000));
//        System.out.print("Priority: " );
//        EmpiricalDistribution pri;
//        printDist(pri = getSamplingPriorityDistribution(n.core.concepts, 1000));
//
//        List<SummaryStatistics> l = pri.getBinStats();
//        assertTrue(l.get(0).getN() < l.get(l.size() - 1).getN());

    }


}

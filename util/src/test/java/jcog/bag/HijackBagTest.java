package jcog.bag;

import com.google.common.base.Joiner;
import jcog.bag.impl.hijack.DefaultHijackBag;
import jcog.pri.PLink;
import jcog.pri.PriMerge;
import jcog.pri.RawPLink;
import org.junit.Test;

import java.util.Random;
import java.util.TreeSet;

import static jcog.bag.BagTest.*;
import static jcog.pri.PriMerge.max;
import static jcog.pri.PriMerge.plus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 2/9/17.
 */
public class HijackBagTest {

    @Test public void testSamplingFlatHijack() {
        for (int reprobes : new int[] { 1, 2, 4, 8 }) {
            for (int capacity : new int[] { 1, 2, 4, 8, 16, 32, 64, 128 }) {
                testPutMinMaxAndUniqueness(
                        new DefaultHijackBag<>(max, capacity, reprobes));
            }
        }
    }

    @Test public void testRemoveByKey() {
        BagTest.testRemoveByKey(new DefaultHijackBag(plus, 2, 3));
    }



//    @Test
//    public void testScalePutArray() {
//        testScalePutHalfs(0.5f, new ArrayBag<>(2, max, new HashMap<>(2)), 1f, 0.5f);
//        testScalePutHalfs(0.75f, new ArrayBag<>(2, plus, new HashMap<>(2)), 1f, 0.5f);
//        testScalePut2(new ArrayBag(2, plus, new HashMap<>(2)));
//
//    }
//    @Test
//    public void testScalePutHijaMax() {
//        //second scale has no effect being smaller than the first one
//        BagTest.testScalePutHalfs(0.5f, new DefaultHijackBag(max, 2, 1), 1f, 0.5f);
//        BagTest.testScalePutHalfs(0.5f, new DefaultHijackBag(max, 2, 2), 1f, 0.5f);
//
//        BagTest.testScalePutHalfs(0.75f, new DefaultHijackBag(plus, 2, 2), 1f, 0.5f);
//    }
//    @Test
//    public void testScalePutHija3() {
//        BagTest.testScalePut2(new DefaultHijackBag(plus, 2, 1));
//    }
//    @Test
//    public void testScalePutHija4() {
//        BagTest.testScalePut2(new DefaultHijackBag(plus, 2, 2));
//    }

    @Test
    public void testBasicInsertionRemovalHijack() {
        testBasicInsertionRemoval(new DefaultHijackBag(max, 1, 1));
    }

    @Test public void testHijackFlatBagRemainsRandomInNormalizedSampler() {

        int n = 256;

        Bag<String,PLink<String>> a = new DefaultHijackBag<String>(max, n, 4);
        for (int i = 0; i < n*8; i++) {
            a.put(new RawPLink("x" + Integer.toString(Float.floatToIntBits(1f/i),5), ((float)(i))/(n)));
        }

        a.commit();
        int size = a.size();
        //assertTrue(size >= 20 && size <= 30);

//        TreeSet<String> keys = new TreeSet();
//        Iterators.transform(a.iterator(), x -> x.get()).forEachRemaining(keys::add);
//        System.out.println( keys.size() + " " + Joiner.on(' ').join(keys) );

        TreeSet<String> keys2 = new TreeSet();
        a.forEach((b)->{
            if (!keys2.add(b.get()))
                throw new RuntimeException("duplicate detected");
        });
        System.out.println( keys2.size() + " " + Joiner.on(' ').join(keys2) );

        assertEquals(size, keys2.size());

//        int b = 20;
//        EmpiricalDistribution e = BagTest.getSamplingPriorityDistribution(a, n * 500, b);
//
//        printDist(e);
//
//        //monotonically increasing:
//        assertTrue(e.getBinStats().get(0).getMean() < e.getBinStats().get(b-1).getMean());
        //assertTrue(e.getBinStats().get(0).getMean() < e.getBinStats().get(b/2).getMean());
        //assertTrue(e.getBinStats().get(b/2).getMean() < e.getBinStats().get(b-2).getMean());

        //a.print();
    }


    @Test
    public void testHijackResize() {
        Random rng = rng();
        DefaultHijackBag b = new DefaultHijackBag(PriMerge.max, 0, 7);
        BagTest.populate(b, rng, 10, 20, 0f, 1f, 0.5f);
        //        assertEquals(b.reprobes /*0*/, b.size());


        int dimensionality = 50;
        b.setCapacity(dimensionality * 2);

        BagTest.populate(b, rng, dimensionality*5, dimensionality, 0f, 1f, 0.5f);
        System.out.println("under capacity");
        b.print();
        assertApproximatelySized(b, dimensionality, 0.5f);

        b.setCapacity(dimensionality/2*2);

        System.out.println("half capacity");
        b.print();

        assertApproximatelySized(b, dimensionality/2*2, 0.5f);

        BagTest.populate(b, rng, dimensionality*3, dimensionality, 0f, 1f, 0.5f);
        System.out.println("under capacity, refilled");
        b.print();

        //test


        b.setCapacity(dimensionality*2);

        BagTest.populate(b, rng, dimensionality*3, dimensionality, 0f, 1f, 0.5f);
        System.out.println("under capacity, expanded");
        b.print();

        assertApproximatelySized(b, dimensionality, 0.25f);
        //test


    }

    public void assertApproximatelySized(Bag<String,?> b, int expected, float closeness) {
        int bSize = b.size();
        float error = Math.abs(expected - bSize) / (Math.max(bSize, (float) expected));
        System.out.println(bSize + "  === " + expected + ", diff=" + error);
        assertTrue(error < closeness);
    }
}
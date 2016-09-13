package nars.bag;

import com.google.common.collect.Iterators;
import nars.Param;
import nars.bag.impl.ArrayBag;
import nars.bag.impl.CurveBag;
import nars.bag.impl.experimental.HijackBag;
import nars.budget.Budget;
import nars.budget.RawBudget;
import nars.budget.merge.BudgetMerge;
import nars.link.BLink;
import nars.util.data.random.XorShift128PlusRandom;
import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleSupplier;

import static nars.budget.merge.BudgetMerge.plusBlend;
import static nars.budget.merge.BudgetMerge.plusDQDominant;
import static nars.util.Texts.n4;
import static org.junit.Assert.*;

/**
 * @author me
 */
public class BagTest {

    static final Random rng = new XorShift128PlusRandom(1);

    static {
        Param.DEBUG = true;
    }

    @Test
    public void testBasicInsertionRemovalArray() {
        testBasicInsertionRemoval(new ArrayBag<>(1, plusDQDominant, new HashMap<>(1)));
    }

    @Test
    public void testBasicInsertionRemovalCurve() {
        testBasicInsertionRemoval(new CurveBag<>(1, defaultSampler, plusDQDominant, new HashMap(1)));
    }

    @Test
    public void testBasicInsertionRemovalHijack() {
        testBasicInsertionRemoval(new HijackBag(1, 1, new XorShift128PlusRandom(1)));
    }

    public void testBasicInsertionRemoval(Bag<String> c) {


        assertEquals(1, c.capacity());
        if (!(c instanceof HijackBag)) {
            assertEquals(0, c.size());
            assertTrue(c.isEmpty());
        }

        //insert an item with zero budget
        c.put("x");
        c.commit();

        assertEquals(c.size(), 1);


        assertEquals(0, c.priMin(), 0.001f);

        assertTrue(Budget.Zero.equalsBudget(c.get("x"), 0.01f));

    }

    @Test
    public void testBudgetMerge() {
        ArrayBag<String> a = new ArrayBag(4, plusDQDominant, new HashMap<>(4));

        a.put("x", new RawBudget(0.1f, 0.5f, 0.5f));
        a.put("x", new RawBudget(0.1f, 0.5f, 0.5f));
        a.commit();
        assertEquals(1, a.size());


        BLink<String> agx = a.get("x");
        assertTrue(new RawBudget(0.2f, 0.5f, 0.5f).equalsBudget(
                agx, 0.01f));

    }

    @Test
    public void testSort() {
        ArrayBag<String> a = new ArrayBag(4, plusDQDominant, new HashMap<>(4));

        a.put("x", new RawBudget(0.1f, 0.5f, 0.5f));
        a.put("y", new RawBudget(0.2f, 0.5f, 0.5f));

        a.commit();

        Iterator<BLink<String>> ii = a.iterator();
        assertEquals("y", ii.next().get());
        assertEquals("x", ii.next().get());


        assertEquals("[y=$0.2000;0.5000;0.5000$, x=$0.1000;0.5000;0.5000$]", a.listCopy().toString());

        a.put("x", new RawBudget(0.2f, 0.5f, 0.5f));
        a.commit();

        //x should now be ahead
        assertEquals("[x=$0.3000;0.5000;0.5000$, y=$0.2000;0.5000;0.5000$]", a.listCopy().toString());

        ii = a.iterator();
        assertEquals("x", ii.next().get());
        assertEquals("y", ii.next().get());

    }

    @Test
    public void testCapacity() {
        ArrayBag<String> a = new ArrayBag(2, plusDQDominant, new HashMap<>(2));

        a.put("x", new RawBudget(0.1f, 0.5f, 0.5f));
        a.put("y", new RawBudget(0.2f, 0.5f, 0.5f));
        a.put("z", new RawBudget(0.05f, 0.5f, 0.5f));

        a.commit();

        //System.out.println(a.items);
        //System.out.println(ArrayTable.this);

        assertEquals(2, a.size());
        assertTrue(a.contains("x") && a.contains("y"));
        assertFalse(a.contains("z"));

    }

    @Test
    public void testRemoveByKey() {
        ArrayBag<String> a = new ArrayBag(2, plusDQDominant, new HashMap<>(2));

        a.put("x", new RawBudget(0.1f, 0.5f, 0.5f));
        a.commit();
        assertEquals(1, a.size());

        a.remove("x");
        assertEquals(0, a.size());
        assertTrue(a.isEmpty());
        assertTrue(a.listCopy().isEmpty());
        assertTrue(a.keySet().isEmpty());

    }

    @Test
    public void testScalePutArray() {
        testScalePut(new ArrayBag<>(2, plusBlend, new HashMap<>(2)));
        testScalePut2(new ArrayBag(2, plusBlend, new HashMap<>(2)));

    }

    @Test
    public void testScalePutHija() {
        testScalePut(new HijackBag<>(2, 1, new XorShift128PlusRandom(1)));
            testScalePut(new HijackBag<>(2, 2, new XorShift128PlusRandom(1)));
        testScalePut2(new HijackBag<>(2, 1, new XorShift128PlusRandom(1)));
            testScalePut2(new HijackBag<>(2, 2, new XorShift128PlusRandom(1)));
    }

    void testScalePut(Bag<String> a) {
        a.put("x", new RawBudget(0.1f, 0.5f, 0.5f));
        a.put("x", new RawBudget(0.1f, 0.5f, 0.5f), 0.5f, null);
        a.commit();

        assertEquals(0.15, a.get("x").pri(), 0.001f);
    }

    void testScalePut2(Bag<String> a) {

        a.put("y", new RawBudget(0.1f, 0.5f, 0.5f));
        a.put("y", new RawBudget(0.1f, 0.5f, 0.5f), 0.5f, null);
        a.put("y", new RawBudget(0.1f, 0.5f, 0.5f), 0.25f, null);
        a.commit();

        assertEquals(0.175, a.get("y").pri(), 0.001f);

    }

    static void printDist(@NotNull EmpiricalDistribution f) {
        System.out.println(f.getSampleStats().toString().replace("\n", " "));
        f.getBinStats().forEach(
                s -> {
                    /*if (s.getN() > 0)*/
                    System.out.println(
                            n4(s.getMin()) + ".." + n4(s.getMax()) + ":\t" + s.getN());
                }
        );
    }

    @NotNull
    private EmpiricalDistribution getSamplingDistribution(@NotNull CurveBag b, int n) {
        return getSamplingIndexDistribution(b, n, 10);
    }

    @NotNull
    private EmpiricalDistribution getSamplingIndexDistribution(@NotNull CurveBag b, int n, int bins) {
        DoubleArrayList f = new DoubleArrayList(n);
        for (int i = 0; i < n; i++)
            f.add(b.sampleIndex());
        EmpiricalDistribution e = new EmpiricalDistribution(bins);
        e.load(f.toArray());
        return e;
    }


    @NotNull
    private EmpiricalDistribution getSamplingPriorityDistribution(@NotNull Bag b, int n) {
        return getSamplingPriorityDistribution(b, n, 10);
    }

    @NotNull
    private EmpiricalDistribution getSamplingPriorityDistribution(@NotNull Bag b, int n, int bins) {
        DoubleArrayList f = new DoubleArrayList(n);
        if (!b.isEmpty()) {
            for (int i = 0; i < n; i++) {
                @Nullable BLink sample = b.sample();
                if (sample!=null) {
                    float p = sample.pri();
                    f.add(p);
                } else {
                    f.add(-1); //miss
                }
            }
        }
        EmpiricalDistribution e = new EmpiricalDistribution(bins);
        e.load(f.toArray());
        return e;
    }


    @Test
    public void testNormalization() {
        int n = 64;
        int bins = 5;
        int samples = n * 32;

        CurveBag fullDynamicRange = populated(n, Math::random);
        EmpiricalDistribution unifDistr = getSamplingPriorityDistribution(fullDynamicRange, samples, bins);
        printDist(unifDistr);

        float ratioUniform = maxMinRatio(unifDistr);

        //smaller dynamic range should be lesser probabailty difference from low to high
        CurveBag smallDynamicRange = populated(n, () -> 0.1f * Math.random());
        EmpiricalDistribution flatDistr = getSamplingPriorityDistribution(smallDynamicRange, samples, bins);
        printDist(flatDistr);

        float ratioFlat = maxMinRatio(flatDistr);

        System.out.println(ratioUniform + " " + ratioFlat);

        assertTrue(ratioUniform > 7); //should be ideally ~10
        assertTrue(ratioFlat < 6.5); //should be ideally ~1

    }

    private float maxMinRatio(@NotNull EmpiricalDistribution d) {
        List<SummaryStatistics> bins = d.getBinStats();
        return ((float) bins.get(bins.size() - 1).getN() / (bins.get(0).getN()));
    }

    @NotNull
    private CurveBag<String> populated(int n, @NotNull DoubleSupplier random) {


        CurveBag<String> a = curveBag(n, plusDQDominant);


        //fill with uniform randomness
        for (int i = 0; i < n; i++) {
            a.put("x" + i, new RawBudget((float) random.getAsDouble(), 0.5f, 0.5f));
        }

        a.commit();
        //a.printAll();

        return a;

    }

    @Test
    public void testFlatBagRemainsRandomInNormalizedSamplerCurve() {
        @NotNull CurveBag<String> a = curveBag(8, plusDQDominant);

        testSamplingFlat(a, 0.04f);


        int n = a.capacity();
        int rrr = 100;
        EmpiricalDistribution d = getSamplingIndexDistribution((CurveBag) a, n * rrr, n - 1);
        //printDist(d);
        for (int i = 0; i < n - 1; i++) {
            long bi = d.getBinStats().get(i).getN();
            assertTrue("bin " + i + " sampled x " + bi, bi > (rrr / 2)); //received enough samples
        }

    }

    @Test
    public void testFlatBagRemainsRandomInNormalizedSamplerHija() {

        int n = 32;

        testSamplingFlat(new HijackBag<>(n, 4, new XorShift128PlusRandom(1)), 0.038f);

        HijackBag<String> a = new HijackBag<>((int)(n*2f), 2, new XorShift128PlusRandom(2));
        for (int i = 0; i < n; i++) {
            a.put("x" + Integer.toString(Float.floatToIntBits(1f/i),5), new RawBudget(((float)(i+1))/(n), 0.5f, 0.5f));
        }
        int expectedSize = n - 1; /* not all fit */

        assertEquals(expectedSize, a.map.values().size());
        assertEquals(expectedSize, Iterators.toArray(a.iterator(), Object.class).length);

        int b = 20;
        EmpiricalDistribution e = getSamplingPriorityDistribution(a, n * 5000, b);

        printDist(e);

        //monotonically increasing:
        assertTrue(e.getBinStats().get(0).getMean() < e.getBinStats().get(b/2).getMean());
        assertTrue(e.getBinStats().get(b/2).getMean() < e.getBinStats().get(b-2).getMean());

        //a.print();
    }

    void testSamplingFlat(Bag<String> a, float level) {
        int n = a.capacity();


        for (int i = 0; i < n; i++) {
            a.put("x" + i, new RawBudget(level, 0.5f, 0.5f));
        }

        a.commit(); //commit necessary to set sampler's dynamic range

        assertEquals(level, a.priMin(), 0.01f);
        assertEquals(a.priMin(), a.priMax(), 0.01f);

    }

    static final CurveBag.CurveSampler defaultSampler = new CurveBag.NormalizedSampler(CurveBag.power6BagCurve, rng);

    @NotNull
    public CurveBag<String> curveBag(int n, BudgetMerge mergeFunction) {
        return new CurveBag(n, defaultSampler, mergeFunction, new HashMap());
    }

    //AutoBag does not apply to this test
//    @Test public void testDistribution() {
//        Default n = new Default(48, 4, 2, 4);
//        n.perfection.setValue(1f);
//        n.input("$1$ a:b.");
//        n.input("$1$ b:c.");
//        n.input("$1$ c:d.");
//        n.run(4);
//        Bag<Concept> bag = n.core.concepts;
//
//        bag.forEach(System.out::println);
//        System.out.println(bag.size() + " " + bag.priMax() + ' ' + bag.priMin());
//
//        //TODO verify the histogram resulting from the above execution is relatively flat:
//        //ex: [0.21649484536082475, 0.2268041237113402, 0.28865979381443296, 0.26804123711340205]
//        //the tests below assume that it begins with a relatively flat distribution
//        System.out.println(Arrays.toString(bag.priHistogram(4)));
//        System.out.println(Arrays.toString(bag.priHistogram(8)));
//
//
//
//        System.out.print("Sampling: " );
//        printDist(getSamplingDistribution((CurveBag) n.core.concepts, 1000));
//        System.out.print("Priority: " );
//        EmpiricalDistribution pri;
//        printDist(pri = getSamplingPriorityDistribution(n.core.concepts, 1000));
//
//        List<SummaryStatistics> l = pri.getBinStats();
//        assertTrue(l.get(0).getN() < l.get(l.size() - 1).getN());
//
//    }


//    /** maybe should be in ArrayBagTest */
//    @Test public void inQueueTest() {
//
//        CurveBag<String> s = newBag(2);
//
//        //1. fill bag
//        s.put("a0", new RawBudget(0.25f, 0, 0));
//        s.put("a1", new RawBudget(0.25f, 0, 0));
//
//        //2. attempt to insert new under-budgeted item while bag is full
//        s.put("b", new RawBudget(0.2f, 0, 0));
//        assertEquals(2, s.size());
//
//        assertEquals(1, s.sizeQueue()); //b should be in the queue
//
//        s.commit(); //apply pending changes. try inserting items in the queue. if an item is not able to be inserted it remains buffering
//
//        //3. insert again, bringing "b" effective budget to 0.4
//        s.put("b", new RawBudget(0.2f, 0, 0));
//        assertEquals(2, s.size()); //still not actually in the bag
//        assertEquals("a0=$0.2500;0.0000;0.0000$,a1=$0.2500;0.0000;0.0000$", s.toStringDetailed());
//
//        assertEquals(1, s.sizeQueue()); //b should remain in the queue, accumulating the 2nd dose
//
//        s.commit(); //apply pending changes, while sorting. then try inserting items in the queue. if an item is
//
//        //4. if "b" has been successfully backlogged in the time prior to the commit,
//        //its accumulated budget is enough to displace a previous entry
//
//        assertEquals(2, s.size()); //still not actually in the bag
//        assertEquals(0, s.sizeQueue()); //commit has cleared queue
//        assertEquals("b=$0.4000;0.0000;0.0000$,a1=$0.2500;0.0000;0.0000$", s.toStringDetailed());
//
//        //TODO test queue finite size and
//
//    }

//    @NotNull
//    public static CurveBag<String> newBag(int n) {
//        Random rng = new XorShift128PlusRandom(1);
//        return new CurveBag(n, rng);
//    }


//
//    static final BagCurve curve = new CurveBag.FairPriorityProbabilityCurve();
//
//    @Test
//    public void testBagSampling() {
//
//        /** for testing normalization:
//         * these should produce similar results regardless of the input ranges */
//        testBags(0.25f, 0.75f,
//                1, 2, 3, 7, 12);
//        testBags(0.5f, 0.6f,
//                1, 2, 3, 7, 12);
//
//        testBags(0, 1.0f,
//                1, 2, 3, 6, 13, 27, 32, 64, 100);
//    }
//
//    public void testBags(float pMin, float pMax, int... capacities) {
//
//
//        //FractalSortedItemList<NullItem> f1 = new FractalSortedItemList<>();
//        //int[] d2 = testCurveBag(f1);
//        //int[] d3 = testCurveBag(new RedBlackSortedIndex<>());
//        //int[] d1 = testCurveBag(new ArraySortedIndex<>(40));
//
//
//        //use the final distribution to compare that each implementation generates exact same results
//        //assertTrue(Arrays.equals(d1, d2));
//        //assertTrue(Arrays.equals(d2, d3));
//
//        int repeats = 2;
//
//        System.out.println("Bag sampling distributions, inputs priority range=" + pMin + " .. " + pMax);
//        for (int capacity : capacities ) {
//
//
//            double[] total = new double[capacity];
//
//            for (int i = 0; i < repeats; i++) {
//                double[] count = testRemovalDistribution(pMin, pMax, capacity);
//                total = MathArrays.ebeAdd(total, count);
//            }
//
//            System.out.println("  " + capacity + ',' + " = " + Arrays.toString(total));
//
//        }
//
//    }
//
//    @Test  public void testCurveBag() {
//
//        ArraySortedIndex items = new ArraySortedIndex(1024);
//
//        testCurveBag(items);
//        testCurveBag(items);
//        testCapacityLimit(new CurveBag(curve, 4, rng));
//
//
//
//        testAveragePriority(4, items);
//        testAveragePriority(8, items);
//
//        int[] d = null;
//        for (int capacity : new int[] { 10, 51, 100, 256 } ) {
//            d = AbstractBagTest.testRemovalPriorityDistribution(items);
//        }
//
//
//    }
//
//    public void testCurveBag(SortedIndex<NullItem> items) {
//        CurveBag<CharSequence, NullItem> f = new CurveBag<>(items, curve, rng);
//        assertEquals(0, f.getPrioritySum(), 0.001);
//
//
//        NullItem ni;
//        f.put(ni = new NullItem(0.25f));
//        assertEquals(1, f.size());
//        assertEquals(ni.getPriority(), f.getPrioritySum(), 0.001);
//
//        f.put(new NullItem(0.9f));
//        f.put(new NullItem(0.75f));
//
//        //System.out.println(f);
//
//        //sorted
//        assertEquals(3, f.size());
//        assertTrue(f.getItems().toString(),
//                f.get(0).getPriority() > f.get(1).getPriority());
//
//        f.pop();
//
//        assertEquals(2, f.size());
//        f.pop();
//        assert(f.size() == 1);
//        f.pop();
//        assert(f.isEmpty());
//
//        assertEquals(0, f.getPrioritySum(), 0.01);
//    }
//
//    public void testCapacityLimit(Bag<CharSequence,NullItem> f) {
//
//        NullItem four = new NullItem(0.4f);
//        NullItem five = new NullItem(0.5f);
//
//        f.put(four); testOrder(f);
//
//
//
//        f.put(five); testOrder(f);
//
//        f.put(new NullItem(0.6f)); testOrder(f);
//
//
//        Item a = f.put(new NullItem(0.7f)); assertNull(a); testOrder(f);
//
//        assertEquals(4, f.size());
//        assertEquals(f.size(), f.keySet().size());
//        assertTrue(f.contains(five));    //5 should be in lowest position
//
//        System.out.println("x\n"); f.printAll();
//
//        f.put(new NullItem(0.8f)); //limit
//
//        System.out.println("x\n"); f.printAll(); testOrder(f);
//
//        assertEquals(4, f.size());
//    }
//
//    private void testOrder(Bag<CharSequence, NullItem> f) {
//        float max = f.getPriorityMax();
//        float min = f.getPriorityMin();
//
//        Iterator<NullItem> ii = f.iterator();
//
//        NullItem last = null;
//        do {
//            NullItem n = ii.next();
//            if (last == null)
//                assertEquals(max, n.getPriority(), 0.001);
//            else {
//                assertTrue(n.getPriority() <= last.getPriority() );
//            }
//
//            last = n;
//
//        } while (ii.hasNext());
//
//        assertEquals(min, last.getPriority(), 0.001);
//
//    }
//
//
//
//    public static double[] testRemovalDistribution(float priMin, float priMax, int capacity) {
//        int samples = 512 * capacity;
//
//        double[] count = new double[capacity];
//
//
//
//        CurveBag<CharSequence, NullItem> f = new CurveBag(curve, capacity, rng);
//
//        //fill
//        for (int i= 0; i < capacity; i++) {
//            f.put(new NullItem(priMin, priMax));
//            assertTrue(f.isSorted());
//        }
//
//        assertEquals(f.size(), f.capacity());
//
//
//        for (int i= 0; i < samples; i++) {
//            count[f.sample()]++;
//        }
//
//        assert(Util.isSemiMonotonicallyDec(count));
//
//        //System.out.println(random + " " + Arrays.toString(count));
//        //System.out.println(count[0] + " " + count[1] + " " + count[2] + " " + count[3]);
//
//        return count;
//    }
//
//    public void testAveragePriority(int capacity, SortedIndex<NullItem> items) {
//
//
//        float priorityEpsilon = 0.01f;
//
//        CurveBag<CharSequence, NullItem> c = new CurveBag<>(items, curve, rng);
//        LevelBag<CharSequence, NullItem> d = new LevelBag<>(capacity, 10);
//
//        assertEquals(c.getPrioritySum(), d.getPrioritySum(), 0);
//        assertEquals(c.getPriorityMean(), d.getPriorityMean(), 0);
//
//        c.printAll(System.out);
//
//        c.put(new NullItem(0.25f));
//        d.put(new NullItem(0.25f));
//
//        c.printAll(System.out);
//
//        //check that continuousbag and discretebag calculate the same average priority value
//        assertEquals(0.25f, c.getPriorityMean(), priorityEpsilon);
//        assertEquals(0.25f, d.getPriorityMean(), priorityEpsilon);
//
//        c.clear();
//        d.clear();
//
//        assertEquals(0, c.size());
//        assertEquals(0, d.size());
//        assertEquals(0, c.getPrioritySum(), 0.001);
//        assertEquals(0, d.getPrioritySum(), 0.001);
//        assertEquals(0, c.getPriorityMean(), 0.001);
//        assertEquals(0, d.getPriorityMean(), 0.001);
//
//        c.put(new NullItem(0.30f));
//        d.put(new NullItem(0.30f));
//        c.put(new NullItem(0.50f));
//        d.put(new NullItem(0.50f));
//
//        assertEquals(0.4, c.getPriorityMean(), priorityEpsilon);
//        assertEquals(0.4, d.getPriorityMean(), priorityEpsilon);
//
//    }
//
//
////    public void testCurveBag2(boolean random) {
////        ContinuousBag2<NullItem,CharSequence> f = new ContinuousBag2(4, new ContinuousBag2.PriorityProbabilityApproximateCurve(), random);
////
////        f.putIn(new NullItem(.25f));
////        assert(f.size() == 1);
////        assert(f.getMass() > 0);
////
////        f.putIn(new NullItem(.9f));
////        f.putIn(new NullItem(.75f));
////        assert(f.size() == 3);
////
////        //System.out.println(f);
////
////        //sorted
////        assert(f.items.first().getPriority() < f.items.last().getPriority());
////        assert(f.items.first().getPriority() < f.items.exact(1).getPriority());
////
////        assert(f.items.size() == f.nameTable.size());
////
////        assert(f.size() == 3);
////
////        f.takeOut();
////        assert(f.size() == 2);
////        assert(f.items.size() == f.nameTable.size());
////
////        f.takeOut();
////        assert(f.size() == 1);
////        assert(f.items.size() == f.nameTable.size());
////        assert(f.getMass() > 0);
////
////        f.takeOut();
////        assert(f.size() == 0);
////        assert(f.getMass() == 0);
////        assert(f.items.size() == f.nameTable.size());
////
////    }
//
//
//    @Test public void testEqualBudgetedItems() {
//        int capacity = 4;
//
//        CurveBag<CharSequence, NullItem> c = new CurveBag(curve, capacity, rng);
//        c.mergeAverage();
//
//        NullItem a, b;
//        c.put(a = new NullItem(0.5f));
//        c.put(b = new NullItem(0.5f));
//
//        assertEquals(2, c.size());
//
//        NullItem aRemoved = c.remove(a.name());
//
//        assertEquals(aRemoved, a);
//        assertNotEquals(aRemoved, b);
//        assertEquals(1, c.size());
//
//        c.put(a);
//        assertEquals(2, c.size());
//
//        NullItem x = c.peekNext();
//        assertNotNull(x);
//
//        assertEquals(2, c.size());
//
//        x = c.pop();
//
//        assertTrue(x.equals(a) || x.equals(b));
//        assertEquals(1, c.size());
//
//    }
//
//
//    @Test public void testMerge() {
//        int capacity = 3;
//
//        //final AtomicInteger putKey = new AtomicInteger(0);
//        //final AtomicInteger removeKey = new AtomicInteger(0);
//
//
//        CurveBag<CharSequence, NullItem> c = new CurveBag<>(curve, capacity, rng);
////
////
////            protected ArrayMapping<CharSequence, NullItem> newIndex(int capacity) {
////                return new ArrayMapping<CharSequence, NullItem>(
////                        //new HashMap(capacity)
////                        Global.newHashMap(capacity),
////                        items
////                ) {
////                    @Override
////                    public NullItem put(NullItem value) {
////                        return super.put(value);
////                    }
////
////                    @Override
////                    public NullItem putKey(CharSequence key, NullItem value) {
////                        putKey.incrementAndGet();
////                        return super.putKey(key, value);
////                    }
////
////                    @Override
////                    public NullItem remove(CharSequence key) {
////                        removeKey.incrementAndGet();
////                        return super.remove(key);
////                    }
////                };
////            }
////        };
//        c.mergePlus();
//
//        NullItem a = new NullItem(0.5f, "a");
//
//        c.put(a);
//
//        //assertEquals(1, putKey.get()); assertEquals(0, removeKey.get());
//        assertEquals(1, c.size());
//
//        c.put(a);
//
//        //assertEquals(2, putKey.get()); assertEquals(0, removeKey.get());
//        assertEquals(1, c.size());
//
//
//
//        //merged with plus, 0.5 + 0.5 = 1.0
//        assertEquals(1.0f, c.iterator().next().getPriority(), 0.001);
//
//
//        c.validate();
//
//        c.mergeAverage();
//
//        //for average merge, we need a new instance of same key
//        //but with different priority so that it doesnt modify itself (having no effect)
//        NullItem a2 = new NullItem(0.5f, "a");
//
//        c.put(a2);
//
//        //still only one element
//        assertEquals(1, c.size());
//
//        //but the merge should have decreased the priority from 1.0
//        assertEquals(0.833f, c.iterator().next().getPriority(), 0.001);
//
//
//        //finally, remove everything
//
//        c.remove(a.name());
//
//        //assertEquals(3, putKey.get()); assertEquals(1, removeKey.get());
//        assertEquals(0, c.size());
//
//        c.validate();
//
//    }
//

}

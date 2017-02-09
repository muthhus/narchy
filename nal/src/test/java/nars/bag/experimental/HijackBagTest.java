package nars.bag.experimental;

import nars.bag.Bag;
import nars.bag.BagTest;
import nars.budget.BudgetMerge;
import org.junit.Test;

import java.util.Random;

import static nars.bag.BagTest.rng;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 2/9/17.
 */
public class HijackBagTest {

    @Test
    public void testHijackResize() {
        Random rng = rng();
        Bag<String> b = new HijackBag(0, 7, BudgetMerge.maxBlend, rng);
        BagTest.populate(b, rng, 10, 20, 0f, 1f, 0.5f);
        assertEquals(0, b.size());


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

    public void assertApproximatelySized(Bag<String> b, int expected, float closeness) {
        int bSize = b.size();
        float error = Math.abs(expected - bSize) / (Math.max(bSize, (float) expected));
        System.out.println(bSize + "  === " + expected + ", diff=" + error);
        assertTrue(error < closeness);
    }
}
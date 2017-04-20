package jcog.bag;

import com.google.common.base.Joiner;
import jcog.bag.impl.hijack.PLinkHijackBag;
import jcog.pri.PLink;
import jcog.pri.RawPLink;
import jcog.random.XorShift128PlusRandom;
import org.apache.commons.math3.stat.Frequency;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * Created by me on 4/15/17.
 */
public class BagFairnessTest {

    final Random rng = new XorShift128PlusRandom(1);

    @Test
    public void test1() {
        test(new PLinkHijackBag<String>(16, 4, rng));
    }

    private void test(Bag<String,PLink<String>> b) {
        //initial conditions
        b.put(new RawPLink("a", 0.9f));
        b.put(new RawPLink("b", 0.5f));
        b.put(new RawPLink("c", 0.1f));

        b.commit();
        //b.print();

        for (int n : new int[] { 50, 100, 1000 }) {
            Frequency e = new Frequency();
            b.sample(n, x -> {
                e.addValue(x.get());
                return true;
            });
            System.out.println(Joiner.on("\n").join(e.entrySetIterator()) + "\n");
            assertEquals(3, e.getUniqueCount());
            assertEquals(e.getPct("a")*(5/9f), e.getPct("b"), 0.1f);
            assertEquals(e.getPct("a")*(1/9f), e.getPct("c"), 0.05f);
        }

    }
}

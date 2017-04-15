package nars.bag.experimental;

import com.google.common.base.Joiner;
import jcog.bag.Bag;
import jcog.bag.PLink;
import jcog.bag.Prioritized;
import jcog.bag.RawPLink;
import jcog.bag.impl.PLinkHijackBag;
import jcog.random.XorShift128PlusRandom;
import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.stat.Frequency;
import org.junit.Test;

import java.util.Random;

/**
 * Created by me on 4/15/17.
 */
public class BagFairnessTest {

    final Random rng = new XorShift128PlusRandom(1);

    @Test
    public void test1() {
        test(new PLinkHijackBag<String>(8, 3, rng));
    }

    private void test(Bag<String,PLink<String>> b) {
        //initial conditions
        b.put(new RawPLink("a", 0.9f));
        b.put(new RawPLink("b", 0.5f));
        b.put(new RawPLink("c", 0.1f));

        b.commit();
        b.print();

        Frequency e = new Frequency();
        b.sample(1000, x -> {
          e.addValue(x.get());
          return true;
        });
        System.out.println(Joiner.on("\n").join(e.entrySetIterator()));

    }
}

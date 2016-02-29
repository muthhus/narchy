package nars.op.sys;

import nars.$;
import nars.NAR;
import nars.nar.Default;
import org.apache.commons.math3.stat.Frequency;
import org.junit.Test;

/**
 * Created by me on 2/19/16.
 */
public class KernelTest {



    @Test
    public void testBenchmark() {
        NAR n = new Default();

        final Frequency f = new Frequency();

        int cap = 16;
        Kernel k = new Kernel(n, cap);
        for (int i = 0; i < cap; i++) {
            final int ii = i;

            /**
             * create each one with a different priority level.
             * this should result in each task being run at slightly different intervals from low to high.
             * we can use a linear array of dummy tasks like this as an impulse test to benchmark an
             * online system for tuning QoS demands against compute resource costs.
             */

            k.run( i * 1f / cap, "benchmark", ()->{
                //System.out.println(ii + " run");
                f.addValue(ii);
            }, $.the(i));
        }

        for (int i = 0; i < 10; i++) {
            System.out.println(f);
            n.run(100);
        }
    }

}
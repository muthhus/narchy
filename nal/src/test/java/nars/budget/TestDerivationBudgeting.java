package nars.budget;

import jcog.O;
import jcog.random.XORShiftRandom;
import jcog.random.XorShift128PlusRandom;
import nars.nar.Default;
import nars.time.CycleTime;
import nars.util.exe.BufferedSynchronousExecutor;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class TestDerivationBudgeting {

    @Test
    public void testWonderAboutIncompletePlan() {

        O o = O.of(
            BufferedSynchronousExecutor.class,
            CycleTime.class,
            new Default.DefaultTermIndex(1024)
        );
        System.out.println(o);


        O.How<Default> h = o.how(Default.class);
        System.out.println(h);
        assertNotNull(h);

        assertEquals(1, h.wonder.size());

        @Nullable Default d = h.get();

    }

    @Test
    public void testComplete() {
        O o = O.of(
                BufferedSynchronousExecutor.class,
                CycleTime.class,
                new Default.DefaultTermIndex(1024),
                new XorShift128PlusRandom(1)
        );
        System.out.println(o);

        System.out.println();

        O.How<Default> h = o.how(Default.class);
        System.out.println(h);

        h.get();

    }
}

package nars.budget;

import jcog.O;
import nars.nar.Default;
import nars.time.CycleTime;
import nars.util.exe.BufferedSynchronousExecutor;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;


public class TestDerivationBudgeting {

    @Test
    public void testDerivationStealsParentBudget() {

        O o = O.of(
            BufferedSynchronousExecutor.class,
            CycleTime.class,
            new Default.DefaultTermIndex(1024)
        );
        System.out.println(o.toString());


        Default x = o.a(Default.class);
        assertNotNull(x);



        //at the right speed to ensure that "trains" of thought can form through time (relative to forgetting rates, etc)
    }

}

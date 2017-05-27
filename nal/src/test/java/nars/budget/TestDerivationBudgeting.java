package nars.budget;

import jcog.O;
import jcog.decide.Deciding;
import jcog.random.XorShift128PlusRandom;
import nars.nar.Default;
import nars.time.CycleTime;
import nars.util.exe.BufferedSynchronousExecutor;
import org.junit.Test;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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


        O.Possible<Default> h = o.possible(Default.class);
        System.out.println(h);
        assertNotNull(h);

        //assertEquals(1, h.unknown.size());

        //@Nullable Default d = h.get();

    }

    @Test
    public void testComplete() {
        O o = O.of(
                BufferedSynchronousExecutor.class,
                CycleTime.class,
                new Default.DefaultTermIndex(1024),
                new XorShift128PlusRandom(1)
        );


        O.Possible<Default> h = o.possible(Default.class);
        System.out.println(h);

        Default d = o.get(Default.class, new O.How() {

            @Override
            public int which(List options) {
                return ThreadLocalRandom.current().nextInt(options.size()); //random
            }

            @Override
            public Object value(Parameter inConstructor) {

                Class<?> tt = inConstructor.getType();
                System.out.println(inConstructor + " " + tt);
                if (tt == int.class) {
                    return Integer.valueOf(1);
                }
                return null;
            }
        });
        //h.get();

    }
}

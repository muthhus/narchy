package nars.budget;

import jcog.O;
import jcog.random.XorShift128PlusRandom;
import nars.conceptualize.DefaultConceptBuilder;
import nars.index.term.NullTermIndex;
import nars.nar.Default;
import nars.time.CycleTime;
import nars.util.exe.BufferedSynchronousExecutor;
import org.junit.Test;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Random;
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

        //assertEquals(1, h.unknown.size());

        //@Nullable Default d = h.get();

    }

    @Test
    public void testComplete() {

        O of = O.of(
                BufferedSynchronousExecutor.class,
                CycleTime.class,

                new Default.DefaultTermIndex(1024),
                new NullTermIndex(new DefaultConceptBuilder()),

                new XorShift128PlusRandom(1),
                Random.class

        );
        for (int i = 0; i < 100; i++) {
            Default d = of.a(Default.class, new O.How<>() {

                @Override
                public int impl(List choose) {
                    return ThreadLocalRandom.current().nextInt(choose.size()); //random
                }

                @Override
                public Object value(Parameter inConstructor) {

                    Class<?> tt = inConstructor.getType();
                    //System.out.println(inConstructor + " " + tt);
                    if (tt == int.class) {
                        return 1;
                    } else if (tt == float.class) {
                        return 0.5f;
                    } else if (tt == long.class) {
                        return 1L;
                    }
                    return null;
                }
            });
            System.out.println();
        }

    }
}

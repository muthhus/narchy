package nars.nal.nal8;

import com.google.common.base.Joiner;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.nar.Default;
import nars.util.signal.FuzzyScalar;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.junit.Test;

import java.util.TreeSet;

import static nars.util.Texts.n2;

/**
 * Created by me on 7/11/16.
 */
public class RuleInductionTest {

    @Test
    public void testBalancedRuleInduction() {

        Param.DEBUG = true;

        Default d = new Default(1024, 6, 2, 2);


        int loops = 32;
        float hz = 1f/16f;

        MutableFloat m = new MutableFloat(0);

        FuzzyScalar f = new FuzzyScalar(m, d,
                //"f:lo", "f:hi"
                "(f_lo)", "(f_hi)"
        );

        FuzzyScalar g = new FuzzyScalar(()->1f-m.floatValue(), d,
                //"g:lo", "g:hi"
                "(g_lo)", "(g_hi)"
        );


        for (int i = 0; i < loops/hz; i++) {
            m.setValue( (float)(Math.sin(i*hz*(Math.PI*2)) * 0.5f + 0.5f) );
            //System.out.println(m.floatValue());
            d.next();
            System.out.println( Joiner.on("\t").join(
                    d.concept("(f_lo)").belief(d.time()),
                    d.concept("(f_hi)").belief(d.time()),
                    n2(m.floatValue()),
                    d.concept("(g_lo)").belief(d.time()),
                    d.concept("(g_hi)").belief(d.time())
            ));

        }

        TreeSet<Task> nameSorted = new TreeSet<Task>((x, y)->x.toString().compareTo(y.toString()));

        NAR.printTasks(d, true, (x) -> {
            if ((x.op().temporal) && (x.volume()<=7) && (x.dt() <= 1)) {
                x.budget().zero();
                nameSorted.add(x);
            }
        });
        nameSorted.forEach(x -> {
            System.out.println(x.proof());
        });

    }
}

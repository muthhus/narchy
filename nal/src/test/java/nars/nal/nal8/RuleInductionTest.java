package nars.nal.nal8;

import nars.Global;
import nars.NAR;
import nars.nar.Default;
import nars.task.Task;
import nars.util.signal.FuzzyConceptSet;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.junit.Test;

import java.util.TreeSet;

/**
 * Created by me on 7/11/16.
 */
public class RuleInductionTest {

    @Test
    public void testBalancedRuleInduction() {

        Global.DEBUG = true;

        Default d = new Default(1024, 6, 2, 2);


        int loops = 100;
        float hz = 1f/16f;

        MutableFloat m = new MutableFloat(0);

        FuzzyConceptSet f = new FuzzyConceptSet(m, d,
                //"f:lo", "f:hi"
                "(f_lo)", "(f_hi)"
        );

        FuzzyConceptSet g = new FuzzyConceptSet(()->1f-m.floatValue(), d,
                //"g:lo", "g:hi"
                "(g_lo)", "(g_hi)"
        );


        for (int i = 0; i < loops/hz; i++) {
            m.setValue( (float)(Math.sin(i*hz*(Math.PI*2)) * 0.5f + 0.5f) );
            //System.out.println(m.floatValue());
            d.next();
        }

        TreeSet<Task> nameSorted = new TreeSet<Task>((x,y)->x.toString().compareTo(y.toString()));

        NAR.printTasks(d, true, (x) -> {
            if ((x.op().temporal) && (x.volume()<=7) && (x.dt() <= 1)) {
                x.budget().zero();
                nameSorted.add(x);
            }
        });
        nameSorted.forEach(x -> {
            System.out.println(x);
        });

    }
}

package nars.budget;

import nars.Narsese;
import nars.nar.Default;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.Test;

/**
 * Created by me on 10/28/16.
 */
public class TaskBudgetingTest {

    /** taskbudgeting - structural deduction test
    decompose large conj, analyze the durability, quality decay */
    @Test
    public void structuralDeduction1() throws Narsese.NarseseException {

        DescriptiveStatistics qua = new DescriptiveStatistics();

        Default d = new Default();

        d.onTask(t -> {
            qua.addValue( t.qua() );
        });

        d.log();

        d.believe("(&&, (a),(b),(c),(d),(e) )");

        d.run(100);

        System.out.println("qua: \n" + qua);

    }

}
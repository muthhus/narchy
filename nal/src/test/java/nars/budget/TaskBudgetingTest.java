package nars.budget;

import nars.NAR;
import nars.Narsese;
import nars.nar.NARS;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by me on 10/28/16.
 */
@Ignore
public class TaskBudgetingTest {

    /** taskbudgeting - structural deduction test
    decompose large conj, analyze the durability, quality decay */
    @Test
    public void structuralDeduction1() throws Narsese.NarseseException {


        NAR d = new NARS().get();


        d.log();

        d.believe("(&&, (a),(b),(c),(d),(e) )");

        d.run(100);

    }

}
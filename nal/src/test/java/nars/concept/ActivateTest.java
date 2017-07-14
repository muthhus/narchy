package nars.concept;

import nars.NAR;
import nars.Narsese;
import nars.nar.NARS;
import org.junit.Test;

/**
 * Created by me on 9/9/15.
 */
public class ActivateTest {

    @Test
    public void testDerivedBudgets() throws Narsese.NarseseException {

        NAR n= new NARS().get();

        //TODO System.err.println("TextOutput.out impl in progress");
        //n.stdout();


        n.input("$0.1$ <a --> b>.");
        n.input("$0.1$ <b --> a>.");
        n.run(15);


        n.forEachConceptActive(System.out::println);
    }
}

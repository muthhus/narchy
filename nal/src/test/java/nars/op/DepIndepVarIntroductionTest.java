package nars.op;

import nars.$;
import nars.Narsese;
import nars.nar.Terminal;
import nars.term.Term;
import org.junit.Test;

/**
 * Created by me on 3/30/17.
 */
public class DepIndepVarIntroductionTest {

    @Test
    public void testIntroducedVariablesNormalized() throws Narsese.NarseseException {
        Terminal n = new Terminal();

        Term t = $.$("(&&,(a-->c),(b-->c))");
        Term u = $.$("varIntro(" + t + ")").eval(n);
        System.out.println(t + " " + u);

    }
}
package nars.op;

import nars.$;
import nars.NAR;
import nars.nar.Default;
import nars.term.obj.Termject;
import org.junit.Test;

/**
 * Created by me on 8/5/16.
 */
public class ArithmeticInductionTest2 {

    @Test
    public void testVarIntro() {
        NAR n = new Default();

        //new ArithmeticInduction(n);

        new VariableCompressor(n);

        n.log();

//        n.believe($.parallel($.p(0, 1), $.p(0, 3), $.p(0, 5)));
//        n.next();

        n.believe($.parallel($.p($.p(0,1), new Termject.IntTerm(1)), $.p($.p(0,1), new Termject.IntTerm(3)), $.p($.p(0,1), new Termject.IntTerm(5))));
        n.next();

    }
}

package nars.op;

import nars.$;
import nars.NAR;
import nars.nar.Default;
import nars.term.Compound;
import nars.term.obj.IntTerm;
import nars.term.obj.Termject;
import org.junit.Test;

import static nars.$.*;
import static org.junit.Assert.assertEquals;

/**
 * Created by me on 8/5/16.
 */
public class ArithmeticInductionTest {

    @Test
    public void testIntRangeCompression() {


        assertEquals( //simple range of raw ints
                "`1<=?<=2`",
                conj(the(1), the(2)).toString()
        );
        assertEquals( //simple range of raw ints
                "`1<=?<=3`",
                conj(the(1), the(2), the(3)).toString()
        );

        assertEquals( //range can not be formed
                "(`1`&&`3`)",
                conj(the(1), the(3)).toString()
        );

        assertEquals( //simple range of embedded ints
                "(x,`1<=?<=2`)",
                conj(p(the("x"), the(1)), p(the("x"), the(2))).toString()
        );
    }

    @Test
    public void testIntRangeCompressionInvalid1() {
        assertEquals(
                "((x,`1`)&&(y,`2`))",
                conj(p(the("x"), the(1)), p(the("y"), the(2))).toString()
        );
    }
    @Test
    public void testIntRangeCompressionInvalid2() {
        assertEquals(
                "((x,`1<=?<=2`)&&(y,`2`))",
                conj(p(the("x"), the(1)), p(the("x"), the(2)), p(the("y"), the(2))).toString()
        );
    }
    @Test public void testInvalidDueToDT() {
        assertEquals(
                "((x,`1`) &&+1 (x,`2`))",
                conj(p(the("x"), the(1)), 1, p(the("x"), the(2))).toString()
        );
    }

    @Test
    public void testIntRangeCompressionPartial() {
        assertEquals( //partially covered range of embedded ints
                "((x,`4`)&&(x,`1<=?<=2`))",
                conj(p(the("x"), the(1)), p(the("x"), the(2)), p(the("x"), the(4))).toString()
        );
    }

    @Test
    public void testIntRangeCombination() {
        assertEquals( //partially covered range of embedded ints
                "(x,`1<=?<=4`)",
                conj(
                    p(the("x"), new Termject.IntInterval(1,2)),
                    p(the("x"), new Termject.IntInterval(2,4)))
                .toString()
        );
    }


//        System.out.println(
//                //n.normalize((Compound)
//                    parallel(p(p(0,1), the(1)), p(p(0,1), the(3)), p(p(0,1), the(5)))
//
//        );
        //n.next();


}

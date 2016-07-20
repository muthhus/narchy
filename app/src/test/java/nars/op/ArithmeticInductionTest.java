package nars.op;

import nars.NAR;
import nars.Param;
import nars.nar.Default;
import nars.util.signal.TestNAR;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by me on 7/20/16.
 */
public class ArithmeticInductionTest {

    @Test
    public void test0() {
        test().inputAt(1, "((x,1,2)&&(y,2,4)).").mustNotOutput().test(cycles); //should find nothing
    }

    @Test
    public void test1() {
        test().inputAt(1, "((x,1)&&(y,2)).").mustNotOutput().test(cycles); //should find nothing
    }

    @Test
    public void test2() {
        //should find only one pattern
        test("((x,1) && (x,2))", "((x,$1)&&(intRange,$1,1,1))");
    }
    @Test
    public void test3() {
        //should find only one pattern
        test("((x,1,2) && (x,1,4))", "((x,1,$1)&&(intRange,$1,2,2))");
    }
    @Test
    public void test4() {
        //should find only one pattern involving 2 variables
        test("((x,1,2) && (x,2,4))", "(&&,(x,$1,$2),(intRange,$1,1,1),(intRange,$2,2,2))");
    }

    static {
        Param.DEBUG = true;
    }

    final static int cycles = 32;

    TestNAR test() {
        NAR d = new Default();
        new ArithmeticInduction(d);

        return new TestNAR(d);
    }

    void test(String input, String expected) {
        test(input, expected, false);
    }
    void test(String input, String expected, boolean log) {

        TestNAR t = test()
                .mustBelieve(cycles, expected, 1f, 0.9f);

        if (log)
            t.log();

        t.believe(input).test();

    }

}
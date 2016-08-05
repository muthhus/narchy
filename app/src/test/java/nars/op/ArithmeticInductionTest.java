package nars.op;

import nars.$;
import nars.NAR;
import nars.Param;
import nars.nar.Default;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.obj.Termject;
import nars.util.signal.TestNAR;
import org.junit.Test;


public class ArithmeticInductionTest {

    @Test public void testNew() {
        //"((x,`3`) &&+0 (y,`2`)). 4-3 %1.0;.36%" //<- should not produce anything

        TestNAR test = test();
        Compound b = (Compound) $.conj(
                $.p(new Termject.IntTerm(3), $.the("x")),
                0,
                $.p(new Termject.IntTerm(2), $.the(/*"x"*/  "y" ))
        );
        b = $.impl(b, $.the("z")); //this puts the above term inside somethign else, which changes the matching behavior that will be applied

        Task t = new MutableTask(b, '.', 1f, test.nar);
        test.inputAt(1, t).mustBelieve(cycles, "xvxcvc:a.",1,0.9f).test();
    }

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
        test("((x,1) && (x,2))", "((x,$1)&&(intRange,$1,1,1))", true);
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
//    void test(Compound input, Compound expected, boolean log) {
//
//        TestNAR t = test()
//                .mustOutput()
//                .mustBelieve(cycles, expected, 1f, 0.9f);
//
//        if (log)
//            t.log();
//
//        t.nar.believe(input);
//        t.test();
//
//    }
}
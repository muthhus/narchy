package nars.op.scheme;


import nars.NAR;
import nars.nal.AbstractNALTest;
import nars.util.signal.TestNAR;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

@RunWith(Parameterized.class)
public class EvalSchemeTest extends AbstractNALTest {

    public EvalSchemeTest(Supplier<NAR> build) {
        super(build);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable configurations() {
        return AbstractNALTest.nars(8, false);
    }

    String factorialFunc = "(define factorial (lambda (n) (if (= n 1) 1 (* n (factorial (- n 1))))))";
    String factorialTest = "(factorial 3)";

    @Test
    public void testSharedSchemeNALRepresentations() {

        String defineFactorial = "scheme(\"" + factorialFunc + "\", #y)!";

        test()
        //.log()
        .inputAt(1, defineFactorial)
        .inputAt(2, "scheme(\"factorial\", #x)!")
        .inputAt(3, "scheme(\"" + factorialTest + "\", #y)!")
        .mustBelieve(7, "(6-->(/,^scheme,\"(factorial 3)\",_))", 1f, 0.9f, 9);

    }

    @Test
    public void testCAR() {

        test()
            .input("scheme((car, (quote, (2, 3))), #x)!")
            .mustBelieve(4, "<2 --> (/, ^scheme, (car, (quote, (2, 3))), _)>", 1f, 0.9f, 3);


    }

//    //----
//    @Test @Ignore
//    public void testDynamicBrainfuckProxy() throws Exception {
//
//        NAR n = new NAR(new Default().clock(new HardRealtimeClock(false)) );
//
//        //TextOutput.out(n);
//
//        BrainfuckMachine bf= new NALObjects(n).build("scm", BrainfuckMachine.class);
//
//        bf.execute("++++++++[>++++[>++>+++>+++>+<<<<-]>+>+>->>+[<]<-]>>.>---.+++++++..+++.>>.<-.<.+++.------.--------.>>+.>++.");
//
//        n.frame(6500);
//
//    }

/*
    @Test @Ignore
    public void testDynamicSchemeProxy() throws Exception {

        NAR n = new NAR(new Default().clock(new HardRealtimeClock(false)) );

        //TextOutput.out(n);

        SchemeClosure env = new NALObjects(n).build("scm", SchemeClosure.class);

        String input = "(define factorial (lambda (n) (if (= n 1) 1 (* n (factorial (- n 1))))))";

        env.eval(input);

        List<Expression> result = env.eval("(factorial 3)");

        assertThat(result.get(0), is(number(6)));

        n.frame(50);

        n.frame(1660);
        }
*/

//
//
//        new Thread( () -> { Repl.repl(System.in, System.out, e); } ).start();
//
//        while (true) {
//            n.frame(10);
//            Thread.sleep(500);
//        }
//


//        Class derivedClass = new NALObject().add(new TestHandler()).connect(TestClass.class, n);
//
//        System.out.println(derivedClass);
//
//        Object x = derivedClass.newInstance();
//
//        System.out.println(x);
//
//        ((TestClass)x).callable();


}

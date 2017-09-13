package nars.perf;

import com.google.common.collect.Lists;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.nal.nal1.NAL1Test;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParametersFactory;
import org.junit.runners.parameterized.TestWithParameters;
import org.openjdk.jmh.annotations.*;

import java.util.function.Supplier;

/**
 * Created by me on 4/24/17.
 */
@State(Scope.Benchmark)
public class NARTestBenchmark {

//    @Setup public void prepare() {
//        System.out.println(DefaultDeriver.the); //warm
//    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void testExample() throws InitializationError {
        RunNotifier n = new RunNotifier();
        new BlockJUnit4ClassRunnerWithParametersFactory().createRunnerForTestWithParameters(new TestWithParameters(
            "x", new TestClass(NAL1Test.class), Lists.newArrayList( (Supplier)(()-> new NARS().get()) )
        )).run(n);
    }

//    public static void main(String[] args) throws RunnerException {
//        perf(NARTestBenchmark.class, 4, 1);
//    }
}

//public class TestBenchmark1 {
//
////    static String eval(String script) {
////        // We don't actually need the context object here, but we need it to have
////        // been initialized since the
////        // constructor for Ctx sets static state in the Clojure runtime.
////
////        Object result = Compiler.eval(RT.readString(script));
////
////        return RT.printString(result) + " (" +result.getClass() + ")";
////    }
////    @Benchmark
////    @BenchmarkMode(value = Mode.SingleShotTime)
////    public void eval1() {
////
////        new Dynajure().eval("(+ 1 1)");
////    }
////
////    @Benchmark
////    @BenchmarkMode(value = Mode.SingleShotTime)
////    public void eval2() {
////        new Dynajure().eval("(* (+ 1 1) 8)");
////        //out.println(eval("'(inh a b)") );
////        //out.println(eval("'[inh a b]") );
////    }
//
//    @Benchmark
//    @BenchmarkMode(Mode.SingleShotTime)
//    public void testExecution() throws Narsese.NarseseException {
//        NAR n = new NARS().get();
//        //n.log();
//        n.input("a:b!");
//        n.input("<(rand 5)==>a:b>.");
//
//        n.run(6);
//    }
//
////    public static void main(String[] args) throws RunnerException {
////        perf(TestBenchmark1.class, 6, 10);
////
////    }
//}

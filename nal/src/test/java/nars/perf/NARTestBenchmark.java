package nars.perf;

import com.google.common.collect.Lists;
import nars.NARS;
import nars.nal.nal1.NAL1Test;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParametersFactory;
import org.junit.runners.parameterized.TestWithParameters;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.RunnerException;

import java.util.function.Supplier;

import static nars.perf.JmhBenchmark.perf;

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

    public static void main(String[] args) throws RunnerException {
        perf(NARTestBenchmark.class, 4, 1);
    }
}

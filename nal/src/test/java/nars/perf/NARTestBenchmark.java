package nars.perf;

import com.google.common.collect.Lists;
import junit.textui.TestRunner;
import nars.nal.nal6.NAL6Test;
import nars.nar.Default;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParametersFactory;
import org.junit.runners.parameterized.ParametersRunnerFactory;
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

    @Benchmark
    @BenchmarkMode(value = Mode.AverageTime)
    public void testNal6() throws InitializationError {
        RunNotifier n = new RunNotifier();
//        n.addListener(new RunListener() {
//            @Override  public void testRunFinished(Result result) throws Exception {
//                System.out.println("result: " + result);
//            }
//        });
        new BlockJUnit4ClassRunnerWithParametersFactory().createRunnerForTestWithParameters(new TestWithParameters(
            "nal6", new TestClass(NAL6Test.class), Lists.newArrayList( (Supplier)(()->new Default()) )
        )).run(n);
    }

    public static void main(String[] args) throws RunnerException {
        perf(NARTestBenchmark.class, 1, 1);
    }
}

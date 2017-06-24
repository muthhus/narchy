
package nars.perf;

import nars.$;
import nars.NAR;
import nars.nar.NARBuilder;
import nars.test.DeductiveChainTest;
import nars.test.DeductiveMeshTest;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.RunnerException;

import static nars.perf.JmhBenchmark.perf;

@State(Scope.Benchmark)
public class NARBenchmark {

    NAR n;

    @Setup
    public void prepare() {

        n = new NARBuilder().get();
        //n.inputActivation.setValue(0.5f);
        //n.derivedActivation.setValue(0.5f);
        //n.nal(4);

        new DeductiveMeshTest(n, new int[]{16, 16});
        new DeductiveChainTest(n, 10, 9999991, (x, y) -> $.p($.the(x), $.the(y)));
    }


    @Benchmark
    @BenchmarkMode(value = Mode.AverageTime)
    public void deductiveChainTest1() {
        n.run(2000);
    }

//    @Benchmark
//    @BenchmarkMode(value = Mode.AverageTime)
//    public void nal1Deduction() throws Narsese.NarseseException {
//        n.nal(1);
//        Compound a = $("<a-->b>");
//        Compound b = $("<b-->c>");
//
//        n.believe(a);
//        n.believe(b);
//        n.run(10000);
//    }
//
//    @Benchmark
//    @BenchmarkMode(value = Mode.AverageTime)
//    public void nal1DeductionInNAL8() throws Narsese.NarseseException {
//        n.nal(8);
//        Compound a = $("<a-->b>");
//        Compound b = $("<b-->c>");
//
//        n.believe(a);
//        n.believe(b);
//        n.run(10000);
//    }


    public static void main(String[] args) throws RunnerException {
        perf(NARBenchmark.class, 1, 1);
    }

}

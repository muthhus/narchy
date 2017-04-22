package nars.perf;


import nars.NAR;
import nars.Narsese;
import nars.nar.Default;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.RunnerException;

import static nars.perf.Main.perf;

public class TestBenchmark1 {

//    static String eval(String script) {
//        // We don't actually need the context object here, but we need it to have
//        // been initialized since the
//        // constructor for Ctx sets static state in the Clojure runtime.
//
//        Object result = Compiler.eval(RT.readString(script));
//
//        return RT.printString(result) + " (" +result.getClass() + ")";
//    }
//    @Benchmark
//    @BenchmarkMode(value = Mode.SingleShotTime)
//    public void eval1() {
//
//        new Dynajure().eval("(+ 1 1)");
//    }
//
//    @Benchmark
//    @BenchmarkMode(value = Mode.SingleShotTime)
//    public void eval2() {
//        new Dynajure().eval("(* (+ 1 1) 8)");
//        //out.println(eval("'(inh a b)") );
//        //out.println(eval("'[inh a b]") );
//    }

    @Benchmark
    @BenchmarkMode(value = Mode.SingleShotTime)
    public void testExecution() throws Narsese.NarseseException {
        NAR n = new Default();
        //n.log();
        n.input("a:b!");
        n.input("<(rand 5)==>a:b>.");

        n.run(6);
    }

    public static void main(String[] args) throws RunnerException {
        perf(TestBenchmark1.class, 6, 10);

    }
}

package nars.perf.nars.nar.perf;

import jcog.data.random.XorShift128PlusRandom;
import nars.NAR;
import nars.conceptualize.DefaultConceptBuilder;
import nars.experiment.misc.Line1DContinuous;
import nars.index.term.map.CaffeineIndex;
import nars.nar.Default;
import nars.time.FrameTime;
import nars.util.exe.Executioner;
import nars.util.exe.SynchronousExecutor;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.RunnerException;

import static nars.experiment.misc.Line1DContinuous.random;
import static nars.perf.Main.perf;

/**
 * Created by me on 2/21/17.
 */
public class BenchmarkLine1DContinuous {

    public static void main(String[] args) throws RunnerException {
        perf(BenchmarkLine1DContinuous.class, 4, 1);
    }

    @Benchmark
    @BenchmarkMode(value = Mode.AverageTime)
    public void line1d() {

        int time = 5000;

        XorShift128PlusRandom rng = new XorShift128PlusRandom((int)(Math.random()*1000));
        int conceptsPerCycle = 32;

        final Executioner exe =
                //new MultiThreadExecutioner(2, 2048);
                new SynchronousExecutor();

        Default nar = new Default(1024,
                conceptsPerCycle, 1, 3, rng,
                new CaffeineIndex(new DefaultConceptBuilder(), 1024*64, false, exe),
                new FrameTime(1f), exe
        );
        nar.termVolumeMax.set(32);


        nar.beliefConfidence(0.9f);
        nar.goalConfidence(0.9f);

        //nar.truthResolution.setValue(0.02f);


        Line1DContinuous l = new Line1DContinuous(nar, 6,
                //sine(50)
                random(16)
        );


        //NAgents.chart(l);

        //nar.logSummaryGT(System.out, 0.5f);
//        nar.onTask(t -> {
//            if (t instanceof DerivedTask && t.isGoal())
//                System.out.println(t.proof());
//        });

        l.print = false;
        //l.runRT(25, 15000).join();
        l.run(time);


//        NAR.printActiveTasks(nar, true);
//        NAR.printActiveTasks(nar, false);

//        l.predictors.forEach(p->{
//           nar.concept(p).print();
//        });
        System.out.println("AVG SCORE=" + l.rewardSum()/ nar.time());


    }
}

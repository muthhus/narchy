package nars.perf.nars.nar.perf;

import jcog.random.XorShift128PlusRandom;
import nars.conceptualize.DefaultConceptBuilder;
import nars.derive.Deriver;
import nars.derive.InstrumentedDeriver;
import nars.index.term.map.CaffeineIndex;
import nars.nar.Default;
import nars.test.agent.Line1DContinuous;
import nars.time.FrameTime;
import nars.util.exe.Executioner;
import nars.util.exe.InstrumentedExecutor;
import nars.util.exe.SynchronousExecutor;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.RunnerException;

import static nars.perf.Main.perf;
import static nars.test.agent.Line1DContinuous.random;

/**
 * Created by me on 2/21/17.
 */
public class BenchmarkLine1DContinuous {

    public static void main(String[] args) throws RunnerException {
        perf(BenchmarkLine1DContinuous.class, 2, 1);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public void line1d() {

        int time = 2500;

        XorShift128PlusRandom rng = new XorShift128PlusRandom((int)(Math.random()*1000));
        int conceptsPerCycle = 16;

        Executioner exe =
                //new MultiThreadExecutioner(3, 2048, true);
                new SynchronousExecutor();

        exe = new InstrumentedExecutor( exe, 16 );

        InstrumentedDeriver r = null ; //new InstrumentedDeriver((TrieDeriver) (DefaultDeriver.the));

        Default nar = new Default(1024,
                conceptsPerCycle, 3, rng,
                new CaffeineIndex(new DefaultConceptBuilder(), 1024*64, false, null),
                new FrameTime().dur(1),
                exe
        ) {
            @Override
            public Deriver newDeriver() {
                return r == null ? super.newDeriver() : r;
            }
        };
        nar.termVolumeMax.set(46);


        nar.beliefConfidence(0.9f);
        nar.goalConfidence(0.9f);

        //nar.truthResolution.setValue(0.02f);


        Line1DContinuous l = new Line1DContinuous(nar, 12,
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
        l.runCycles(time);


        if (r!=null)
            r.print();


//        NAR.printActiveTasks(nar, true);
//        NAR.printActiveTasks(nar, false);

//        l.predictors.forEach(p->{
//           nar.concept(p).print();
//        });
        System.out.println("AVG SCORE=" + l.rewardSum()/ nar.time());

        nar.stop();

    }
}

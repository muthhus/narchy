package nars.perf.nars.nar.perf;

import nars.agent.NAgentOld;
import nars.experiment.misc.Line1D;
import nars.index.CaffeineIndex;
import nars.nar.Default;
import nars.nar.Executioner;
import nars.nar.MultiThreadExecutioner;
import nars.nar.util.DefaultConceptBuilder;
import nars.time.FrameClock;
import nars.util.data.random.XorShift128PlusRandom;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.RunnerException;

import static nars.experiment.misc.Line1D.random;
import static nars.experiment.tetris.Tetris.DEFAULT_INDEX_WEIGHT;
import static nars.perf.Main.perf;

/**
 * Created by me on 8/17/16.
 */
@State(Scope.Benchmark)
public class Line1DTest {

    static int cycles = 5000;

    @Param({"16", "8"})
    public int width;

    @Benchmark
    @BenchmarkMode(value = Mode.AverageTime)
    public void line1DTest() {

        //Default nar = new Default(1024, 4, 1, 3);

        XorShift128PlusRandom rng = new XorShift128PlusRandom((int)(Math.random()*1000));
        int cyclesPerFrame = 16;
        int conceptsPerCycle = cyclesPerFrame;

        final Executioner exe = new MultiThreadExecutioner(2, 4096);

        Default nar = new Default(1024,
                conceptsPerCycle, 2, 2, rng,
                new CaffeineIndex(new DefaultConceptBuilder(rng), DEFAULT_INDEX_WEIGHT, false, exe),
                new FrameClock(), exe
        );



        nar.cyclesPerFrame.setValue(cyclesPerFrame);

        //nar.beliefConfidence(0.51f);
        //nar.goalConfidence(0.51f);
        nar.DEFAULT_BELIEF_PRIORITY = 0.1f;
        nar.DEFAULT_GOAL_PRIORITY = 0.5f;
        nar.DEFAULT_QUESTION_PRIORITY = 0.3f;
        nar.DEFAULT_QUEST_PRIORITY = 0.2f;

        //new MySTMClustered(nar, 4, '.', 2);

        NAgentOld nagent = new NAgentOld(nar);

        Line1D line = new Line1D(width,
                random(50)
                //sine(30)
        );

        float score = line.run( nagent, cycles);

//        NAR.printTasks(nar, true);
//        NAR.printTasks(nar, false);
//        nar.index.forEach(t -> {
//            if (t instanceof Concept) {
//                Concept c = (Concept) t;
//                if (c.hasQuestions()) {
//                    System.out.println(c.questions().iterator().next());
//                }
//            }
//        });
//        nagent.printActions();

        System.out.println(" ------> SCORE=" + score);

        nar.stop();
    }

    public static void main(String[] args) throws RunnerException {
        perf(Line1DTest.class, 2, 1);
    }

}

package nars.experiment;

import com.google.common.collect.LinkedHashMultimap;
import nars.NAR;
import nars.experiment.misc.Line1DContinuous;
import nars.experiment.pacman.Pacman;
import nars.index.term.map.CaffeineIndex;
import nars.nar.Default;
import nars.nar.exe.Executioner;
import nars.nar.exe.SingleThreadExecutioner;
import nars.nar.util.DefaultConceptBuilder;
import nars.time.FrameClock;
import nars.util.Optimize;
import nars.util.Texts;
import nars.util.data.random.XorShift128PlusRandom;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.util.function.Supplier;

import static nars.experiment.misc.Line1DContinuous.random;
import static nars.experiment.misc.Line1DContinuous.sine;
import static nars.experiment.tetris.Tetris.DEFAULT_INDEX_WEIGHT;

/**
 * Created by me on 8/30/16.
 */
public class EvalExperiments {

    final LinkedHashMultimap<String, Float> out = LinkedHashMultimap.create();

    public EvalExperiments(Supplier<NAR> nar) {

        int cycles = 2000;

        int iterations = 3;
        for (int i = 0; i < iterations; i++) {


            for (int radius : new int[] { 1, 4}) {
                out.put("Pacman_r" + radius, new Pacman(nar.get(), 0, radius, false).run(cycles).rewardSum());
            }

            for (int width : new int[] { 4, 8}) {
                out.put("Line1DContinuous_" + width, new Line1DContinuous(nar.get(), width, random(120)).run(cycles).rewardSum());
            }
        }

    }

    public void print() {
        out.asMap().forEach((k, v) -> {
            SummaryStatistics s = new SummaryStatistics();
            for (float x : v) {
                s.addValue(x);
            }
            System.out.println(k + "\t" + Texts.n2(s.getMean()) + " +/- " + Texts.n2(s.getVariance()) );
        });
    }

    public static void main(String[] args) {

        Supplier<Default> defaultNARBuilder = () -> {
            XorShift128PlusRandom rng = new XorShift128PlusRandom((int) (Math.random() * 1000));
            int cyclesPerFrame = 4;
            int conceptsPerCycle = 4;

            final Executioner exe =
                    //new MultiThreadExecutioner(2, 2048);
                    new SingleThreadExecutioner();

            DefaultConceptBuilder cb = new DefaultConceptBuilder(rng);
            //cb.defaultCurveSampler = new CurveBag.NormalizedSampler(c, rng);

            Default nar = new Default(1024,
                    conceptsPerCycle, 2, 2, rng,
                    new CaffeineIndex(cb, DEFAULT_INDEX_WEIGHT, false, exe),
                    new FrameClock(), exe
            );

            nar.beliefConfidence(0.9f);
            nar.goalConfidence(0.8f);
            nar.DEFAULT_BELIEF_PRIORITY = 0.5f;
            nar.DEFAULT_GOAL_PRIORITY = 0.5f;
            nar.DEFAULT_QUESTION_PRIORITY = 0.1f;
            nar.DEFAULT_QUEST_PRIORITY = 0.1f;



            return nar;

        };
        Optimize.Result r = new Optimize<Default>(defaultNARBuilder)

            .tweak(0, 1f, 0.1f, "linkFeedbackRate.setValue(#x)")
            .tweak(1, 8, 2, "cyclesPerFrame.setValue(#i)")
            .tweak(1, 8, 2, "core.conceptsFiredPerCycle.setValue(#i)")
            .tweak(10, 80, 10, "compoundVolumeMax.setValue(#i)")

            .run(500 /* max evaluations */, 4 /* repeats per evaluation */, (nar) ->
                    //new Pacman(nar, 0, 2, false).runSync(100).rewardSum()
                    new Line1DContinuous(nar, 3, sine(50)).run(700).rewardSum()
            );

//        //for (CurveBag.BagCurve c : new CurveBag.BagCurve[] { CurveBag.power2BagCurve, CurveBag.power4BagCurve, CurveBag.power6BagCurve } ) {
//        for (float f : new float[] { 0, 0.1f, 0.25f, 0.5f, 0.75f, 1f } ) {
//            Supplier<NAR> sn = () -> {
//            };
//
//
//
//
//            System.out.println("Link Feedback Rate = " + f);
//            e.print();
//            System.out.println();
//        }
    }
}

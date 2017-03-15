package nars.test.agent;

import jcog.list.FasterList;
import jcog.math.RecycledSummaryStatistics;
import nars.NAR;
import nars.Narsese;
import nars.Param;
import nars.nar.Default;
import nars.task.DerivedTask;
import nars.time.Tense;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static jcog.io.SparkLine.renderFloats;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 3/4/17.
 */
public class Line1DSimplestTest {

    @Ignore @Test
    public void testScripted() throws Narsese.NarseseException {

        Param.DEBUG = true;

        float c = 0.9f;

        NAR n = new Default(1024, 16, 1, 3);
        n.termVolumeMax.setValue(8);

        int BETWEEN = 32;

        n.believe("in(L)", Tense.Present, 0f, c);
        n.believe("out(L)", Tense.Present, 0f, c);
        n.believe("happy(L)", Tense.Present, 1f, c);
        n.run(BETWEEN);

        n.believe("in(L)", Tense.Present, 1f, c);
        n.believe("out(L)", Tense.Present, 1f, c);
        n.believe("happy(L)", Tense.Present, 1f, c);
        n.run(BETWEEN);

        n.believe("in(L)", Tense.Present, 0f, c);
        n.believe("out(L)", Tense.Present, 1f, c);
        n.believe("happy(L)", Tense.Present, 0f, c);
        n.run(BETWEEN);

        n.believe("in(L)", Tense.Present, 1f, c);
        n.believe("out(L)", Tense.Present, 0f, c);
        n.believe("happy(L)", Tense.Present, 0f, c);
        n.run(BETWEEN);

        n.clear();

        n.log();
        n.onTask(t -> {

            if (t instanceof DerivedTask) {


                if (t.isGoal() && t.term().toString().contains("out(L)")) {
                    System.err.println(t.proof());
                }
            }

            //System.out.println(t);
        });

        n.input("$0.99$ happy(L)! %1.0;0.99%");
        n.input("$0.99$ out(L)@ :|:");


        n.run(100);

    }

    @Test
    public void testSimple1() throws Narsese.NarseseException {

        NAR n = new Default();


        Line1DSimplest a = new Line1DSimplest(n);

        a.init();

        //n.log();
        a.trace = true;

        System.out.println("START initializing at target..\n");
        a.current = 0; a.target = 0;

        n.run(1);

        assertEquals(1f, a.rewardValue, 0.01f);

        n.run(1);

        assertEquals( 0.81f, n.emotion.happy(), 0.05f);
        assertEquals( 0.0, n.emotion.sad(), 0.01f);

        System.out.println("moving target away from reward..\n");
        a.target = 1;
        n.run(1);

        assertEquals(-1f, a.rewardValue, 0.01f);
        assertEquals( 0.0f, n.emotion.happy(), 0.1f);
        assertEquals( 0.81f, n.emotion.sad(), 0.4f); //this will be weakened by what caused the happiness in the previous cycle due to evidence decay's lingering effect

        assertEquals(0f, a.rewardSum(), 0.01f);

        System.out.println("AVG SCORE=" + a.rewardSum() / n.time());

    }

    @Test
    public void testSimplePerformance() throws Narsese.NarseseException {

        Param.ANSWER_REPORTING = false;

        Default n = new Default() {
//            @Override
//            public Deriver newDeriver() {
//                return Deriver.get("induction.nal");
//            }
        };
        n.core.conceptsFiredPerCycle.setValue(16);

        n.truthResolution.setValue(0.01f);
        n.termVolumeMax.setValue(16);

        Line1DSimplest a = new Line1DSimplest(n);

        a.init();

        List<Float> rewards = new ArrayList(8*1024);
        List<Float> motv = new ArrayList(8*1024);

        n.onCycle(()->{
            rewards.add(a.rewardValue);
            motv.add(a.dexterity());
        });

        //n.log();
        //a.trace = true;

        int trainTime = 8;

        a.current = 0; a.target = 0; n.run(trainTime);
        a.current = 0; a.target = 1; n.run(trainTime);
        a.current = 1; a.target = 0; n.run(trainTime);
        a.current = 1; a.target = 1; n.run(trainTime);


        final int changePeriod = trainTime;

        int time = 100;

        //n.log();
        for (int i = 0; i < time; i++) {
            if (i % changePeriod == 0)
                a.target = n.random.nextBoolean() ?  1f : 0f;
            n.run(1);
        }

        System.out.println( "rwrd: " +  renderFloats(downSample(rewards, 4)) );
        System.out.println( "motv: " + renderFloats(downSample(motv, 4)) );
        float avgReward = a.rewardSum() / n.time();
        System.out.println("avg reward = " + avgReward);

        assertTrue(avgReward > 0.5f); //75% accuracy

    }

    private static List<Float> downSample(List<Float> f, int divisor) {
        if (divisor == 1)
            return f;

        List<Float> l = new FasterList<>((int)Math.ceil(((float)f.size())/divisor));
        for (int i = 0; i < f.size(); ) {
            float total = 0;
            int j;
            for (j = 0; j < divisor && i < f.size(); j++ ) {
                total += f.get(i++);
            }
            l.add(total/j);
        }
        return l;
    }


    /** tests with an explicit rule provided that will help it succeed */
    @Ignore
    @Test public void testSimpleCheat() throws Narsese.NarseseException {


        NAR n = new Default(1024, 64, 1, 3);

        final int changePeriod = 16;

        n.time.dur(4);

        n.termVolumeMax.setValue(13);

        //n.log();

        Line1DSimplest a = new Line1DSimplest(n);

        Param.DEBUG = true;

        //n.derivedEvidenceGain.setValue(0f);

        //a.trace = true;
        a.init();
        a.current = 0;
        a.target = 0;
        a.curiosity.setValue(0.05);
        a.predictorProbability = 0.5f;


//        //n.log();
//        n.onCycle(()->{
//            System.err.println(a.current + " --->? " + a.target);
//        });
        n.onTask(t -> {

            if (t instanceof DerivedTask) {


                if (t.isGoal() && t.term().toString().contains("out(L)")) {
                    System.err.println(t.proof());
                }
            }

            //System.out.println(t);
        });

//        n.input("(L(in) <=> L(out)). %1.0;0.99%");
//        n.input("$0.99$ (L(in) &&+0 L(out))! %1.0;0.99%");
//        n.input("$0.99$ (--L(in) &&+0 --L(out))! %1.0;0.99%");
//        n.input("$0.99$ (L(in) ==>+0 L(out)). %1.0;0.99%");
//        n.input("$0.99$ (--L(in) ==>+0 --L(out)). %1.0;0.99%");



        for (int c = 0; c < 128; c++) {

            List<Float> hapy = new ArrayList(1*1024);
            List<Float> motv = new ArrayList(1*1024);
            List<Float> in = new ArrayList(1*1024);
            List<Float> out = new ArrayList(1*1024);

            int time = 128;

            int j = 0;
            for (int i = 0; i < time; i++) {

                if ((i + 1) % changePeriod == 0) {
                    a.target = (j++) % 2 == 0 ? 1f : 0f;
                    //n.goal(a.out.term(), Tense.Present, a.target, 0.9f);
                }
//                if (j > 5) {
//                    a.curiosity.setValue(0f);
//                }
                //                //a.goal()
                //            }

                n.run(1);

                in.add(a.in.asFloat());
                float o = (float) a.out.feedback.getAsDouble();
                if (o != o)
                    o = 0.5f;
                out.add(o);
                hapy.add(a.rewardValue);
                motv.add(a.dexterity());
            }

            int ds = 4;
            System.out.println("  in:\t" + renderFloats(downSample(in, ds)));
            System.out.println(" out:\t" + renderFloats(downSample(out, ds)));
            System.out.println("hapy:\t" + renderFloats(downSample(hapy, ds)));
            System.out.println("motv:\t" + renderFloats(downSample(motv, ds)));

            System.out.println("AVG SCORE=" + a.rewardSum() / n.time());

            RecycledSummaryStatistics motvStat = new RecycledSummaryStatistics();
            for (Float x : motv)
                motvStat.accept(x);
            System.out.println(motvStat);
        }
    }


}
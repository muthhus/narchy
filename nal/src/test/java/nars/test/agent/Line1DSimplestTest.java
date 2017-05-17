package nars.test.agent;

import jcog.list.FasterList;
import nars.NAR;
import nars.Narsese;
import nars.Param;
import nars.nar.Default;
import nars.op.Operator;
import nars.term.var.Variable;
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

        NAR n = new Default(1024);
        n.termVolumeMax.setValue(8);

        int BETWEEN = 32;

        n.believe("(i)", Tense.Present, 0f, c);
        n.goal("(o)", Tense.Present, 0f, c);
        n.believe("(o)", Tense.Present, 0f, c);
        n.believe("(happy)", Tense.Present, 1f, c);
        n.run(BETWEEN);

        n.believe("(i)", Tense.Present, 1f, c);
        n.goal("(o)", Tense.Present, 1f, c);
        n.believe("(o)", Tense.Present, 1f, c);
        n.believe("(happy)", Tense.Present, 1f, c);
        n.run(BETWEEN);

        n.believe("(i)", Tense.Present, 0f, c);
        n.goal("(o)", Tense.Present, 1f, c);
        n.believe("(o)", Tense.Present, 1f, c);
        n.believe("(happy)", Tense.Present, 0f, c);
        n.run(BETWEEN);

        n.believe("(i)", Tense.Present, 1f, c);
        n.goal("(o)", Tense.Present, 0f, c);
        n.believe("(o)", Tense.Present, 0f, c);
        n.believe("(happy)", Tense.Present, 0f, c);
        n.run(BETWEEN);


//        n.onTask(t -> {
//
//            if (t instanceof DerivedTask) {
//
//
//                if (t.isGoal()) {
//                    System.err.println(t.proof());
//                }
//            }
//
//            //System.out.println(t);
//        });

//        n.input("$0.99$ happy(L)! %1.0;0.99%");
//        n.input("$0.99$ out(L)@ :|:");
        //n.log();

//        n.input("$0.99$ ((i) ==>+0 (o))?");
//        n.input("$0.99$ (--(i) ==>+0 (o))?");
//        n.input("$0.99$ ((o) ==>+0 (i))?");
//        n.input("$0.99$ (--(o) ==>+0 (i))?");

        n.input("$0.99$ (?x ==>+0 (happy))?");

        n.run(500);

    }

    @Test
    public void testSimple1() throws Narsese.NarseseException {

        NAR n = new Default();


        Line1DSimplest a = new Line1DSimplest(n);

        a.init();

        //n.log();
        a.trace = true;

        System.out.println("START initializing at target..\n");
        a.o.setValue(0);
        a.i.setValue(0);

        n.run(1);

        assertEquals(1f, a.reward, 0.01f);

        n.run(1);

        assertEquals( 0.81f, n.emotion.happy(), 0.05f);
        assertEquals( 0.0, n.emotion.sad(), 0.01f);

        System.out.println("moving target away from reward..\n");
        a.i.setValue(1f);
        n.run(1);

        assertEquals(-1f, a.reward, 0.01f);
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

        n.truthResolution.setValue(0.01f);
        n.termVolumeMax.setValue(16);

        Line1DSimplest a = new Line1DSimplest(n);

        a.init();

        List<Float> rewards = new ArrayList(8*1024);
        List<Float> motv = new ArrayList(8*1024);

        n.onCycle(()->{
            rewards.add(a.reward);
            motv.add(a.dexterity());
        });

        //n.log();
        //a.trace = true;

        int trainTime = 8;


        final int changePeriod = trainTime;

        int time = 100;

        //n.log();
        for (int i = 0; i < time; i++) {
            if (i % changePeriod == 0)
                a.i.setValue( n.random().nextBoolean() ?  1f : 0f );
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


        Default n = new Default(1024);



        final int changePeriod = 8;

        n.time.dur(2);

        n.termVolumeMax.setValue(16);

        //n.log();

        Line1DSimplest a = new Line1DSimplest(n);

        Param.DEBUG = true;

        //n.derivedEvidenceGain.setValue(0f);

        //a.trace = true;
        a.init();
        a.curiosityConf.setValue(0.25);
        //a.predictorProbability = 1.0f;


//        //n.log();
//        n.onCycle(()->{
//            System.err.println(a.current + " --->? " + a.target);
//        });

//        n.onTask(t -> {
//
//            if (t instanceof DerivedTask) {
//
//
//                if (t.isGoal() && t.term().toString().contains("(o)")) {
//                    System.err.println(t.proof());
//                }
//            }
//
//            //System.out.println(t);
//        });

        n.input("$0.99$ (?x ==>+0 (happy))?");

//        n.input("((i) <=> (o)). %1.0;0.99%");
//        n.input("$0.99$ ((i) &&+0 (o))! %1.0;0.99%");
//        n.input("$0.99$ (--(i) &&+0 --(o))! %1.0;0.99%");
//        n.input("$0.99$ ((i) ==>+0 (o)). %1.0;0.99%");
//        n.input("$0.99$ (--(i) ==>+0 --(o)). %1.0;0.99%");



        for (int c = 0; c < 128; c++) {

            List<Float> hapy = new ArrayList(1*1024);
            List<Float> motv = new ArrayList(1*1024);
            List<Float> in = new ArrayList(1*1024);
            List<Float> out = new ArrayList(1*1024);

            int time = 128;

            int j = 0;
            for (int i = 0; i < time; i++) {

                if ((i + 1) % changePeriod == 0) {
                    a.i.setValue( (j++) % 2 == 0 ? 1f : 0f );
                    //n.goal(a.out.term(), Tense.Present, a.target, 0.9f);
                }
//                if (j > 5) {
//                    a.curiosity.setValue(0f);
//                }
                //                //a.goal()
                //            }

                n.run(1);

                //in.add(a.in.asFloat());
                hapy.add(a.reward);
                motv.add(a.dexterity());
            }

            int ds = 1;
            System.out.println("  in:\t" + renderFloats(downSample(in, ds)));
            System.out.println(" out:\t" + renderFloats(downSample(out, ds)));
            System.out.println("hapy:\t" + renderFloats(downSample(hapy, ds)));
            System.out.println("motv:\t" + renderFloats(downSample(motv, ds)));

            System.out.println("\tavg rwrd=" + a.rewardSum() / time);
            a.rewardSum = 0; //reset

//            RecycledSummaryStatistics motvStat = new RecycledSummaryStatistics();
//            for (Float x : motv)
//                motvStat.accept(x);
//            System.out.println(motvStat);
        }

//        TreeSet<Task> impl = new TreeSet(Truth.compareConfidence);
//
//        n.forEachTask(t -> {
//           if (t.isBelief() && t.op()==IMPL) {
//               impl.add(t);
//           }
//        });
//        impl.forEach(System.out::println);

    }

    @Test public void testSequenceLearning() throws Narsese.NarseseException {
        Param.DEBUG = true;

        Default n = new Default(1024);
        n.on("say", (Operator) (op, args, nar) -> {
            if (!(args[0] instanceof Variable))
                try {
                    n.inputAt(Math.round(nar.time()+nar.dur()), "say(" +  args[0] + "). :|:");
                } catch (Narsese.NarseseException e) {
                    e.printStackTrace();
                }
        });

        n.log();
        n.input("(say(a) &&+10 say(b))! :|:");

        n.run(100);
    }

}
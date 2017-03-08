package nars.test.agent;

import jcog.io.SparkLine;
import nars.NAR;
import nars.Narsese;
import nars.Param;
import nars.nar.Default;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 3/4/17.
 */
public class Line1DSimplestTest {

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
        assertEquals( 0.81f, n.emotion.happy(), 0.01f);
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

        System.out.println( "rwrd: " +  SparkLine.renderFloats(downSample(rewards, 4)) );
        System.out.println( "motv: " + SparkLine.renderFloats(downSample(motv, 4)) );
        float avgReward = a.rewardSum() / n.time();
        System.out.println("avg reward = " + avgReward);

        assertTrue(avgReward > 0.5f); //75% accuracy

    }

    private static List<Float> downSample(List<Float> motv, int divisor) {
        List<Float> l = new ArrayList((int)Math.ceil(((float)motv.size())/divisor));
        for (int i = 0; i < motv.size(); ) {
            float total = 0;
            int j;
            for (j = 0; j < divisor && i < motv.size(); j++ ) {
                total += motv.get(i++);
            }
            l.add(total/j);
        }
        return l;
    }

}
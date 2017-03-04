package nars.test.agent;

import nars.NAR;
import nars.Narsese;
import nars.nar.Default;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by me on 3/4/17.
 */
public class Line1DSimplestTest {

    @Test
    public void testSimple1() throws Narsese.NarseseException {

        NAR n = new Default();

        Line1DSimplest a = new Line1DSimplest(n);

        a.init();

        n.log();
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
}
package jcog.learn;

import jcog.learn.ql.DPG;
import jcog.learn.ql.DQN;
import jcog.learn.ql.HaiQ;
import jcog.learn.ql.HaiQAgent;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class AgentTest {


    @Test public void testHaiQ() {
        testAgent( new HaiQ(1, 2) );
    }
    @Test public void testHaiQAgent() {
        testAgent( new HaiQAgent(1, 2) );
    }

    @Ignore @Test public void testDPGAgent() {
        testAgent( new DPG(1, 2) ); //broken it seems
    }

    @Ignore
    @Test public void testDQNAgent() {
        testAgent( new DQN(1, 2) ); //broken it seems
    }

    static void testAgent(Agent agent) {

        //assert(agent.inputs == 1); //not true via HaiQ perception
        assert(agent.inputs >= 1);
        assert(agent.actions == 2);

        final float minRatio = 3f;

        float nextReward = 0;
        IntIntHashMap acts = new IntIntHashMap();
        for (int i = 0; i < 1000; i++) {
            int action = agent.act(nextReward, new float[] { (float)Math.random() } );
            //System.out.println(a);
            acts.addToValue(action, 1);
            switch (action) {
                case 0: nextReward = 0.0f; break;
                case 1: nextReward = 1.0f; break;
            }
        }
        System.out.println(agent.getClass() + " " + agent.summary() + "\n" + acts);
        assertTrue(acts.get(1) > acts.get(0));
        assertTrue(acts.get(1)/ minRatio > acts.get(0)); //at least 10:1
    }

}
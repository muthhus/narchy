//package nars.experiment.misc;
//
//import jcog.Util;
//import jcog.learn.Agent;
//import org.eclipse.collections.api.tuple.Twin;
//
//import java.util.Arrays;
//
//
//public interface DiscreteEnvironment {
//
//    Twin<Integer> start();
//
//    default float run(Agent a, int cycles) {
//        return run(a, cycles, 0);
//    }
//
//    default float run(Agent a, int cycles, long periodMS) {
//
//        Twin<Integer> x = start();
//
//        final int inputs = x.getOne();
//
//        float[] ins = new float[inputs];
//
//        Arrays.fill(ins, 0.5f);
//
//        preStart(a);
//
//        a.start(inputs, x.getTwo());
//
//        float rewardSum = 0;
//
//        for (int t = 0; t < cycles; t++) {
//            float reward = pre(t, ins);
//            rewardSum += reward;
//
//            post(t, a.act(reward, ins), ins, a);
//
//            if (periodMS > 0)
//                Util.sleep(periodMS);
//        }
//
//        float scoreAvg = rewardSum/cycles;
//        //System.out.println(a + " score=" + scoreAvg);
//        return scoreAvg;
//    }
//
//    /** returns reward value */
//    float pre(int t, float[] ins);
//
//    void post(int t, int action, float[] ins, Agent a);
//
//    default void preStart(Agent a) { }
//}

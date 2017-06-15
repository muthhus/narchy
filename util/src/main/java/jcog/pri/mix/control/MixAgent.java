package jcog.pri.mix.control;

import jcog.tensor.ArrayTensor;
import jcog.tensor.Tensor;

public interface MixAgent {
    void act(Tensor in, float score, ArrayTensor out);

    //agent = new HaiQAgent(numInputs, size*4, outs * 2);
    //agent.setQ(0.05f, 0.5f, 0.9f); // 0.1 0.5 0.9

    //agent = CMAESAgent.build(numInputs, size /* level for each */ );


//floatToDoubleArray(
    //                if (agent.outs!=null) {
//            agentOut.set(agent.outs);
////            float[] ll = levels.data;
////             //bipolarize
////            for (int i = 0, dataLength = ll.length; i < dataLength; i++) {
////                ll[i] = (ll[i] - 0.5f) * 2f;
////            }
//        }

}

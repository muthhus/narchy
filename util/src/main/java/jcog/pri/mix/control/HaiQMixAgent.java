package jcog.pri.mix.control;

import jcog.Util;
import jcog.learn.ql.HaiQAgent;
import jcog.tensor.ArrayTensor;

public class HaiQMixAgent implements MixAgent {

    HaiQAgent agent;
    float delta = 0.25f;

    @Override
    public void act(ArrayTensor in, float score, ArrayTensor out) {
        if (agent == null) {
            //TODO wrap input with chain that includes feedback from previous
            /*   new TensorChain(
                                //agentOut
            */
            agent = new HaiQAgent(in.volume(), in.volume() / 2, out.volume() * 2);
        }

        int action = agent.act(score, in.get());
        if (action == -1)
            return; //error

        int which = action / 2;
        out.set(Util.clamp(out.get(which) + ((action % 2 == 0) ? (+1) : (-1)) * delta, -1, +1), which);

    }
}

package jcog.pri.mix.control;

import jcog.Util;
import jcog.learn.Autoencoder;
import jcog.learn.ql.HaiQAgent;
import jcog.tensor.ArrayTensor;

public class MultiHaiQMixAgent implements MixAgent {

    HaiQAgent[] agent;
    final int controlResolution = 5;
    final float controlGranularity = 2.0f / (controlResolution-1);

    /** shared QL input state */
    private int sharedInputState = -1;
    public Autoencoder sharedPerception = null;

    @Override
    public void act(ArrayTensor in, float score, ArrayTensor out) {
        int o = out.volume();
        if (agent == null) {
            init(in, o);
        }

        float[] ii = in.get();
        for (int a = 0; a < o; a++) {
            int action = agent[a].act(score, ii);
            if (action == -1)
                return; //error
            out.set(gain(action), a);
        }

    }

    private float gain(int action) {
        return -1f + (action * controlGranularity);
    }

    protected void init(ArrayTensor in, int o) {
        agent = new HaiQAgent[o];
        for (int oo = 0; oo < o; oo++) {
            int ooo = oo;
            agent[oo] = new HaiQAgent(in.volume(), in.volume() / 4, controlResolution) {

                @Override
                protected int perceive(float[] input) {
                    if (ooo == 0) {
                        return sharedInputState = super.perceive(input);
                    } else {
                        return sharedInputState;
                    }
                }

                /** use shared autoencoder so only instantiate one in the first agent */
                @Override protected Autoencoder perception(int inputs, int states) {
                    if (ooo == 0)
                        return sharedPerception = super.perception(inputs, states);
                    else
                        return null;
                }
            };
        }
    }
}
package jcog.pri.mix.control;

import jcog.Util;
import jcog.learn.Autoencoder;
import jcog.learn.ql.HaiQAgent;
import jcog.tensor.ArrayTensor;
import jcog.tensor.Tensor;

public class MultiHaiQMixAgent implements MixAgent {

    public HaiQAgent[] agent;


    final boolean controlRelative = false;

    final int controlResolution = 5;
    final float controlGranularity = 1.0f / (controlResolution);

    /**
     * shared QL input state
     */
    private int sharedInputState = -1;
    public Autoencoder sharedPerception = null;
    private float sharedPerceptionError;

    @Override
    public void act(Tensor in, float score, ArrayTensor out) {
        int o = out.volume();
        if (agent == null) {
            init(in, o);
        }

        float[] ii = in.get();

        for (int a = 0; a < o; a++) {
            int action = agent[a].act(score, ii);

            if (action == -1)
                return; //error

            float next;
            if (controlRelative) {
                next = out.get(a);
                switch (action) {
                    case 0: //no change
                        break;
                    case 1:
                        next += controlGranularity;
                        break;
                    case 2:
                        next -= controlGranularity;
                        break;
                }
            } else {
                //absolute control
                next = (action * controlGranularity);
            }

            out.set(Util.clamp(next, 0f, +1f), a);
        }

    }

    protected void init(Tensor in, int o) {
        agent = new HaiQAgent[o];
        for (int oo = 0; oo < o; oo++) {
            int ooo = oo;
            agent[oo] = new HaiQAgent(in.volume(), in.volume() / 4,
                    controlRelative ? 3 : controlResolution + 1) {

                @Override
                protected int perceive(float[] input) {
                    if (ooo == 0) {
                        return sharedInputState = super.perceive(input);
                    } else {
                        return sharedInputState;
                    }
                }

                /** use shared autoencoder so only instantiate one in the first agent */
                @Override
                protected Autoencoder perception(int inputs, int states) {
                    if (ooo == 0)
                        return sharedPerception = super.perception(inputs, states);
                    else
                        return null;
                }

                @Override
                public int act(float reward, float[] input) {
                    if (ooo == 0)
                        sharedPerceptionError = perceptionError.asFloat();
                    return super.act(reward, input, sharedPerceptionError);
                }
            };
        }
    }
}
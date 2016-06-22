package nars.learn;

import nars.util.Util;

/**
 * lowest common denominator reinforcement learning agent interface
 */
public interface Agent {

    void start(int inputs, int actions);

    int act(float reward, float[] nextObservation);

    default int act(double reward, double[] nextObservation) {
        float[] f = Util.toFloat(nextObservation);

        return act((float)reward, f);
    }

    default String summary() {
        return "";
    }
}

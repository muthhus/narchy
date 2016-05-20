package nars.util;

/**
 * lowest common denominator reinforcement learning agent interface
 */
public interface Agent {

    void start(int inputs, int actions);

    int act(float reward, float[] nextObservation);

    default String summary() {
        return "";
    }
}

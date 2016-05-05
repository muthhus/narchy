package nars.util;

/**
 * lowest common denominator reinforcement learning agent interface
 */
public interface Agent {

    public void start(int inputs, int actions);

    public int act(float reward, float[] nextObservation);

    default String summary() {
        return "";
    }
}

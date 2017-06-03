package jcog.learn;

import jcog.math.TensorF;

import java.util.function.Supplier;

/**
 * lowest common denominator reinforcement learning agent interface
 */
public interface Agent {

    void start(int inputs, int actions);

    //default int act(float reward, TensorF input) {
        //TODO
    //}

    int act(float reward, float[] nextObservation);

//    default int act(double reward, double... nextObservation) {
//        float[] f = Util.toFloat(nextObservation);
//
//        return act((float)reward, f);
//    }

    default String summary() {
        return "";
    }
}

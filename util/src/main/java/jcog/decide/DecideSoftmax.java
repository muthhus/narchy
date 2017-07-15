package jcog.decide;

import jcog.Util;

import java.util.Random;

/**
 *  https://en.wikipedia.org/wiki/Softmax_function
 *  For high temperatures ( {\displaystyle \tau \to \infty } \tau \to \infty ),
 *  all actions have nearly the same probability and the lower the temperature,
 *  the more expected rewards affect the probability.
 *
 *  For a low temperature ( {\displaystyle \tau \to 0^{+}} \tau \to 0^{+}),
 *  the probability of the action with the highest expected reward tends to 1.
 */
public class DecideSoftmax implements Deciding {

    private final float minTemperature;
    private final float temperatureDecay;
    private final Random random;
    /**
     * normalized motivation
     */
    private float[] mot, motProb;


    /** whether to exclude negative values */
    boolean onlyPositive;
    boolean normalize;

    float temperature;
    private float decisiveness;

    public DecideSoftmax(float constantTemp, Random random) {
        this(constantTemp, constantTemp, 1f, random);
    }

    public DecideSoftmax(float initialTemperature, float minTemperature, float decay, Random random) {
        this.temperature = initialTemperature;
        this.minTemperature = minTemperature;
        this.temperatureDecay = decay;
        this.random = random;
    }

    @Override
    public int decide(float[] motivation, int lastAction) {

        temperature = Math.max(minTemperature,temperature * temperatureDecay);

        int actions = motivation.length;
        if (mot == null) {
            mot = new float[actions];
            motProb = new float[actions];
        }

        //TODO generalize to a function which can select ranges or distort values via curves
        if (onlyPositive) {
            for (int i = 0; i < motivation.length; i++)
                motivation[i] = Math.max(0, motivation[i]);
        }

        float sumMotivation = Util.sum(motivation);
        if (sumMotivation < Float.MIN_VALUE) {
            decisiveness = 0;
            return random.nextInt(motivation.length);
        }

        if (normalize) {
            float[] minmax = Util.minmax(motivation);
            float min = minmax[0];
            float max = minmax[1];
            for (int i = 0; i < actions; i++) {
                mot[i] = Util.normalize(motivation[i], min, max);
            }
        } else {
            System.arraycopy(motivation, 0, mot, 0, actions);
        }

        /* http://www.cse.unsw.edu.au/~cs9417ml/RL1/source/RLearner.java */
        float sumProb = 0;
        for (int i = 0; i < actions; i++) {
            sumProb += (motProb[i]  = (float) Math.exp(mot[i] / temperature));
        }

        float r = random.nextFloat() * sumProb;

        int i;
        for (i = actions - 1; i >= 1; i--) {
            float m = motProb[i];
            r -= m;
            if (r <= 0) {
                break;
            }
        }

        decisiveness = motivation[i] / sumMotivation;
        //System.out.println("decisiveness: " + decisiveness );

        return i;
    }

    public float decisiveness() {
        return decisiveness;
    }
}

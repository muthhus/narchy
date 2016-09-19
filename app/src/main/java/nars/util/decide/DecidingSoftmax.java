package nars.util.decide;

import nars.Param;
import nars.util.Util;

import java.util.Random;

public class DecidingSoftmax implements Deciding {

    private final float minTemperature;
    private final float temperatureDecay;
    /**
     * normalized motivation
     */
    private float[] mot, motProb;


    /** whether to exclude negative values */
    boolean onlyPositive;
    boolean normalize;

    float temperature;
    private float decisiveness;

    public DecidingSoftmax(float initialTemperature, float minTemperature, float decay) {
        this.temperature = initialTemperature;
        this.minTemperature = minTemperature;
        this.temperatureDecay = decay;
    }

    @Override
    public int decide(float[] motivation, int lastAction, Random random) {

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
        if (sumMotivation < Param.TRUTH_EPSILON) {
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
            for (int i = 0; i < actions; i++) {
                mot[i] = motivation[i]; //use the value directly
            }
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

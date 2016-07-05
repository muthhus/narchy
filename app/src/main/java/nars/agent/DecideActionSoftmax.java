package nars.agent;

import nars.util.Util;

import java.util.Random;

/**
 * Created by me on 6/9/16.
 */
public class DecideActionSoftmax implements DecideAction {

    /**
     * normalized motivation
     */
    private float[] motNorm, motProb;


    /** whether to exclude negative values */
    boolean onlyPositive = false;

    float temperature;

    public DecideActionSoftmax(float temperature) {
        this.temperature = temperature;
    }

    @Override
    public int decideAction(float[] motivation, int lastAction, Random random) {
        int actions = motivation.length;
        if (motNorm == null) {
            motNorm = new float[actions];
            motProb = new float[actions];
        }

        if (onlyPositive) {
            for (int i = 0; i < motivation.length; i++)
                motivation[i] = Math.max(0, motivation[i]);
        }

        float[] minmax = Util.minmax(motivation);
        float min = minmax[0];
        float max = minmax[1];
        for (int i = 0; i < actions; i++) {
            motNorm[i] = Util.normalize(motivation[i], min, max);
        }

        /* http://www.cse.unsw.edu.au/~cs9417ml/RL1/source/RLearner.java */
        float sumProb = 0;
        for (int i = 0; i < actions; i++) {
            float m;
            motProb[i] = m = (float) Math.exp(motNorm[i] / temperature);
            sumProb += m;
        }


        float r = random.nextFloat() * sumProb;

        for (int i = actions - 1; i >= 1; i--) {
            float m = motProb[i];
            r -= m;
            if (r <= 0)
                return i;
        }

        return 0;
    }
}

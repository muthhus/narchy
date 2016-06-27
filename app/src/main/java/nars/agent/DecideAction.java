package nars.agent;

import java.util.Random;

/**
 * Created by me on 6/9/16.
 */
public interface DecideAction {
    int decideAction(float[] motivation, int lastAction, Random random);
}

package nars.util.experiment;

import com.gs.collections.api.tuple.Twin;
import nars.util.Agent;
import nars.util.data.Util;
import org.apache.commons.lang3.mutable.MutableFloat;

import java.util.Arrays;

import static java.lang.System.out;

/**
 * Created by me on 5/5/16.
 */
public interface Environment {

    Twin<Integer> start();

    default float run(Agent a, int cycles) {

        Twin<Integer> x = start();

        final int inputs = x.getOne();

        float[] ins = new float[inputs];

        Arrays.fill(ins, 0.5f);

        a.start(inputs, x.getTwo());

        float reward = 0, rewardSum = 0;

        for (int t = 0; t < cycles; t++) {
            reward = cycle(t, a.act(reward, ins), ins, a);
            rewardSum += reward;
        }

        float scoreAvg = rewardSum/cycles;
        System.out.println(a + " score=" + scoreAvg);
        return scoreAvg;
    }

    /** returns reward value */
    float cycle(int t, int action, float[] ins, Agent a);

}

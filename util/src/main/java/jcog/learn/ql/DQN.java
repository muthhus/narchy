package jcog.learn.ql;

import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.eclipse.collections.impl.bag.sorted.mutable.TreeBag;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;

import static java.lang.System.out;

/**
 * Created by me on 5/3/16.
 */
public class DQN extends ReinforceJSAgent {

    @Deprecated private final float alpha = 0.05f;
    @Deprecated private final float epsilon = 0.01f;

    public DQN(int inputs, int actions) {
        super(inputs, actions);
    }

    @Override
    String getAgentInitCode(int inputs, int actions) {
        int hiddens = 4 * inputs * actions; //heuristic
        String spec = "{ alpha: " + alpha + ", epsilon: " + epsilon + ", num_hidden_units: " + hiddens + " }";
        return "var agent = new RL.DQNAgent(env, " + spec + "); ";
    }



}

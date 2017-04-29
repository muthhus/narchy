package jcog.learn.ql;

import static java.lang.System.out;

/**
 * Created by me on 5/3/16.
 */
public class DQN extends ReinforceJSAgent {

    @Deprecated private final float alpha = 0.05f;
    @Deprecated private final float epsilon = 0.01f;

    public DQN(int inputs, int actions) {
        start(inputs, actions);
    }

    @Override
    String getAgentInitCode(int inputs, int actions) {
        int hiddens = 4 * inputs * actions; //heuristic
        String spec = "{ alpha: " + alpha + ", epsilon: " + epsilon + ", num_hidden_units: " + hiddens + " }";
        return "var agent = new RL.DQNAgent(env, " + spec + "); ";
    }


    public static void main(String[] args) throws Exception {
        DQN d = new DQN(2, 3);
        out.println( d.act(0, 0.5f, 0.5f) );
        out.println( d.act(-0.2f, 0.5f, 0.7f) );
        out.println( d.act(0.2f, 0.1f, 0.7f) );
    }
}

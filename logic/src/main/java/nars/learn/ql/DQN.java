package nars.learn.ql;

import static java.lang.System.out;

/**
 * Created by me on 5/3/16.
 */
public class DQN extends ReinforceJSAgent {

    @Deprecated private final float alpha = 0.03f;

    public DQN() {

    }

    public DQN(int inputs, int actions) {
        start(inputs, actions);
    }

    @Override
    String getAgentInitCode(int inputs, int actions) {
        int hiddens = 3 * inputs * actions; //heuristic
        return "var spec =  { alpha: + " + alpha + ", num_hidden_units: " + hiddens + " }; " +
               "var agent = new RL.DQNAgent(env, spec); ";
    }


    public static void main(String[] args) throws Exception {
        DQN d = new DQN(2, 3);
        out.println( d.act(0, 0.5f, 0.5f) );
        out.println( d.act(-0.2f, 0.5f, 0.7f) );
        out.println( d.act(0.2f, 0.1f, 0.7f) );
    }
}

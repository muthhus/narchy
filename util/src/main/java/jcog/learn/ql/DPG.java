package jcog.learn.ql;

import static java.lang.System.out;

/**
 * Deterministic Policy Gradient
 */
public class DPG extends ReinforceJSAgent {

    @Deprecated private final float alpha = 0.03f;

    public DPG() {

    }

    public DPG(int inputs, int actions) {
        start(inputs, actions);
    }

    @Override
    String getAgentInitCode(int inputs, int actions) {
        int hiddens = 3 * inputs * actions; //heuristic
        return "var spec =  { alpha: + " + alpha + ", num_hidden_units: " + hiddens + " }; " +
                "var agent = new RL.DeterministPG(env, spec); ";
    }


    public static void main(String[] args) throws Exception {
        DQN d = new DQN(2, 3);
        out.println( d.act(0, 0.5f, 0.5f) );
        out.println( d.act(-0.2f, 0.5f, 0.7f) );
        out.println( d.act(0.2f, 0.1f, 0.7f) );
    }
}

package nars.learn.ql;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import nars.learn.Agent;

import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngineManager;
import java.io.FileReader;

import static java.lang.System.out;

/**
 * Created by me on 5/3/16.
 */
public class DQN implements Agent {

    private Invocable js;

    public DQN() {

    }

    public DQN(int inputs, int actions) {
        start(inputs, actions);
    }

    @Override public void start(int inputs, int actions)  {
        try {
            NashornScriptEngine engine = (NashornScriptEngine) new ScriptEngineManager().getEngineByName("nashorn");
            engine.eval(new FileReader("/home/me/opennars/app/src/main/java/nars/util/rl.js"));


            float alpha = 0.03f;
            int hiddens = 3 * inputs * actions; //heuristic

            CompiledScript cscript = engine.compile(
                    "java.lang.System.out.println('creating new rl.js agent'); " +
                    "var Math = Java.type('java.lang.Math'); " +
                    "var env = { getNumStates: function() { return " + inputs + "; }, getMaxNumActions: function() { return " + actions + "; } }; " +

                    /*
                    http://cs.stanford.edu/people/karpathy/reinforcejs/index.html
                    spec.gamma = 0.9; // discount factor, [0, 1)
                    spec.epsilon = 0.2; // initial epsilon for epsilon-greedy policy, [0, 1)
                    spec.alpha = 0.005; // value function learning rate
                    spec.experience_add_every = 5; // number of time steps before we add another experience to replay memory
                    spec.experience_size = 10000; // size of experience
                    spec.learning_steps_per_iteration = 5;
                    spec.tderror_clamp = 1.0; // for robustness
                    spec.num_hidden_units = 100 // number of neurons in hidden layer
                    */
                    "var spec =  { alpha: + " + alpha + ", num_hidden_units: " + hiddens + " }; " +
                    "var agent = new RL.DQNAgent(env, spec); " +
                    "function act(i,r) { var a = agent.act(i); agent.learn(r); return a;  } ");

            //Bindings bindings = cscript.getEngine().createBindings();
            //        for(Map.Entry me : bindings.entrySet()) {
//            System.out.printf("%s: %s\n",me.getKey(),String.valueOf(me.getValue()));
//        }
//        //cscript.eval();
            cscript.eval(engine.getBindings(ScriptContext.ENGINE_SCOPE));
            js = (Invocable) cscript.getEngine();

        }catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }



        /*
        var env = {};
        env.getNumStates = function() { return 8; }
        env.getMaxNumActions = function() { return 4; }

        // create the DQN agent
        var spec = { alpha: 0.01 } // see full options on DQN page
        agent = new RL.DQNAgent(env, spec);

        setInterval(function(){ // start the learning loop
          var action = agent.act(s); // s is an array of length 8
          //... execute action in environment and get the reward
          agent.learn(reward); // the agent improves its Q,policy,model, etc. reward is a float
        }, 0);
         */

        //System.out.println(agent + " " + agent.getClass());



//        System.out.println(a);
//        Object b = js.invokeFunction("act", new double[] { 0,0,0,0,0,1,0,1 }, 0.14);
//        System.out.println(b);

    }

    @Override public int act(float prevReward, float... nextInputs) {
        try {
            Number a = (Number) js.invokeFunction("act", nextInputs, prevReward);
            return a.intValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static void main(String[] args) throws Exception {
        DQN d = new DQN(2, 3);
        out.println( d.act(0, 0.5f, 0.5f) );
        out.println( d.act(-0.2f, 0.5f, 0.7f) );
        out.println( d.act(0.2f, 0.1f, 0.7f) );
    }
}

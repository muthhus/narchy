package nars;

import jdk.nashorn.api.scripting.NashornScriptEngine;

import javax.script.*;
import java.io.FileReader;

import static java.lang.System.*;

/**
 * Created by me on 5/3/16.
 */
public class DQN {


    private Invocable js;

    public DQN(int inputs, int actions)  {

        try {
            NashornScriptEngine engine = (NashornScriptEngine) new ScriptEngineManager().getEngineByName("nashorn");
            engine.eval(new FileReader("/home/me/opennars/app/src/main/java/nars/rl.js"));


            CompiledScript cscript = engine.compile(
                    "java.lang.System.out.println('creating new rl.js agent'); " +
                    "var Math = Java.type('java.lang.Math'); " +
                    "var env = { getNumStates: function() { return " + inputs + "; }, getMaxNumActions: function() { return " + actions + "; } }; " +
                    //http://cs.stanford.edu/people/karpathy/reinforcejs/index.html
                    "var opts =  { alpha: 0.01 }; " +
                    "var agent = new RL.DQNAgent(env, opts); " +
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

    public int learn(float prevReward, float... nextInputs) {
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
        out.println( d.learn(0, 0.5f, 0.5f) );
        out.println( d.learn(-0.2f, 0.5f, 0.7f) );
        out.println( d.learn(0.2f, 0.1f, 0.7f) );
    }
}

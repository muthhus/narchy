package nars;


import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.gs.collections.api.tuple.Twin;
import com.gs.collections.impl.tuple.Tuples;
import com.gs.collections.impl.tuple.primitive.PrimitiveTuples;
import nars.util.Agent;
import nars.util.DQN;
import nars.util.Texts;
import nars.util.data.Util;

import nars.util.experiment.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class AIGymClient extends Shell  {

    private static final Logger logger = LoggerFactory.getLogger(AIGymClient.class);



    private final String envID;
    boolean envReady;
    //final AtomicBoolean replReady = new AtomicBoolean(true);
    private final AtomicReference<Consumer<String>> nextLineConsumer = new AtomicReference(null);
    private int framesRemain;
    private AgentBuilder agentBuilder;
    private Agent agent = null;
    int inputs, outputs;

    //public final PrintStream output;
    //private final Future<ProcessResult> future;

    public AIGymClient(String environmentID) throws IOException {
        super("python", "-i" /* interactive */);

        input("import gym, logging, json, yaml");

        input("logging.getLogger().setLevel(logging.INFO)");

        input("def enc(x):\n\treturn json.dumps(x)\n");
        input("def encS(x):\n\tx.pop() ; x[0] = x[0].tolist() ; x[2] = str(x[2])\n\treturn json.dumps(x)\n");

        this.envID = environmentID;
        input("env = gym.make('" + environmentID + "')"); //TODO get initial state

    }

    @Override
    protected void readln(String line) {
        super.readln(line);

        if (!line.startsWith(">>>")) //ignore input echo
            return;

        if (!envReady && line.contains("new env:") && line.contains(envID)) {
            envReady = true;
        }

        if (nextLineConsumer!=null) /* since it can be called from super class constructor */ {
            Consumer<String> c = nextLineConsumer.getAndSet(null);
            if (c != null) {
                c.accept(line);
            }
        }


    }

    protected void input(String line) {
        input(line, null);
    }

    protected void input(String line, Consumer<String> result) {
        if (!nextLineConsumer.compareAndSet(null, result)) {
            throw new RuntimeException("repl interruption");
        }
        println(line);
    }

    protected void waitEnv() {
        //TODO dont use polling method but a better async interrupt
        if (envReady)
            return;
        logger.info("environment {} starting...", envID);
        while (!envReady) {
            Util.pause(20);
        }
        logger.info("environment {} ready", envID);
    }

    @FunctionalInterface interface AgentBuilder {
        public Agent newAgent(int inputs, int outputs);
    }

    public synchronized void run(int frames, AgentBuilder a) {

        waitEnv();

        framesRemain = frames;

        this.agentBuilder = a;

        //begins chain reaction
        input("enc([env.observation_space.low.tolist(),env.observation_space.high.tolist(),str(env.action_space),env.reset().tolist()])", this::onFrame);
        

    }


    private synchronized void onFrame(String f) {
        //ob, reward, done, _

        if (f.contains("Discrete") || f.contains("Continuous")) {
            //first cycle, determine model parameters
            //model = Tuples.twin()
            inputs = 1;
            outputs = 0;
            agent = agentBuilder.newAgent(inputs, outputs);
        } else {

            String j = f.substring(5, f.length() - 1);


            JsonArray state = ((JsonArray) Json.parse(j));

            JsonArray statePart = (JsonArray) state.get(0);
            double[] input = statePart.values().stream().mapToDouble(v -> v.asDouble()).toArray();
            double reward = state.get(1).asDouble();
            boolean finished = state.get(2).asString().equals("True");

            int a = act(input, reward, finished);
        }


        if (--framesRemain > 0) {
            nextFrame();
        }
    }

    protected int act(double[] input, double reward, boolean finished) {

        System.out.println(Texts.n4(input) + " |- " + reward);
        return 0;
    }

    private void nextFrame() {
        //ob, reward, done, _
        input("encS(list(env.step(env.action_space.sample())))", this::onFrame);
    }

    public static void main(String[] args) throws Exception {
        AIGymClient c = new AIGymClient(
                "CartPole-v0"
                //"DoomDeathmatch-v0"
        );
        c.run(3, (i,o) -> new DQN(i, o));

/*
    # The world's simplest agent!
    class RandomAgent(object):
        def __init__(self, action_space):
            self.action_space = action_space

        def act(self, observation, reward, done):
            return self.action_space.sample()


    # You can optionally set up the logger. Also fine to set the level
    # to logging.DEBUG or logging.WARN if you want to change the
    # amount of output.
    logger = logging.getLogger()
    logger.setLevel(logging.INFO)

    # also dump to a tempdir if you'd like: tempfile.mkdtemp().
    outdir = '/tmp/random-agent-results'
    env.monitor.start(outdir, force=True)

    episode_count = 100
    max_steps = 200
    reward = 0
    done = False

    for i in range(episode_count):
        ob = env.reset()

        for j in range(max_steps):
            action = agent.act(ob, reward, done)
            ob, reward, done, _ = env.step(action)
            if done:
                break

    # Dump result info to disk
    env.monitor.close()
*/
        //Util.pause(1000);


    }




//    public static void main(String[] args) throws Exception {
//        File pyFile = new File("/tmp/x.py");
//        String ret = (String) PyCaller.call(pyFile, "hi", (Object)new String[]{"all", "my", "friend", "!"});
//        System.out.println(ret);
//        //Assert.assertEquals("hello, all my friend ! ",ret);
//    }


//        Socket s = new Socket();
//        //s.setKeepAlive(true);
//        s.setReceiveBufferSize(1);
//        s.setSendBufferSize(1);
//        //s.setTcpNoDelay(true);
//
//        String host = "127.0.0.1";
//        int port = 7777;
//        PrintWriter s_out = null;
//
//            s.connect(new InetSocketAddress(host , port));
//
//            //writer for socket
//            s_out = new PrintWriter( s.getOutputStream());
//        BufferedReader s_in = new BufferedReader(new InputStreamReader(s.getInputStream()));
//
//
//        System.out.println(s_in.readLine());
//
//        //s_out.println( "admin\nadmin\n" );
//        //s_out.flush();
//
//        //Send message to server
//        String message = "print 1+2\nprint 3+1\n";
//        s_out.println( message );
//        s_out.flush();
//
//
//
//        //Get response from server
//        String response;
//
//        do {
//            response = s_in.readLine();
//            System.out.println( response );
//
//            Thread.sleep(100);
//        } while (true);
//
//
//
//    }
}

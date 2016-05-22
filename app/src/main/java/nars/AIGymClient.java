package nars;


import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import nars.nar.Default;
import nars.util.Agent;
import nars.util.DQN;
import nars.util.NAgent;
import nars.util.Texts;
import nars.util.data.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class AIGymClient extends Shell {

    private static final Logger logger = LoggerFactory.getLogger(AIGymClient.class);


    private final String envID;
    boolean envReady;
    //final AtomicBoolean replReady = new AtomicBoolean(true);
    private final AtomicReference<Consumer<String>> nextLineConsumer = new AtomicReference(null);
    private int framesRemain;
    private AgentBuilder agentBuilder;
    private Agent agent = null;
    int inputs, outputs;
    private int nextAction = -1;
    private double[] low;
    private double[] high;
    private double[] input;
    private boolean finished;

    //public final PrintStream output;
    //private final Future<ProcessResult> future;

    public AIGymClient(String environmentID) throws IOException {
        super("python", "-i" /* interactive */);

        input("import gym, logging, json, yaml");

        input("logging.getLogger().setLevel(logging.INFO)");

        input("def enc(x):\n\treturn json.dumps(x)\n");
        input("def encS(x):\n\tx.pop() ; x[0] = x[0].tolist() ; x[2] = str(x[2])\n\treturn json.dumps(x)\n");


        this.envID = environmentID;
        input("env = gym.make('" + environmentID + "')");

    }

    @Override
    protected void readln(String line) {
        super.readln(line);

        if (!line.startsWith(">>>")) //ignore input echo
            return;

        if (!envReady && line.contains("new env:") && line.contains(envID)) {
            envReady = true;
        }

        if (nextLineConsumer != null) /* since it can be called from super class constructor */ {
            synchronized (nextLineConsumer) {
                Consumer<String> c = nextLineConsumer.get();
                if (c != null) {
                    try {
                        c.accept(line);
                        //nextLineConsumer.set(null);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                }

            }
        }


    }

    protected void input(String line) {
        input(line, null);
    }

    protected void input(String line, Consumer<String> result) {
        //if (nextLineConsumer.compareAndSet(null, result) || nextLineConsumer.compareAndSet(result, result)) {

        synchronized (nextLineConsumer) {
            System.out.println(nextLineConsumer.get() + " -> " + result);

            if (result != null)
                nextLineConsumer.set(result);

            println(line);
        }
        //}
        //throw new RuntimeException("repl interruption");
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

    @FunctionalInterface
    interface AgentBuilder {
        public Agent newAgent(int inputs, float[] inputLow, float[] inputHigh, int outputs);
    }

    public synchronized void run(AgentBuilder a, int frames) {

        finished = false;

        waitEnv();

        //env.monitor.start('/tmp/" + environmentID + "',force=True)");

        framesRemain = frames;

        this.agentBuilder = a;

        //begins chain reaction
        input("enc([env.observation_space.low.tolist(),env.observation_space.high.tolist(),str(env.action_space),env.reset().tolist()])", this::onFrame);


    }


    private void onFrame(String f) {

        JsonArray j = pyjson(f);

        if (f.contains("Discrete") || f.contains("Continuous")) {
            //first cycle, determine model parameters
            //model = Tuples.twin()
            this.input = asArray(j, 3);
            this.inputs = input.length;

            this.low = asArray(j, 0);
            this.high = asArray(j, 1);
            //restore +-Infinity HACK
            for (int i = 0; i < inputs; i++) {
                if (low[i] == high[i]) {
                    low[i] = Double.NEGATIVE_INFINITY;
                    high[i] = Double.POSITIVE_INFINITY;
                }
            }

            String actionSpace = j.get(2).asString();
            if (actionSpace.startsWith("Discrete")) {
                this.outputs = Integer.parseInt(actionSpace.substring(9, actionSpace.length() - 1));
            } else {
                throw new UnsupportedOperationException("Unknown action_space type: " + actionSpace);
            }


            agent = agentBuilder.newAgent(inputs, Util.toFloat(low), Util.toFloat(high), outputs);
            agent.start(inputs, outputs);

            nextAction = agent.act(0, input);

        } else {

            //ob, reward, done, _

            input = asArray(j, 0);
            double reward = asDouble(j, 1);
            finished = j.get(2).asString().equals("True");

            System.out.println(Texts.n4(input) + " |- " + reward);

            nextAction = agent.act(reward, input);
        }


        if (!finished && --framesRemain > 0) {
            nextFrame();
        }
    }

    static JsonArray pyjson(String f) {
        String j = f.substring(5, f.length() - 1);
        j = j.replace("Infinity", "0");

        //doesnt handle Inf
        return ((JsonArray) Json.parse(j));
    }

    private static double[] asArray(JsonArray j, int index) {
        return ((JsonArray) j.get(index)).values().stream().mapToDouble(v -> v.asDouble()).toArray();
    }

    private static double asDouble(JsonArray j, int index) {
        return j.get(index).asDouble();
    }


    private void nextFrame() {
        //ob, reward, done, _
        input("encS(list(env.step(" + nextAction + ")))", this::onFrame);
    }

    public static void main(String[] args) throws IOException {
        new AIGymClient(
                "CartPole-v0"
                //"MountainCar-v0"
                //"DoomDeathmatch-v0" //2D inputs
        ).run((i, iLow, iHigh, o) ->
            //new DQN(i, o)
            new NAgent(new Default())
            ,160
        );

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

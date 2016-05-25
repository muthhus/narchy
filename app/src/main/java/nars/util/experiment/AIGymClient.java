package nars.util.experiment;


import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import nars.learn.Agent;
import nars.nar.Default;
import nars.op.time.MySTMClustered;
import nars.time.FrameClock;
import nars.util.NAgent;
import nars.util.Shell;
import nars.util.Texts;
import nars.util.data.Util;
import nars.util.data.random.XorShift128PlusRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class AIGymClient extends Shell {

    private static final Logger logger = LoggerFactory.getLogger(AIGymClient.class);


    private final String environmentID;
    boolean envReady;
    //final AtomicBoolean replReady = new AtomicBoolean(true);
    private final AtomicReference<Predicate<String>> nextLineConsumer = new AtomicReference(null);
    private int framesRemain, batchesRemain;
    private AgentBuilder agentBuilder;
    private Agent agent = null;
    int inputs, outputs;
    private int nextAction = -1;
    private double[] low;
    private double[] high;
    private double[] input;
    private boolean finished;
    private double reward;
    private double[] lastInput;

    //public final PrintStream output;
    //private final Future<ProcessResult> future;

    /**
     * terminates if batches < 0 or frames < 0
     */
    public AIGymClient(String environmentID, AgentBuilder a, int batches, int frames) throws IOException {
        super("python", "-i" /* interactive */);

        input("import gym, logging, json, yaml");
        input("import numpy as np");

        input("logging.getLogger().setLevel(logging.WARN)"); //INFO and LOWER are not yet supported


        input("def enc(x):\n\treturn json.dumps(x)\n");
        input("def encS(x):\n\tx.pop() ; x[0] = x[0].flatten().tolist() ; x[2] = int(x[2])\n\treturn json.dumps(x)\n");


        this.environmentID = environmentID;

        envReady = false;
        input("env = gym.make('" + environmentID + "')", (line) -> {
            if (line.contains("Making new env: " + environmentID)) {


                batchesRemain = batches;
                framesRemain = frames;

                this.agentBuilder = a;

                //video_callable=None
//                input("env.monitor.start('/tmp/" + this.environmentID + "',force=True)", x -> {
//
//                    if (x.contains("Starting") || x.contains("Clearing")) {
//                    //if (x.contains("Clearing")) {
//                        //monitor started
//
//                        reset();
//                    }
//
//                    return true;
//                });
                reset();

            }
            return true;
        });

    }

    public void reset() {
        finished = false;

        input("enc([env.observation_space.low.flatten().tolist(),env.observation_space.high.flatten().tolist(),str(env.action_space),env.reset().flatten().tolist()])", line1 -> {

            batchesRemain--;
            envReady = true;

            onFrame(line1);

            return true;
        });
    }


    @Override
    protected void readln(String line) {
        super.readln(line);


        if (nextLineConsumer != null) /* since it can be called from super class constructor */ {

            synchronized (nextLineConsumer) {
                Predicate<String> c = nextLineConsumer.get();
                if (c != null) {
                    c.test(line);


                }

            }


        }


    }

    protected void input(String line) {
        input(line, null);
    }

    protected void input(String line, Predicate<String> result) {
        //if (nextLineConsumer.compareAndSet(null, result) || nextLineConsumer.compareAndSet(result, result)) {

        synchronized (nextLineConsumer) {

            nextLineConsumer.set(result);

            println(line);
        }
        //}
        //throw new RuntimeException("repl interruption");
    }

//    protected void waitEnv() {
//        //TODO dont use polling method but a better async interrupt
//        if (envReady)
//            return;
//        logger.info("environment {} starting...", environmentID);
//        while (!envReady) {
//            Util.pause(20);
//        }
//        logger.info("environment {} ready", environmentID);
//    }

    @FunctionalInterface
    interface AgentBuilder {
        public Agent newAgent(/*int inputs, float[] inputLow, float[] inputHigh, int outputs*/);
    }


    private boolean onFrame(String f) {


        f = f.trim();

        if (f.startsWith(">>> "))
            f = f.substring(4);

        if (f.startsWith("\'")) {
            f = f.substring(1, f.length() - 1);

            if (onJSON(f)) {
                framesRemain--;
                nextFrame();
            } else {
                System.err.println("not json: " + f);
            }
            return true;
        } /*else {
            System.err.println("ignored: " + f);
        }*/
        return false;
    }

    private void nextFrame() {
        if ((--framesRemain < 0) || (batchesRemain < 0))
            return;

        if (finished) {
            reset();
            return;
        }

        if (nextAction == -1) {
            nextAction = (int) Math.floor(Math.random() * outputs); //HACK
        }

        String actionString = actionModel.toEnvironment(nextAction);
        input("env.render() ; encS(list(env.step(" + actionString + ")))", this::onFrame);

        System.out.println( Texts.n4(reward) + " -| " + actionString
                // + " -| "  + Texts.n4(input)
                );

    }

    ActionModel actionModel = null;

    interface ActionModel {

        String toEnvironment(int i);

        int actions();
    }

    public static class DiscreteActionModel implements ActionModel {
        final int actions;

        public DiscreteActionModel(String a) {
            this.actions = Integer.parseInt(a.substring(9, a.length() - 1));
        }

        @Override
        public String toEnvironment(int i) {
            return Integer.toString(i);
        }

        @Override
        public int actions() {
            return actions;
        }
    }

    public static class BoxActionModel implements ActionModel {

        final int actions;
        private final float deltaSpeed;
        private final float maxSpeed;
        private final float decay = 0.9f;
        float f[];

        public BoxActionModel(String a, float deltaSpeed, float maxSpeed) {
            this.deltaSpeed = deltaSpeed;
            this.maxSpeed = maxSpeed;
            if (a.endsWith(",)")) {
                int dims = Integer.parseInt(a.substring(4, a.length() - 2));
                if (dims != 1)
                    throw new UnsupportedOperationException(a);
                actions = dims * 4; //TODO support proportional control
                f = new float[dims];
            } else {
                throw new UnsupportedOperationException(a);
            }
        }

        @Override
        public String toEnvironment(int i) {
            int index = i % 4;
            switch (index) {
                case 0:
                    break;
                case 1:
                    f[0] = f[0] * decay;
                    break;
                case 2:
                    f[0] = Util.clampBi(f[0] + deltaSpeed);
                    break;
                case 3:
                    f[0] = Util.clampBi(f[0] - deltaSpeed);
                    break;
            }



            float[] g = f.clone();
            for (int x = 0; x < g.length; x++)
                g[x] = maxSpeed * g[x];

            return "np.array(" + Arrays.toString(g) + ")";
        }

        @Override
        public int actions() {
            return actions;
        }
    }

    private boolean onJSON(String f) {

        JsonArray j = pyjson(f);
        if (j == null)
            return false;


        if (f.contains("Discrete") || f.contains("Box")) {
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

            String a = j.get(2).asString();
            if (a.startsWith("Discrete")) {
                this.actionModel = new DiscreteActionModel(a);
            } else if (a.startsWith("Box")) {
                this.actionModel = new BoxActionModel(a, 0.3f /*default */, 4f);
            } else {
                throw new UnsupportedOperationException("Unknown action_space type: " + a);
            }
            this.outputs = actionModel.actions();

            agent = agentBuilder.newAgent(/*inputs, Util.toFloat(low), Util.toFloat(high), outputs*/);
            agent.start(inputs, outputs);

            nextAction = agent.act(0, normalizeInput(input, lastInput));

            return true;
        } else {

            //ob, reward, done, _

            input = asArray(j, 0);
            reward = asDouble(j, 1);
            finished = j.get(2).asInt() == 1;

            nextAction = agent.act(reward, normalizeInput(input, lastInput));

            lastInput = input;

            return true;
        }


    }

    protected void computeDelta(double[] a, double[] b) {
        if (b == null) return;

        int byteDiffs = 0, bitDiffs = 0;
        for (int i = 0; i < a.length; i++) {
            if (!Util.equals(a[i], b[i], 0.5f/256.0f)) {
                byteDiffs++;
                int x = (int)Math.round(a[i]);
                int y = (int)Math.round(b[i]);
                int z = x ^ y;
                bitDiffs += Integer.bitCount(z);
            }
        }
        System.out.println("byte diffs: " + byteDiffs + " / " + a.length + "\t\tbit diffs: " + bitDiffs + " / " + (a.length * 8));
    }

    public double[] normalizeInput(double[] x, double[] x0 /* prev */) {

        //computeDelta(x, x0);

        //normalize input to low/high range
        double[] low = this.low;
        double[] high = this.high;
        for (int i = 0; i < x.length; i++) {
            double l = low[i];
            double h = high[i];
            x[i] = Util.normalize(x[i], l, h);
        }
        return x;
    }

    static JsonArray pyjson(String j) {
        j = j.replace("Infinity", "1");

        try {
            //doesnt handle Inf
            return ((JsonArray) Json.parse(j));
        } catch (Exception e) {
            System.err.println("can not parse: " + j);
            e.printStackTrace();
        }
        return null;

    }

    private static double[] asArray(JsonArray j, int index) {
        return ((JsonArray) j.get(index)).values().stream().mapToDouble(v -> v.asDouble()).toArray();
    }

    private static double asDouble(JsonArray j, int index) {
        return j.get(index).asDouble();
    }


    public static void main(String[] args) throws IOException {
        new AIGymClient(
                //"BeamRider-ram-v0"

                //"CrazyClimber-ram-v0"
                //"CrazyClimber-v0"

                //"CartPole-v0" //WARNING: infinity
                //"MountainCar-v0"
                //"DoomDeathmatch-v0" //2D inputs
                //"LunarLander-v1"
                "Pendulum-v0"
                //"InvertedDoublePendulum-v1"

                //"Pong-v0"
                //"Pong-ram-v0"

                //"BipedalWalker-v1"
                //"Hopper-v1"
                //"MsPacman-ram-v0"
                //"SpaceInvaders-ram-v0" //<---
                //"Hex9x9-v0"
                , () -> {
            //new DQN()

            {

                XorShift128PlusRandom rng = new XorShift128PlusRandom(1);
                Default nar = new Default(
                        512, 4, 1, 1, rng,
                        new Default.WeakTermIndex(256 * 1024, rng),
                        //new Default.SoftTermIndex(128 * 1024, rng),
                        //new Default.DefaultTermIndex(128 *1024, rng),
                        new FrameClock());
                nar.beliefConfidence(0.3f);
                nar.premiser.confMin.setValue(0.07f);
                nar.conceptActivation.setValue(0.2f);
                nar.cyclesPerFrame.set(32);

                new MySTMClustered(nar, 8, '.');
                //new MySTMClustered(nar, 24, '!');
                return new NAgent(nar);
            }
            //new HaiQAgent()
        }
                , 1000, 100000);

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

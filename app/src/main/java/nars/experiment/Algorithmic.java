package nars.experiment;

import com.gs.collections.api.tuple.Twin;
import com.gs.collections.impl.tuple.Tuples;
import nars.Global;
import nars.agent.NAgent;
import nars.learn.Agent;
import nars.learn.ql.DQN;
import nars.nar.Default;
import nars.util.data.random.XorShift128PlusRandom;

import java.util.Arrays;
import java.util.Random;

import static nars.experiment.pong.PongEnvironment.beliefChart;

/**
 * http://arxiv.org/pdf/1511.07275v2.pdf
 * https://github.com/openai/gym/blob/master/gym/envs/algorithmic/algorithmic_env.py
 */
abstract public class Algorithmic implements Environment {

     final int ins, outs;
    final int radix, last;
     final boolean chars;
    final int[] dim;
     int[] x;
     int[] target;
     int y;
     boolean done;
    float rewardSum = 0;
    int correct  = 0, incorrect = 0;

    int xMax; //determines max part of target which is visible to reader

    int nextAction = -1;
    private int time = 0, lastPredictionTime;

    boolean trace = true;
    private int rewPrintPeriod = 10;

    public Algorithmic(int[] space) {
        this(space, 10, false);
    }

    public Algorithmic(int[] space, int radix, boolean chars) {
        this.radix = radix;
        this.chars = chars;
        this.last = 10;


        this.ins = radix;// + 1;

        this.dim = space;
        int dims = space.length;
        this.outs = 1 /* predict? */ + (dims) /* read tape movement */ + radix;

        //action = ( #dim, +/-, #base )
        //self.action_space = Tuple(([Discrete(2 * self.inp_dim), Discrete(2), Discrete(self.base)]))

        /*

        hash_base = 50 ** np.arange(inp_dim)
        self.total_reward = 0
        self.sum_reward = 0
        AlgorithmicEnv.sum_rewards = []
        AlgorithmicEnv.current_length = 2
        tape_control = []


        self._seed()
        self.reset()
         */
        reset();
    }

    abstract int[] nextTarget();

    @Override
    public Twin<Integer> start() {
        return Tuples.twin(ins,outs);
    }

    @Override
    public float pre(int t, float[] ins) {
        time++;

        float reward = 0;

        if (nextAction >= 0) {

            int xx = x[0];
            if (nextAction == 0) { // (moveInput0) {
                //LEFT
                xx = (xx - 1);
                if (xx == -1) {
                    xx = xMax-1;
                }

            } else if (nextAction == 1) {
                //RIGHT
                xx = (xx + 1) % (xMax);
            } else {
                int pred = nextAction - 2;

                float dt = time - lastPredictionTime;
                float rewardFactor =
                        //1/dt; //less reward for delayed prediction caused by reading
                        1f;

                if (pred == target[y]) {
                    reward = (1f * rewardFactor);
                    lastPredictionTime = time;
                    correct++;
                } else {
                    reward = -1f;
                    incorrect++;
                    done = true;
                }
                y++;
            }

            x[0] = xx;

        }

        if (x[0] > xMax)
            throw new RuntimeException("tried to read beyond revealed tape segment");

        if (y >= target.length)
            done = true;

        if (done)
            reset();

        Arrays.fill(ins, 0);


        ins[target[x[0]]] = 1;


        if (time%rewPrintPeriod == 0) {
            System.out.println(rewardSum + ", " + correct + ", " + incorrect);
        }

        this.rewardSum += reward;
        return reward;
    }

    @Override
    public void post(int t, int action, float[] ins, Agent a) {
        this.nextAction = action;
        if (trace) {
            System.out.println(a.summary());
        }
    }

    public String targetString() {
        if (chars) {
            StringBuilder sb = new StringBuilder(target.length);
            for (int x : target) {
                sb.append((char)('A'+x));
            }
            return sb.toString();
        } else {
            return Arrays.toString(target);
        }
    }

/*



    def _step(self, action):
        self.last_action = action
        inp_act, out_act, pred = action
        done = False
        reward = 0.0
        # We are outside the sample.
        self.time += 1
        if self.y not in self.target:
            reward = -10.0
            done = True
        else:
            if out_act == 1:
                if pred == self.target[self.y]:
                    reward = 1.0
                else:
                    reward = -0.5
                    done = True
                self.y += 1
                if self.y not in self.target:
                    done = True
            if inp_act == 0:
                self.x[0] -= 1
            elif inp_act == 1:
                self.x[0] += 1
            elif inp_act == 2:
                self.x[1] -= 1
            elif inp_act == 3:
                self.x[1] += 1
            if self.time > self.total_len + self.total_reward + 4:
                reward = -1.0
                done = True
        obs = self._get_obs()
        self.reward = reward
        self.sum_reward += reward
        return (obs, reward, done, {})
*/

    protected void reset() {
        //this.x = Array.newInstance(int.class, dim);
        if (dim.length > 1)
            throw new UnsupportedOperationException("yet");
        this.x = new int[dim.length];
        this.y = 0;
        this.done = false;
        this.lastPredictionTime = time;
        this.target = nextTarget();

    }

/*
    def _reset(self):
        self.last_action = None
        self.x = np.zeros(self.inp_dim).astype(np.int)
        self.y = 0
        AlgorithmicEnv.sum_rewards.append(self.sum_reward - self.total_reward)
        AlgorithmicEnv.sum_rewards = AlgorithmicEnv.sum_rewards[-self.last:]
        if len(AlgorithmicEnv.sum_rewards) == self.last and \
          min(AlgorithmicEnv.sum_rewards) >= -1.0 and \
          AlgorithmicEnv.current_length < 30:
            AlgorithmicEnv.current_length += 1
            AlgorithmicEnv.sum_rewards = []
        self.sum_reward = 0.0
        self.time = 0
        self.total_len = self.np_random.randint(3) + AlgorithmicEnv.current_length
        self.set_data()
        return self._get_obs()
 */

    /** https://github.com/openai/gym/blob/master/gym/envs/algorithmic/copy.py */
    public static class CopyTask extends Algorithmic {

        final static Random rng = new XorShift128PlusRandom(1);

        public CopyTask(int length, int base) {
            super(new int[] { length }, base, true);
        }

        @Override
        int[] nextTarget() {
            int[] i = new int[dim[0]];
            for (int x = 0; x < i.length; x++)
                i[x] = rng.nextInt(radix);

            this.xMax = i.length; //see entire thing
            return i;
        }
    }

    public static void main(String[] args) {

        Global.DEBUG = true;

        Default n = new Default(1024, 4, 2, 2);
        n.beliefConfidence(0.55f);
        n.goalConfidence(0.55f);
        n.DEFAULT_BELIEF_PRIORITY = 0.8f;
        n.DEFAULT_GOAL_PRIORITY = 0.8f;
        n.DEFAULT_QUESTION_PRIORITY = 0.6f;
        n.DEFAULT_QUEST_PRIORITY = 0.6f;
        n.cyclesPerFrame.set(128);
        //n.logSummaryGT(System.out, 0.2f);
        //n.log();

        final NAgent a = new NAgent(n) {
            @Override
            public void start(int inputs, int actions) {
                super.start(inputs, actions);
                beliefChart(this);
            }
        };

        n.onTask(tt -> {
            if (tt.isGoal()) {
                if ((tt.term().equals(a.happy) && tt.freq() < 0.5f) ||
                        (tt.term().equals(a.sad) && tt.freq() > 0.5f))
                {
                    if (tt.conf() > 0.25f) {
                        System.err.println("WTF psychotic");
                        System.err.println(tt.explanation());
                    }
                }
            }
        });

        new CopyTask(2, 3).run(
                a,
                //new DQN(),
                1024);


        a.printActions();

    }
}

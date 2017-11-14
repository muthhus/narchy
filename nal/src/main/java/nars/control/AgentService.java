package nars.control;

import jcog.learn.Agent;
import jcog.math.FloatSupplier;
import jcog.math.IntIntToObjectFunc;
import jcog.math.tensor.ScalarTensor;
import jcog.math.tensor.Tensor;
import nars.$;
import nars.NAR;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class AgentService extends DurService {

    private final Consumer<float[]> input;

    private final Agent agent;

    private final IntConsumer act;

    private final FloatSupplier reward;

    /** buffer where inputs to the agent are stored */
    public final float[] in;

    public AgentService(IntIntToObjectFunc<Agent> a, int inputs, Consumer<float[]> input, FloatSupplier reward, int outputs, IntConsumer act, MutableFloat durations, NAR n) {
        super(n, durations);
        this.input = input;
        this.in = new float[inputs];
        this.reward = reward;
        this.agent = a.apply(inputs, outputs);
        this.act = act;
    }

    @Override
    public void run(NAR nar, long dt) {
        input.accept(in);
        int a = agent.act(reward.asFloat(), in);
        act.accept(a);
    }

    public static class AgentBuilder {
        final List<Tensor> sensors = $.newArrayList();
        final List<IntObjectPair<? extends IntConsumer>> actions = $.newArrayList();
        final FloatSupplier reward;
        private final IntIntToObjectFunc<Agent> a;
        float durations = 1f;

        /** whether to add an extra NOP action */
        private final static boolean NOP_ACTION = true;

        public AgentBuilder(IntIntToObjectFunc<Agent> a, FloatSupplier reward) {
            this.a = a;
            this.reward = reward;
        }

        public AgentBuilder durations(float runEveryDurations) {
            this.durations = runEveryDurations;
            return this;
        }

        public AgentService get(NAR nar) {

            final int inputs = sensors.stream().mapToInt(Tensor::volume).sum();
            final int outputs = actions.stream().mapToInt(IntObjectPair::getOne).sum() + (NOP_ACTION ? 1 : 0);

            Consumer<float[]> inputter = (f) -> {
                int s = sensors.size();
                int j = 0;
                for (int i = 0; i < s; i++) {
                    Tensor x = sensors.get(i);
                    x.writeTo(f, j);
                    j += x.volume();
                }
                assert(j == f.length);
            };
            IntConsumer act = (c) -> {

                int s = actions.size();
                for (int i = 0; i < s; i++) {
                    IntObjectPair<? extends IntConsumer> aa = actions.get(i);
                    int bb = aa.getOne();
                    if (c >= bb) {
                        c -= bb;
                    } else {
                        aa.getTwo().accept(c);
                        return;
                    }
                }
            };
            return new AgentService(a, inputs, inputter, reward, outputs, act, new MutableFloat(durations), nar);
        }

        public AgentBuilder in(FloatSupplier f) {
            sensors.add(new ScalarTensor(f));
            return this;
        }

        public AgentBuilder in(Tensor t) {
            sensors.add(t);
            return this;
        }

        public AgentBuilder out(int actions, IntConsumer action) {
            return out(PrimitiveTuples.pair(actions, action));
        }

        public AgentBuilder out(IntObjectPair<? extends IntConsumer> action) {
            actions.add(action);
            return this;
        }

    }

}

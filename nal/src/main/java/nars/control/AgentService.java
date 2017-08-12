package nars.control;

import jcog.learn.Agent;
import jcog.math.FloatSupplier;
import jcog.util.IntIntToObjectFunc;
import nars.NAR;
import org.apache.commons.lang3.mutable.MutableFloat;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class AgentService extends DurService {

    private final Consumer<float[]> input;
    private final Agent agent;
    private final IntConsumer act;
    private final FloatSupplier reward;
    private final float[] in;

    public AgentService(IntIntToObjectFunc<Agent> a, int inputs, Consumer<float[]> input, FloatSupplier reward, int outputs, IntConsumer act, MutableFloat durations, NAR n) {
        super(n, durations);
        this.input = input;
        this.in = new float[inputs];
        this.reward = reward;
        this.agent = a.apply(inputs, outputs);
        this.act = act;
    }

    @Override
    protected void runDur(NAR nar) {
        input.accept(in);
        int a = agent.act(reward.asFloat(), in);
        act.accept(a);
    }
}

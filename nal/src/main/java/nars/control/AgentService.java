package nars.control;

import jcog.learn.Agent;
import jcog.math.FloatSupplier;
import jcog.util.IntIntToObjectFunc;
import nars.NAR;
import org.apache.commons.lang3.mutable.MutableFloat;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class AgentService extends DurService {

    private final Supplier<float[]> input;
    private final Agent agent;
    private final IntConsumer act;
    private final FloatSupplier reward;

    public AgentService(IntIntToObjectFunc<Agent> a, Supplier<float[]> input, FloatSupplier reward, int outputs, IntConsumer act, MutableFloat durations, NAR n) {
        super(n, durations);
        this.input = input;
        int inputs = input.get().length;
        this.reward = reward;
        this.agent = a.apply(inputs, outputs);
        this.act = act;
    }

    @Override
    protected void runDur(NAR nar) {
        int a = agent.act(reward.asFloat(), input.get());
        act.accept(a);
    }
}

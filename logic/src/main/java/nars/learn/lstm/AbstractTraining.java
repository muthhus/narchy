package nars.learn.lstm;


import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public abstract class AbstractTraining {
    public int batchsize = 400000000;

    public AbstractTraining(Random random, final int inputs, final int outputs) {
        this.random = random;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public double scoreSupervised(AgentSupervised agent)  {

        final double[] fit = {0};
        final double[] max_fit = {0};

        this.interact(inter -> {
            if (inter.do_reset)
                agent.clear();

            if (inter.expected == null) {
                agent.predict(inter.actual, false);
            }
            else {
                double[] actual_output = null;

                if (validation_mode)
                    actual_output = agent.predict(inter.actual, true);
                else
                    actual_output = agent.learn(inter.actual, inter.expected, true);

                if (util.argmax(actual_output) == util.argmax(inter.expected))
                    fit[0]++;

                max_fit[0]++;
            }
        });

        return fit[0] / max_fit[0];
    }

    public void supervised(AgentSupervised agent) throws Exception {

        List<AgentSupervised.NonResetInteraction> agentNonResetInteraction = new ArrayList<>();

        this.interact(inter -> {

            if (inter.do_reset) {
                agentExecuteNonResetInteractionsAndFlush(agent, agentNonResetInteraction);

                agent.clear();
            }

            AgentSupervised.NonResetInteraction newInteraction = new AgentSupervised.NonResetInteraction();
            newInteraction.observation = inter.actual;
            newInteraction.target_output = inter.expected;
            agentNonResetInteraction.add(newInteraction);

            if( agentNonResetInteraction.size() > batchsize ) {
                agentExecuteNonResetInteractionsAndFlush(agent, agentNonResetInteraction);
            }

        });

        agentExecuteNonResetInteractionsAndFlush(agent, agentNonResetInteraction);
    }

    private void agentExecuteNonResetInteractionsAndFlush(AgentSupervised agent, final List<AgentSupervised.NonResetInteraction> nonResetInteractions)  {
        agent.learnBatch(nonResetInteractions, false);

        nonResetInteractions.clear();
    }


    public final static class Interaction {

        public double[] actual;
        public double[] expected;

        public boolean do_reset;

        @Override
        public String toString() {
            return ArrayUtils.toString(actual) + " " +
                    ArrayUtils.toString(expected) + " " +
                    do_reset;
        }
    }

    protected final Random random;
    protected int batches; // need to be set by GenerateInteractions()
    protected boolean validation_mode;
    protected abstract void interact(Consumer<Interaction> each);

    public final int inputs;
    public final int outputs;
}

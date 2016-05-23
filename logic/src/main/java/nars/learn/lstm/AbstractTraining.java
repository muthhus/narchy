package nars.learn.lstm;


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

    public SimpleLSTM lstm(int cell_blocks, double initialLearningRate) {
        return new SimpleLSTM(random, inputs, outputs, cell_blocks, initialLearningRate);
    }

    @Deprecated public double scoreSupervised(AgentSupervised agent)  {

        final double[] fit = {0};
        final double[] max_fit = {0};

        this.interact(inter -> {
            if (inter.reset)
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

    @Deprecated public void supervised(AgentSupervised agent) throws Exception {

        List<AgentSupervised.NonResetInteraction> agentNonResetInteraction = new ArrayList<>();

        this.interact(inter -> {

            if (inter.reset) {
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


    protected final Random random;
    protected int batches; // need to be set by GenerateInteractions()
    protected boolean validation_mode;

    @Deprecated protected abstract void interact(Consumer<Interaction> each);

    public final int inputs;
    public final int outputs;
}

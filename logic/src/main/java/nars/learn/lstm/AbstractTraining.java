package nars.learn.lstm;


import nars.util.Util;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public abstract class AbstractTraining {

    public AbstractTraining(Random random, final int inputs, final int outputs) {
        this.random = random;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public SimpleLSTM lstm(int cell_blocks) {
        return new SimpleLSTM(random, inputs, outputs, cell_blocks);
    }

    @Deprecated public double scoreSupervised(SimpleLSTM agent)  {

        float learningRate = 0.1f;
        final double[] fit = {0};
        final double[] max_fit = {0};

        this.interact(inter -> {
            if (inter.forget > 0)
                agent.forget(inter.forget);

            if (inter.expected == null) {
                agent.predict(inter.actual);
            }
            else {
                double[] actual_output = null;

                if (validation_mode)
                    actual_output = agent.predict(inter.actual);
                else
                    actual_output = agent.learn(inter.actual, inter.expected, learningRate);

                if (Util.argmax(actual_output) == Util.argmax(inter.expected))
                    fit[0]++;

                max_fit[0]++;
            }
        });

        return fit[0] / max_fit[0];
    }

//    @Deprecated public void supervised(SimpleLSTM agent) throws Exception {
//
//        List<AgentSupervised.NonResetInteraction> agentNonResetInteraction = new ArrayList<>();
//
//        this.interact(inter -> {
//
//            if (inter.forget==1f) {
//                agentExecuteNonResetInteractionsAndFlush(agent, agentNonResetInteraction);
//            }
//
//            AgentSupervised.NonResetInteraction newInteraction = new AgentSupervised.NonResetInteraction();
//            newInteraction.observation = inter.actual;
//            newInteraction.target_output = inter.expected;
//            agentNonResetInteraction.add(newInteraction);
//
//            if( agentNonResetInteraction.size() > batchsize ) {
//                agentExecuteNonResetInteractionsAndFlush(agent, agentNonResetInteraction);
//            }
//
//        });
//
//        agentExecuteNonResetInteractionsAndFlush(agent, agentNonResetInteraction);
//    }

    private void agentExecuteNonResetInteractionsAndFlush(SimpleLSTM agent, final List<AgentSupervised.NonResetInteraction> nonResetInteractions, float forgetRate)  {
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

package jcog.learn.lstm;

import java.util.List;

public abstract class AgentSupervised {
	public final static class NonResetInteraction {
		public double[] observation;
		public double[] target_output;
	}

	public abstract void clear();
	public abstract double[] learn(double[] input, double[] target_output, float learningRate) ;
	public abstract double[] learnBatch(List<NonResetInteraction> interactions, final boolean requireOutput) ;
	public abstract double[] predict(double[] input) ;
}

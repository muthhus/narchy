package jcog.learn.lstm;

public final class IdentityNeuron implements Neuron
{
	@Override
	final public double activate(double x)
	{
		return x;
	}

	@Override
	final public double derivate(double x) {
		return 1.0;
	}
}


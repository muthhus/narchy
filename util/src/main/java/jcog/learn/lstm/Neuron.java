package jcog.learn.lstm;

public interface Neuron
{
	static Neuron build(NeuronType neuron_type)
	{
		switch (neuron_type) {
			case Sigmoid:
				return new SigmoidNeuron();
			case Tanh:
				return new TanhNeuron();
			case Identity:
				return new IdentityNeuron();
			default:
				throw new RuntimeException("ERROR: unknown neuron type");
		}
	}
	
	double activate(double x);
	double derivate(double x);
}

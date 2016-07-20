//package nars.learn.freeform;
//
//import org.encog.engine.network.activation.ActivationSigmoid;
//import org.encog.mathutil.error.ErrorCalculation;
//import org.encog.neural.freeform.FreeformConnection;
//import org.encog.neural.freeform.FreeformNeuron;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.function.Consumer;
//
///**
// * From Encog https://github.com/encog/encog-java-core
// * Provides basic propagation functions to other trainers.
// */
//public abstract class FreeformPropagationTraining {
////    /**
////     * The training data.
////     */
////    private MLDataSet training;
//
////
////    /**
////     * The current iteration.
////     */
////    private int iteration;
//
//	/**
//	 * The constant to use to fix the flat spot problem.
//	 */
//	public static final double FLAT_SPOT_CONST = 0.1;
//
//	/**
//	 * The network that we are training.
//	 */
//	private final FreeformNetwork network;
//
//
//	/**
//	 * The number of iterations.
//	 */
//	private int iterationCount;
//
//	/**
//	 * The error at the beginning of the last iteration.
//	 */
//	private double error;
//
////	/**
////	 * The neurons that have been visited.
////	 */
////	private final Set<FreeformNeuron> visited = new HashSet<>();
//
//	/**
//	 * Are we fixing the flat spot problem?  (default = true)
//	 */
//	private final boolean fixFlatSopt = true;
//
////	/**
////	 * The batch size. Specify 1 for pure online training. Specify 0 for pure
////	 * batch training (complete training set in one batch). Otherwise specify
////	 * the batch size for batch training.
////	 */
////	private int batchSize;
//
//
//
//	/**
//	 * Construct the trainer.
//	 * @param theNetwork The network to train.
//	 * @param theTraining The training data.
//	 */
//	public FreeformPropagationTraining(final FreeformNetwork theNetwork) {
//		//super(TrainingImplementationType.Iterative);
//		this.network = theNetwork;
//	}
//
//	/**
//	 * Calculate the gradient for a neuron.
//	 * @param toNeuron The neuron to calculate for.
//	 */
//	private void calculateNeuronGradient(@NotNull final FreeformNeuron toNeuron) {
//
//		// Only calculate if layer has inputs, because we've already handled the
//		// output
//		// neurons, this means a hidden layer.
//		if (toNeuron.getInputSummation() != null) {
//
//			// between the layer deltas between toNeuron and the neurons that
//			// feed toNeuron.
//			// also calculate all inbound gradeints to toNeuron
//			for (final FreeformConnection connection : toNeuron
//					.getInputSummation().list()) {
//
//				// calculate the gradient
//				final double gradient = connection.getSource().getActivation()
//						* toNeuron.getTempTraining(0);
//				connection.addTempTraining(0, gradient);
//
//				// calculate the next layer delta
//				final FreeformNeuron fromNeuron = connection.getSource();
//				double sum = 0.0;
//				for (final FreeformConnection toConnection : fromNeuron
//						.getOutputs()) {
//					sum += toConnection.getTarget().getTempTraining(0)
//							* toConnection.getWeight();
//				}
//				final double neuronOutput = fromNeuron.getActivation();
//				final double neuronSum = fromNeuron.getSum();
//				double deriv = toNeuron.getInputSummation()
//						.getActivationFunction()
//						.derivativeFunction(neuronSum, neuronOutput);
//
//				if (this.fixFlatSopt
//						&& (toNeuron.getInputSummation()
//								.getActivationFunction() instanceof ActivationSigmoid)) {
//					deriv += FreeformPropagationTraining.FLAT_SPOT_CONST;
//				}
//
//				final double layerDelta = sum * deriv;
//				fromNeuron.setTempTraining(0, layerDelta);
//			}
//
//			// recurse to the next level
//			for (final FreeformConnection connection : toNeuron
//					.getInputSummation().list()) {
//				final FreeformNeuron fromNeuron = connection.getSource();
//				calculateNeuronGradient(fromNeuron);
//			}
//
//		}
//
//	}
//
//	/**
//	 * Calculate the output delta for a neuron, given its difference.
//	 * Only used for output neurons.
//	 * @param neuron
//	 * @param diff
//	 */
//	private void calculateOutputDelta(@NotNull final FreeformNeuron neuron,
//                                      final double diff) {
//		final double neuronOutput = neuron.getActivation();
//		final double neuronSum = neuron.getInputSummation().getSum();
//		double deriv = neuron.getInputSummation().getActivationFunction()
//				.derivativeFunction(neuronSum, neuronOutput);
//		if (this.fixFlatSopt
//				&& (neuron.getInputSummation().getActivationFunction() instanceof ActivationSigmoid)) {
//			deriv += FreeformPropagationTraining.FLAT_SPOT_CONST;
//		}
//		final double layerDelta = deriv * diff;
//		neuron.setTempTraining(0, layerDelta);
//	}
//
//
//
////	/**
////	 * {@inheritDoc}
////	 */
////	@Override
////	public MLDataSet getTraining() {
////		return this.training;
////	}
//
//	/**
//	 * @return True, if we are fixing the flat spot problem.
//	 */
//	public boolean isFixFlatSopt() {
//		return this.fixFlatSopt;
//	}
//
//
//	/**
//	 * {@inheritDoc}
//	 */
//
//	public void iteration() {
//		//preIteration();
//		this.iterationCount++;
//		this.network.clear();
//
//        processPureBatch();
//		//postIteration();
//	}
//
//
//
//    public static class MLDataPair {
//        public double weight;
//        public double[] ideal;
//        public double[] input;
//    }
//
//
//	/**
//	 * Process training for pure batch mode (one single batch).
//	 */
//	protected void processPureBatch(@NotNull MLDataPair... training) {
//		final ErrorCalculation errorCalc = new ErrorCalculation();
//		//this.visited.clear();
//
//        double[] actual = null;
//		for (final MLDataPair pair : training) {
//			final double[] input = pair.input;
//			final double[] ideal = pair.ideal;
//			actual  = this.compute(input, actual);
//			final double sig = pair.weight;
//
//			errorCalc.updateError(actual, ideal, sig);
//
//            process(ideal, actual, sig);
//        }
//
//		// Set the overall error.
//        this.error = errorCalc.calculate();
//
//        // Learn for all data.
//		learn();
//	}
//
//    public static final class FreeformNetwork {
//        final FreeformNeuron[] in, out;
//
//        public FreeformNetwork(FreeformNeuron[] in, FreeformNeuron[] out) {
//            this.in = in;
//            this.out = out;
//        }
//
//        public void clear() {
//            eachVertex((FreeformNeuron f) -> {
//                f.setActivation(0);
//            });
//        }
//
//        //vertex
//        public void eachVertex(Consumer<FreeformNeuron> c) {
//
//        }
//
//        //edge
//        public void eachEdge(Consumer<FreeformConnection> c) {
//
//        }
//    }
//
//    @Nullable
//    private double[] compute(@NotNull double[] input, @Nullable double[] result) {
//
//        int outs = network.out.length;
//        if ((result == null) || (result.length!= outs))
//                result = new double[outs];
//
//        for (int i = 0; i < input.length; i++) {
//            network.in[i].setActivation(input[i]);
//        }
//
//        // Request calculation of outputs
//        for (int i = 0; i < result.length; i++) {
//            final FreeformNeuron o = network.out[i];
//            o.performCalculation();
//            result[i] = o.getActivation();
//        }
//
//        return result;
//    }
//
//
////    /**
////	 * Process training batches.
////	 */
////	protected void processBatches() {
////		int lastLearn = 0;
////		final ErrorCalculation errorCalc = new ErrorCalculation();
////		//this.visited.clear();
////
////		for (final MLDataPair pair : this.training) {
////			final MLData input = pair.getInput();
////			final MLData ideal = pair.getIdeal();
////			final MLData actual = this.network.compute(input);
////			final double sig = pair.getSignificance();
////
////			errorCalc.updateError(actual.getData(), ideal.getData(), sig);
////
////            process(ideal, actual, sig);
////
////            // Are we at the end of a batch.
////			lastLearn++;
////			if( lastLearn>=this.batchSize ) {
////				lastLearn = 0;
////				learn();
////			}
////		}
////
////		// Handle any remaining data.
////		if( lastLearn>0 ) {
////			learn();
////		}
////
////		// Set the overall error.
////        this.error = errorCalc.calculate();
////
////    }
//
//    private void process(double[] ideal, double[] actual, double sig) {
//        for (int i = 0; i < this.network.out.length; i++) {
//            final double diff = (ideal[i] - actual[i]) * sig;
//            final FreeformNeuron neuron = this.network.out[i];
//            calculateOutputDelta(neuron, diff);
//            calculateNeuronGradient(neuron);
//        }
//    }
//
//    /**
//	 * Learn for the entire network.
//	 */
//	protected void learn() {
//		this.network.eachEdge(connection -> {
//            learnConnection(connection);
//            connection.setTempTraining(0, 0.0);
//        });
//	}
//
//	/**
//	 * Learn for a single connection.
//	 * @param connection The connection to learn from.
//	 */
//	protected abstract void learnConnection(FreeformConnection connection);
//
//
//
//
//
//}

/*
 * Encog(tm) Core v3.3 - Java Version
 * http://www.heatonresearch.com/encog/
 * https://github.com/encog/encog-java-core
 
 * Copyright 2008-2014 Heaton Research, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *   
 * For more information on Heaton Research copyrights, licenses 
 * and trademarks visit:
 * http://www.heatonresearch.com/copyright
 */
package nars.learn.freeform;

import org.encog.neural.freeform.FreeformConnection;
import org.jetbrains.annotations.NotNull;

/**
 * From Encog https://github.com/encog/encog-java-core
 * Perform backpropagation for a freeform neural network.
 */
public class FreeformBackPropagation extends FreeformPropagationTraining		{


	/**
	 * The learning rate.  The coefficient for how much of the gradient is applied to each weight.
	 */
	private final double learningRate;
	
	/**
	 * The momentum.  The coefficient for how much of the previous delta is applied to each weight.  
	 * In theory, prevents local minima stall.
	 */
	private final double momentum;

	/**
	 * Construct a back propagation trainer.
	 * @param n The network to train.
	 * @param learningRate The learning rate. The coefficient for how much of the previous delta is applied to each weight.
	 * In theory, prevents local minima stall.
	 * @param momentum The momentum.
	 */
	public FreeformBackPropagation(final FreeformNetwork n,
								   final double learningRate,
								   final double momentum) {
		super(n);
		this.learningRate = learningRate;
		this.momentum = momentum;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void learnConnection(@NotNull final FreeformConnection connection) {
		final double gradient = connection.getTempTraining(0);
		final double delta = (gradient * this.learningRate)
				+ (connection.getTempTraining(1) * this.momentum);
		connection.setTempTraining(1, delta);
		connection.addWeight(delta);
	}


}

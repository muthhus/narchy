package nars.util.data;

import nars.NAR;
import org.jetbrains.annotations.NotNull;

/**
 * 
 * @author me
 */
public abstract class VectorMap {

	@NotNull
	public final UniformVector input;
	@NotNull
	public final UniformVector output;

	protected VectorMap(NAR n, String prefix, int numInputs,
			float inputPriority, int numOutputs, float outputPriority) {
		input = new UniformVector(n, prefix + "_i", new float[numInputs])
				.setPriority(inputPriority);
		output = new UniformVector(n, prefix + "_o", new float[numOutputs])
				.setPriority(outputPriority);

	}

	public void update() {
		map(input.data, output.data);
		input.update();
		output.update();
	}

	protected abstract void map(float[] in, float[] out);

}

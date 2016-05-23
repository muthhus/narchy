package nars.learn.lstm;

import java.util.Random;
import java.util.function.Consumer;

public class DistractedSequenceRecall extends AbstractTraining {
	static final int observation_dimension = 10;
	static final int action_dimension = 4;
	int len = 22;
	int width = 4;

	public DistractedSequenceRecall(Random r, int tests) {
		super(r, observation_dimension, action_dimension);

		this.tests = tests;
	}

	@Override
	protected void interact(Consumer<Interaction> each) {


		for (int i = 0; i < this.tests; i++) {


			int[] seq = new int[len];

			int target1 = random.nextInt(width);
			int target2 = random.nextInt(width);
			for (int t = 0; t < len; t++) {
				seq[t] = random.nextInt(width) + width;//+4 so as not to overlap with target symbols
			}
			int loc1 = random.nextInt(len);
			int loc2 = random.nextInt(len);
			while (loc1 == loc2)
				loc2 = random.nextInt(len);
			if (loc1 > loc2) {
				int temp = loc1;
				loc1 = loc2;
				loc2 = temp;
			}
			seq[loc1] = target1;
			seq[loc2] = target2;

			for (int t = 0; t < seq.length; t++) {
				double[] input = new double[observation_dimension];
				input[seq[t]] = 1.0;

				Interaction inter = new Interaction();
				if (t == 0)
					inter.do_reset = true;
				inter.actual = input;
				each.accept(inter);

			}

			//final 2 steps
			double[] input1 = new double[observation_dimension];
			input1[8] = 1.0;
			double[] target_output1 = new double[action_dimension];
			target_output1[target1] = 1.0;
			Interaction inter1 = new Interaction();
			inter1.actual = input1;
			inter1.expected = target_output1;
			each.accept(inter1);

			double[] input2 = new double[observation_dimension];
			input2[9] = 1.0;
			double[] target_output2 = new double[action_dimension];
			target_output2[target2] = 1.0;
			Interaction inter2 = new Interaction();
			inter2.actual = input2;
			inter2.expected = target_output2;
			each.accept(inter2);
		}

	}
}
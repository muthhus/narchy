package nars.learn.lstm;

import nars.util.data.random.XORShiftRandom;
import nars.util.data.random.XorShift128PlusRandom;

import java.util.Random;

public class Test {
	public static void main(String[] args) throws Exception {
		
		System.out.println("Test of SimpleLSTM\n");
		
		Random r = new XorShift128PlusRandom(1234);
		DistractedSequenceRecall task = new DistractedSequenceRecall(r, 1000);


		int cell_blocks = 5;
		double learningRate = 0.07;
		int epochs = 5000;

		SimpleLSTM slstm = new SimpleLSTM(r,
				task.getInputDimension(),
				task.getOutputDimension(),
				cell_blocks,
				learningRate);


		for (int epoch = 0; epoch < epochs; epoch++) {
			double fit = task.scoreSupervised(slstm);
			if (epoch % 10 == 0)
				System.out.println("["+epoch+"] error = " + (1 - fit));
		}
		System.out.println("done.");
		slstm.print(System.out);
	}

}

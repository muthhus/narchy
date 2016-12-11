package nars.learn.lstm.test;

import jcog.data.random.XorShift128PlusRandom;
import nars.learn.lstm.DistractedSequenceRecall;
import nars.learn.lstm.SimpleLSTM;

import java.util.Random;

public class TestLSTM1 {

	public static void main(String[] args) throws Exception {
		
		System.out.println("Test of SimpleLSTM\n");
		
		Random r = new XorShift128PlusRandom(1234);

		DistractedSequenceRecall task = new DistractedSequenceRecall(r, 12, 3, 22, 1000);

		int cell_blocks = 4;
		double learningRate = 0.05;
		SimpleLSTM slstm = task.lstm(cell_blocks);

		int epochs = 5000;
		for (int epoch = 0; epoch < epochs; epoch++) {
			double fit = task.scoreSupervised(slstm);
			if (epoch % 10 == 0)
				System.out.println("["+epoch+"] error = " + (1 - fit));
		}
		System.out.println("done.");
	}

}

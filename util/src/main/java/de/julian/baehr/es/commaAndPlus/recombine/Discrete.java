package de.julian.baehr.es.commaAndPlus.recombine;

import java.util.Arrays;
import java.util.Random;

public class Discrete implements IRecombinator{

	private Random random = new Random();
	
	@Override
	public double[] recombine(double[] v1, double[] v2) {
		double[] result = Arrays.copyOf(v1, v1.length);
		
		for(int i = 0; i < result.length; i++)
			if(random.nextBoolean())
				result[i] = v2[i];
		
		return result;
	}
}

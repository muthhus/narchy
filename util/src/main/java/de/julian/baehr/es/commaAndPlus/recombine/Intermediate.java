package de.julian.baehr.es.commaAndPlus.recombine;

import java.util.Arrays;

public class Intermediate implements IRecombinator{

	@Override
	public double[] recombine(double[] v1, double[] v2) {

		double[] result = Arrays.copyOf(v1, v1.length);
		
		for(int i = 0; i < v1.length; i++)
			result[i] += .5d * (v2[i] - v1[i]);
		
		return result;
	}
}

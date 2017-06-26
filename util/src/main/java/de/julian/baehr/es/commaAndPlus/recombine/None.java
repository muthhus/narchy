package de.julian.baehr.es.commaAndPlus.recombine;

public class None implements IRecombinator{

	@Override
	public double[] recombine(double[] v1, double[] v2) {
		return v1.clone();
	}

}

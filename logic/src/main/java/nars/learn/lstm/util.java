package nars.learn.lstm;

public class util 
{

	public static int argmin(double[] vec) {
		int result = -1;
		double min = Double.POSITIVE_INFINITY;
		int l = vec.length;
		for (int i = 0; i < l; i++) {
			final double v = vec[i];
			if (v < min)  {
				min = v;
				result = i;
			}
		}
		return result;
	}
}

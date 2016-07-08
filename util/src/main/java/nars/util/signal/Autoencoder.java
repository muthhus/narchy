package nars.util.signal;

import nars.util.Util;

import java.util.Random;

/**
 * Denoising Autoencoder (from DeepLearning.net)
 * 
 * TODO parameter for activation function (linear, sigmoid, etc..)
 */
public class Autoencoder {

	public final int n_visible;
	public final int n_hidden;
	public final float[][] W;
	private final float[] hbias;
	private final float[] vbias;
	private final Random rng;
	private float[] tilde_x;
	public float[] y;
	private float[] z;
	private float[] L_vbias;
	private float[] L_hbias;

	private float uniform(float min, float max) {
		return rng.nextFloat() * (max - min) + min;
	}

	/*
	 * public float binomial(final int n, final float p) { if (p < 0 || p > 1)
	 * { return 0; }
	 * 
	 * int c = 0; float r;
	 * 
	 * for (int i = 0; i < n; i++) { r = rng.nextfloat(); if (r < p) { c++; } }
	 * 
	 * return c; }
	 */


	public Autoencoder(int n_visible, int n_hidden, Random r) {
		this(n_visible, n_hidden, null, null, null, r);
	}

	public Autoencoder(int n_visible, int n_hidden, float[][] W,
			float[] hbias, float[] vbias, Random rng) {
		this.n_visible = n_visible;
		this.n_hidden = n_hidden;

		this.rng = rng;

		if (W == null) {
			this.W = new float[n_hidden][n_visible];
			float a = 1.0f / this.n_visible;

			for (int i = 0; i < this.n_hidden; i++) {
				for (int j = 0; j < this.n_visible; j++) {
					this.W[i][j] = uniform(-a, a);
				}
			}
		} else {
			this.W = W;
		}

		if (hbias == null) {
			this.hbias = new float[n_hidden];
			for (int i = 0; i < this.n_hidden; i++) {
				this.hbias[i] = 0;
			}
		} else {
			this.hbias = hbias;
		}

		if (vbias == null) {
			this.vbias = new float[n_visible];
			for (int i = 0; i < this.n_visible; i++) {
				this.vbias[i] = 0;
			}
		} else {
			this.vbias = vbias;
		}
	}

	private void addNoise(float[] x, float[] tilde_x, float maxNoiseAmount,
                          float corruptionRate) {

        Random r = this.rng;

		for (int i = 0; i < n_visible; i++) {
            if ((corruptionRate > 0) && (r.nextFloat() < corruptionRate)) {
				tilde_x[i] = 0;
			} else if (maxNoiseAmount > 0) {
				float nx = x[i] + (r.nextFloat() - 0.5f) * maxNoiseAmount;
				if (nx < 0)
					nx = 0;
				if (nx > 1)
					nx = 1;
				tilde_x[i] = nx;
			}
		}
	}

	// Encode
	public float[] encode(float[] x, float[] y, boolean sigmoid,
			boolean normalize) {

		float[][] W = this.W;

		if (y == null)
			y = new float[n_hidden];

		int nv = n_visible;
		int nh = n_hidden;
		float[] hbias = this.hbias;

		float max = 0, min = 0;
		for (int i = 0; i < nh; i++) {
			float yi = hbias[i];
			float[] wi = W[i];

			for (int j = 0; j < nv; j++) {
				yi += wi[j] * x[j];
			}

			if (sigmoid)
				yi = Util.sigmoid(yi);

			if (i == 0)
				max = min = yi;
			else {
				if (yi > max)
					max = yi;
				if (yi < min)
					min = yi;
			}
			y[i] = yi;

		}

		if ((normalize) && (max != min)) {
			float maxMin = max-min;
			for (int i = 0; i < nh; i++) {
				y[i] = (y[i]-min) / maxMin;
			}

//to check unit result:
//			float len = cartesianLength(y);
//			if (len > 0) {
//				for (int i = 0; i < nh; i++) {
//					y[i] = y[i] / len;
//				}
//				System.out.println(Arrays.toString(y) + " " + len + " " + cartesianLength(y));
//			}

//			for (int i = 0; i < nh; i++) {
//				y[i] = (y[i] - min) / (max-min);
//			}
		}



		return y;
	}

	private float cartesianLength(float[] y) {
		float d = 0;
		for (float z : y) {
			d += z*z;
		}
		return (float)Math.sqrt(d);
	}

	// Decode
    private void get_reconstructed_input(float[] y, float[] z) {
		float[][] w = W;

		float[] vbias = this.vbias;
		int nv = n_visible;
		int nh = n_hidden;

		for (int i = 0; i < nv; i++) {
			float zi = vbias[i];

			for (int j = 0; j < nh; j++) {
				zi += w[j][i] * y[j];
			}

			zi = Util.sigmoid(zi);

			z[i] = zi;
		}
	}

	public float[] getOutput() {
		return y;
	}

	public float train(float[] x, float learningRate,
					   float noiseLevel, float corruptionRate,
					   boolean sigmoid) {
		if ((tilde_x == null) || (tilde_x.length != n_visible)) {
			tilde_x = new float[n_visible];
			z = new float[n_visible];
			L_vbias = new float[n_visible];
		}
		if (y == null || y.length != n_hidden) {
			y = new float[n_hidden];
			L_hbias = new float[n_hidden];
		}

		float[][] W = this.W;
		float[] L_hbias = this.L_hbias;
		float[] L_vbias = this.L_vbias;
		float[] vbias = this.vbias;

		if (noiseLevel > 0) {
			addNoise(x, tilde_x, noiseLevel, corruptionRate);
		} else {
			tilde_x = x;
		}

		float[] tilde_x = this.tilde_x;

		encode(tilde_x, y, sigmoid, true);

		get_reconstructed_input(y, z);

		float error = 0;

		float[] zz = z;
		// vbias
		for (int i = 0; i < n_visible; i++) {

			float lv = L_vbias[i] = x[i] - zz[i];

			error += lv * lv; // square of difference

			vbias[i] += learningRate * lv;
		}

		error /= n_visible;

		int n = n_visible;
		float[] y = this.y;
		float[] hbias = this.hbias;
		int nh = n_hidden;

		// hbias
		for (int i = 0; i < nh; i++) {
			L_hbias[i] = 0;
			float[] wi = W[i];

			float lbi = 0;
			for (int j = 0; j < n; j++) {
				lbi += wi[j] * L_vbias[j];
			}
			L_hbias[i] += lbi;

			float yi = y[i];
			L_hbias[i] *= yi * (1 - yi);
			hbias[i] += learningRate * L_hbias[i];
		}

		// W
		for (int i = 0; i < nh; i++) {
			float yi = y[i];
			float lhb = L_hbias[i];
			float[] wi = W[i];
			for (int j = 0; j < n; j++) {
				wi[j] += learningRate * (lhb * tilde_x[j] + L_vbias[j] * yi);
			}
		}

		return error;
	}

	public float[] reconstruct(float[] x, float[] z) {
		float[] y = new float[n_hidden];

		encode(x, y, true, false);
		get_reconstructed_input(y, z);

		return z;
	}

	/**
	 * finds the index of the highest output value, or returns a random one if
	 * none are
	 */
	public int max() {

		float m = Float.NEGATIVE_INFINITY;
		int best = -1;
		float[] y = this.y;
		for (int i = 0; i < y.length; i++) {
			float Y = y[i];
			if (Y > m) {
				m = Y;
				best = i;
			}
		}
		return best == -1 ? (int) (rng.nextFloat() * y.length) : best;
	}
}

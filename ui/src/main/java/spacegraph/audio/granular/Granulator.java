package spacegraph.audio.granular;

public class Granulator {

	private final float[] sourceBuffer;
	public final float sampleRate;
	private final int grainSizeSamples;
	private final GrainWindow window;

	// public Granulator(SonarSample source, float grainSizeSecs, float
	// windowSizeFactor) {
	// this(source.buf, source.rate, grainSizeSecs, windowSizeFactor);
	// }

	public Granulator(float[] sourceBuffer, float sampleRate,
			float grainSizeSecs, float windowSizeFactor) {
		this.sourceBuffer = sourceBuffer;
		grainSizeSamples = Math.round(sampleRate * grainSizeSecs);

		window = new HanningWindow(Math.round(sampleRate * grainSizeSecs
				* windowSizeFactor));
		// this.window = new NullWindow(Math.round(sampleRate * grainSizeSecs *
		// windowSizeFactor));

		this.sampleRate = sampleRate;
	}

	public boolean hasMoreSamples(long[] grain, long now) {
		long length = grain[1];
		long showTime = grain[2];
		return now < showTime + length + window.getSize();
	}

	public float getSample(long[] grain, long now) {
		long startIndex = grain[0];
		//long length = grain[1];
		long showTime = grain[2];

		float[] sb = sourceBuffer;

		long offset = now - showTime;
		int sourceIndex = (int) ((startIndex + offset + sb.length) % sb.length);
		while (sourceIndex < 0)
			sourceIndex += sb.length;
		return sb[sourceIndex] * window.getFactor( ((int)offset) );
				//(offset <= 0) ? ((int) offset) : ((int) (offset - length)));
	}

	public static boolean isFading(long[] grain, long now) {
		long length = grain[1];
		long showTime = grain[2];
		return now > showTime + length;
	}

	public long[] nextGrain(long[] grain, int startIndex, float fadeInTime) {
		if (grain == null)
			grain = new long[3];
		int ws = window.getSize();
		grain[0] = (startIndex + ws) % sourceBuffer.length;
		grain[1] = grainSizeSamples;
		grain[2] = Math.round(fadeInTime + ws);
		return grain;
	}

}

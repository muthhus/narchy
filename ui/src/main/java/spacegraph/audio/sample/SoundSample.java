package spacegraph.audio.sample;

public class SoundSample {
	public final float[] buf;
	public final float rate;

	public SoundSample(float[] buf, float rate) {
		this.buf = buf;
		this.rate = rate;
		// System.out.println("SonarSample: " + buf.length + " " + rate);
		// System.out.println(Arrays.toString(buf));
	}
}
package spacegraph.audio.granular;

import jcog.random.XorShift128PlusRandom;
import org.apache.commons.lang3.mutable.MutableFloat;
import spacegraph.audio.SoundProducer;
import spacegraph.audio.sample.SonarSample;

import java.util.Random;

public class Granulize extends Granulator implements SoundProducer, SoundProducer.Amplifiable {

	private final float[] sourceBuffer;
	private float now;
	private float playTime;

    /** this actually represents the target amplitude which the current amplitude will continuously interpolate towards */
    public final MutableFloat amplitude = new MutableFloat(1.0f);
	public final MutableFloat stretchFactor = new MutableFloat(1.0f);
	public final MutableFloat pitchFactor = new MutableFloat(1.0f);

    protected float currentAmplitude = amplitude.floatValue();


    /** grains are represented as a triple of long integers (see Granulator.createGrain() which constructs these) */
	private long[] currentGrain;
	private long[] fadingGrain;

	private boolean isPlaying;
	private int playOffset = -1;

	final Random rng = new XorShift128PlusRandom(1);



    public Granulize(SonarSample s, float grainSizeSecs, float windowSizeFactor) {
        this(s.buf, s.rate, grainSizeSecs, windowSizeFactor);
    }

	public Granulize(float[] buffer, float sampleRate, float grainSizeSecs, float windowSizeFactor) {
		super(buffer, sampleRate, grainSizeSecs, windowSizeFactor);

		sourceBuffer = buffer;

		play();
	}

	public Granulize at(int pos) {
		playOffset = pos;
		return this;
	}

	public void process(float[] output, int readRate) {
		if (currentGrain == null && isPlaying) {
			currentGrain = nextGrain(null);
		}
        float dNow = ((sampleRate / readRate)) * pitchFactor.floatValue();

        float amp = currentAmplitude;
        float dAmp = (amplitude.floatValue() - amp) / output.length;

        float n = now;


        boolean p = isPlaying;
        if (!p)
            dAmp = (0 - amp) / output.length; //fade out smoothly if isPlaying false

        long samples = output.length;

        long[] cGrain = currentGrain;
        long[] fGrain = fadingGrain;

		for (int i = 0; i < samples; i++ ) {
            float nextSample = 0;
            long lnow = Math.round(n);
			if (cGrain != null) {
				nextSample = getSample(cGrain, lnow);
				if (Granulator.isFading(cGrain, lnow)) {
					fGrain = cGrain;
					cGrain = p ? nextGrain(cGrain) : null;
				}
			}
			if (fGrain != null) {
                nextSample += getSample(fGrain, lnow);
				if (!hasMoreSamples(fGrain, lnow)) {
					fGrain = null;
				}
			}
			n += dNow;
            output[i] = nextSample * amp;
            amp += dAmp;
		}


        //access and modify these fields only outside of the critical rendering loop
        currentGrain = cGrain;
        fadingGrain = fGrain;
        now = n;
        currentAmplitude = amp;
	}

    @Override
	public final void setAmplitude(float amplitude) {
        this.amplitude.setValue(amplitude);
    }

    @Override
    public float getAmplitude() {
        return amplitude.floatValue();
    }


    public void play() {
		playOffset = Math.abs(rng.nextInt());
		playTime = now;
		isPlaying = true;
	}

	/** continue */
	public void cont() {
		isPlaying = true;
	}

	@Override
	public String toString() {
		return "Granulize{" +
				"sampleRate=" + sampleRate +
				", now=" + now +
				", playTime=" + playTime +
				", amplitude=" + amplitude +
				", stretchFactor=" + stretchFactor +
				", pitchFactor=" + pitchFactor +
				", isPlaying=" + isPlaying +
				", playOffset=" + playOffset +
				'}';
	}

	@Override
	public void stop() {
		isPlaying = false;
	}

	private long[] nextGrain(long[] targetGrain) {
		//System.out.println("create grain: " + calculateCurrentBufferIndex() + " " + now);
        targetGrain = nextGrain(targetGrain, calculateCurrentBufferIndex(), now);
        return targetGrain;
	}

	private int calculateCurrentBufferIndex() {
        float sf = stretchFactor.floatValue();

		return Math.abs(Math.round(playOffset + (now - playTime) / sf)) % sourceBuffer.length;
	}

	public Granulize setStretchFactor(float stretchFactor) {
//		playOffset = calculateCurrentBufferIndex();
//		playTime = now;
		this.stretchFactor.setValue(stretchFactor);
        return this;
	}

    @Override
    public float read(float[] buf, int readRate) {
        process(buf, readRate);
        return 0.0f;
    }

    @Override
    public void skip(int samplesToSkip, int readRate) {
        //TODO
    }

    @Override
    public boolean isLive() {
        return isPlaying;
    }



}

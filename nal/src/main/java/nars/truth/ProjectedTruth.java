package nars.truth;

import org.jetbrains.annotations.NotNull;

/** subclass used as an indicator that it was the result of projection */
public final class ProjectedTruth extends DefaultTruth {

	public final long when;

	public ProjectedTruth(Truth t, long when) {
		this(t.freq(), t.conf(), when);
	}

	public ProjectedTruth(float f, float c, long when) {
		super(f, c);
		this.when = when;
	}

	@NotNull
	@Override
	public Truth confMult(float factor) {
		return factor == 1 ? this : new ProjectedTruth(freq, conf() * factor, when);
	}

	// public ProjectedTruth(Truth cloned, long occurrenceTime) {
	// this(cloned.getFrequency(), cloned.getConfidence(), occurrenceTime);
	// }

	// public long getTargetTime() { return target; }
}

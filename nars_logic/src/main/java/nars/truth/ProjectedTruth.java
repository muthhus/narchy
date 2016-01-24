package nars.truth;

/** subclass used as an indicator that it was the result of projection */
public final class ProjectedTruth extends DefaultTruth {

	public final long when;

	public ProjectedTruth(float f, float c, long when) {
		super(f, c);
		this.when = when;
	}

	// public ProjectedTruth(Truth cloned, long occurrenceTime) {
	// this(cloned.getFrequency(), cloned.getConfidence(), occurrenceTime);
	// }

	// public long getTargetTime() { return target; }
}

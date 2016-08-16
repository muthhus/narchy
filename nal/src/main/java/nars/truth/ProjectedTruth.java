package nars.truth;

import nars.Param;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** subclass used as an indicator that it was the result of projection */
public final class ProjectedTruth extends DefaultTruth {

	public final long when;

	public ProjectedTruth(@NotNull Truth t, long when) {
		this(t.freq(), t.conf(), when);
	}

	public ProjectedTruth(float f, float c, long when) {
		super(f, c);
		this.when = when;
	}

	@Nullable
	@Override
	public Truth confMult(float factor) {
		float newConf = conf() * factor;

		if (newConf < Param.TRUTH_EPSILON)
			return null;

		return factor == 1 ? this : new ProjectedTruth(freq, newConf, when);
	}

	// public ProjectedTruth(Truth cloned, long occurrenceTime) {
	// this(cloned.getFrequency(), cloned.getConfidence(), occurrenceTime);
	// }

	// public long getTargetTime() { return target; }
}

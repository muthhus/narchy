package nars.premise;

import nars.truth.Truth;

/**
 * Created by me on 2/22/17.
 */
public final class TruthPuncEvidence {
    public final Truth truth;
    public final byte punc;
    public final long[] evidence;

    public TruthPuncEvidence(Truth truth, byte punc, long[] evidence) {
        this.truth = truth;
        this.punc = punc;
        this.evidence = evidence;
    }

}

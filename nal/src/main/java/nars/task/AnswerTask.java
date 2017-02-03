package nars.task;

import jcog.Util;
import nars.NAR;
import nars.Task;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 7/3/16.
 */
public class AnswerTask extends MutableTask {


    @Nullable
    protected Task aBelief, bBelief;

    public AnswerTask(@NotNull Termed<Compound> term, char punc, Truth conclusion, long creationTime, long start, long end, long[] evidence) {
        super(term, punc, conclusion);
        evidence(evidence);
        time(creationTime, start, end);
    }

    public AnswerTask(@NotNull Termed<Compound> term, @NotNull Task aBelief, @NotNull Task bBelief, Truth conclusion, long creationTime, long start, long end, float evidenceBalance) {
        this(term, aBelief.punc(), conclusion, creationTime, start, end, Stamp.zip(aBelief.evidence(), bBelief.evidence(), evidenceBalance));

        this.aBelief = aBelief;
        this.bBelief = bBelief;
    }

    /**
     * rather than store weakrefs to these tasks, just use normal refs but be sure to nullify them before returning from onConcept
     */
    public void unlink() {
        this.aBelief = this.bBelief = null;
    }

    @Override
    public final boolean isInput() {
        return false;
    }

    @Nullable
    @Override
    public Task getParentTask() {
        return aBelief;
    }

    @Nullable
    @Override
    public Task getParentBelief() {
        return bBelief;
    }

    @Nullable
    public AnswerTask budget(@NotNull Task a, @NotNull Task b) {
        float ae = a.evi();
        return budget(a, b, ae / (ae + b.evi()));
    }

    @Nullable
    public final AnswerTask budget(@NotNull Task a, @NotNull Task b, float aMix) {
        float priSum = a.pri() + b.pri();
        boolean deleted = priSum != priSum;
        float newPri = !deleted ? Util.unitize(priSum) : Float.NaN;

        budgetSafe(
                newPri,
                !deleted ? Util.lerp(aMix, a.qua(), b.qua()) : 0
        );
        return !deleted ? this : null;
    }

    @Override
    public void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar) {
        unlink();
    }

    @Override
    public boolean delete() {
        if (super.delete()) {
            unlink();
            return true;
        }
        return false;
    }
}
package nars.task;

import jcog.Util;
import nars.NAR;
import nars.Task;
import nars.term.Compound;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jcog.Util.lerp;

/**
 * Created by me on 7/3/16.
 */
public class AnswerTask extends ImmutableTask {


//    @Nullable
//    protected Task aBelief, bBelief;

    public AnswerTask(@NotNull Compound term, byte punc, Truth conclusion, long creationTime, long start, long end, long[] evidence) {
        super(term, punc, conclusion, creationTime, start, end, evidence);
    }

    public AnswerTask(@NotNull Compound term, @NotNull Task aBelief, @NotNull Task bBelief, Truth conclusion, long creationTime, long start, long end, float evidenceBalance) {
        this(term, aBelief.punc(), conclusion, creationTime, start, end, Stamp.zip(aBelief.stamp(), bBelief.stamp(), evidenceBalance));

        //this.aBelief = null; aBelief;
        //this.bBelief = null; //bBelief;
    }

    /**
     * rather than store weakrefs to these tasks, just use normal refs but be sure to nullify them before returning from onConcept
     */
    @Deprecated public void unlink() {

        //this.aBelief = this.bBelief = null;
        }

    @Override
    public final boolean isInput() {
        return false;
    }

    @Nullable
    @Override
    public Task getParentTask() {
        return null; //return aBelief;
    }

    @Nullable
    @Override
    public Task getParentBelief() {
        return null; //return bBelief;
    }

    @Nullable
    public AnswerTask budget(@NotNull Task a, @NotNull Task b, int dur) {
        float ae = a.evi(dur);
        return budget(a, b, ae / (ae + b.evi(dur)));
    }

    @Nullable
    public final AnswerTask budget(@NotNull Task a, @NotNull Task b, float aMix) {
        budgetSafe(
            Util.unitize(
                a.priSafe(0) +
                   b.priSafe(0)
            ),
            lerp(aMix, a.qua(), b.qua())
        );
        return this;
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
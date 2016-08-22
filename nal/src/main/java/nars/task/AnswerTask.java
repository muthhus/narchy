package nars.task;

import nars.NAR;
import nars.Task;
import nars.budget.merge.BudgetMerge;
import nars.concept.Concept;
import nars.concept.TruthDelta;
import nars.nal.Stamp;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 7/3/16.
 */
public class AnswerTask extends MutableTask {

    @Nullable
    protected Task aBelief, bBelief;

    public AnswerTask(@NotNull Termed<Compound> term, @NotNull Task aBelief, @NotNull Task bBelief, Truth conclusion, long creationTime, long occTime, float aMix) {
        super(term, aBelief.punc(), conclusion);

        this.aBelief = aBelief;
        this.bBelief = bBelief;

        //evidence(Stamp.zip(aBelief.evidence(), bBelief.evidence()));
        evidence( Stamp.zip(aBelief.evidence(), bBelief.evidence(), aMix) );

        time(creationTime, occTime);
    }

    /** rather than store weakrefs to these tasks, just use normal refs but be sure to nullify them before returning from onConcept */
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

    @NotNull
    public AnswerTask budget(@NotNull Task a, @NotNull Task b) {
        float acw = a.confWeight();
        float aMix = acw / (acw + b.confWeight());
        budget(a, b, aMix);
        //if (isDeleted())
            //throw new RuntimeException("budget mix resulted in deletion");
        return this;
    }

    @NotNull
    public AnswerTask budget(@NotNull Task a, @NotNull Task b, float aMix) {
        if (!b.isDeleted() && !a.isDeleted()) {
            budget(b.budget());
            BudgetMerge.plusBlend.merge(budget(), a.budget(), aMix);
        } else {
            delete();
        }
        return this;
    }

    @Override
    public void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar) {
        unlink();
    }

    @Override
    public boolean delete() {
        unlink();
        return super.delete();
    }
}
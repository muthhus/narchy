package nars.task;

import nars.budget.merge.BudgetMerge;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 7/3/16.
 */
public class AnswerTask extends MutableTask {

    protected Task aBelief, bBelief;

    public AnswerTask(@NotNull Termed<Compound> term, @NotNull Task aBelief, Task bBelief, Truth conclusion, long creationTime, long occTime, float aMix) {
        super(term, aBelief.punc(), conclusion);

        this.aBelief = aBelief;
        this.bBelief = bBelief;

        //evidence(Stamp.zip(aBelief.evidence(), bBelief.evidence()));
        evidence( Stamp.zip(bBelief.evidence(), bBelief.evidence(), aMix) );

        time(creationTime, occTime);
    }

    public AnswerTask budget(Task a, Task b) {
        float acw = a.confWeight();
        float aMix = acw / (acw + b.confWeight());
        budget(a, b, aMix);
        return this;
    }

    public AnswerTask budget(Task a, Task b, float aMix) {
        if (!b.isDeleted() && !a.isDeleted()) {
            budget(b.budget());
            BudgetMerge.plusDQBlend.merge(budget(), a.budget(), aMix);
        } else {
            delete();
        }
        return this;
    }



}
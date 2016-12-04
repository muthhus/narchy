package nars.concept.dynamic;

import nars.task.RevisionTask;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 12/4/16.
 */
public class DynamicBeliefTask extends RevisionTask {


    public DynamicBeliefTask(@NotNull Termed<Compound> term, char punc, Truth conclusion, long creationTime, long occTime, long[] evidence) {
        super(term, punc, conclusion, creationTime, occTime, evidence);
    }
}

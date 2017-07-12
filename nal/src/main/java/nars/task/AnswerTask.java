package nars.task;

import nars.Task;
import nars.term.Compound;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 7/3/16.
 */
public class AnswerTask extends NALTask {


//    @Nullable
//    protected Task aBelief, bBelief;

    AnswerTask(@NotNull Compound term, byte punc, Truth conclusion, long creationTime, long start, long end, long[] evidence) {
        super(term, punc, conclusion, creationTime, start, end, evidence);

    }

    public AnswerTask(@NotNull Compound term, @NotNull Task aBelief, @NotNull Task bBelief, Truth conclusion, long creationTime, long start, long end, float evidenceBalance) {
        this(term, aBelief.punc(), conclusion, creationTime, start, end, Stamp.zip(aBelief.stamp(), bBelief.stamp(), evidenceBalance));
    }


    @Override
    public final boolean isInput() {
        return false;
    }


//    @Override
//    public boolean delete() {
//        if (super.delete()) {
//            unlink();
//            return true;
//        }
//        return false;
//    }
}
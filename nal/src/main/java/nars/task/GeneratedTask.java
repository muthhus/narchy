package nars.task;

import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class GeneratedTask extends ImmutableTask {

    public GeneratedTask(Compound term, byte punc, Truth truth, long creation, long start, long end, long[] evidence) {
        super(term, punc, truth, creation, start, end, evidence);
    }

    @Override
    public boolean isInput() {
        return false;
    }
}

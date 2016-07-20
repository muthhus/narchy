package nars.task;

import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class GeneratedTask extends MutableTask {

    public GeneratedTask(@NotNull Termed<Compound> term, char punct, @Nullable Truth truth) {
        super(term, punct, truth);
    }

    @Override
    public boolean isInput() {
        return false;
    }
}

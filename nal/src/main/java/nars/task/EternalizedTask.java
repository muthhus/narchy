package nars.task;

import nars.term.Compound;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 7/22/16.
 */
public final class EternalizedTask extends GeneratedTask {
    public EternalizedTask(@NotNull Compound term, char punc, @Nullable Truth t) {
        super(term, punc, t);
    }
}

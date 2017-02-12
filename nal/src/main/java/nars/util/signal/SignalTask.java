package nars.util.signal;

import nars.task.MutableTask;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 2/12/17.
 */
public class SignalTask extends MutableTask {


    public SignalTask(@NotNull Termed<Compound> t, char punct, @Nullable Truth truth, long start, long end) {
        super(t, punct, truth);
        time(start, start, end);
    }

}

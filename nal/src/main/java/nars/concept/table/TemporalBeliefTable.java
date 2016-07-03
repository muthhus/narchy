package nars.concept.table;

import nars.NAR;
import nars.bag.Table;
import nars.task.Task;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 5/7/16.
 */
public interface TemporalBeliefTable extends Table<Task,Task> {

    @Nullable Task top(long when);

    @Nullable Truth truth(long when, EternalTable eternal);

    @Nullable Task ready(Task input, EternalTable eternal, NAR nar);
}

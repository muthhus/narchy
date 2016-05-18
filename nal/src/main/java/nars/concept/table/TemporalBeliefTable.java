package nars.concept.table;

import nars.NAR;
import nars.bag.impl.ListTable;
import nars.task.Task;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by me on 5/7/16.
 */
public interface TemporalBeliefTable extends ListTable<Task,Task> {

    @Nullable Task top(long when);

    @Nullable Truth truth(long when);

    @Nullable Task ready(Task input, NAR nar);
}

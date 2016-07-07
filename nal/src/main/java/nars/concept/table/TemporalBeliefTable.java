package nars.concept.table;

import nars.NAR;
import nars.bag.Table;
import nars.task.Task;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Created by me on 5/7/16.
 */
public interface TemporalBeliefTable extends Table<Task,Task> {

    @Nullable Task strongest(long when, long now, Task against);

    @Nullable Truth truth(long when, long now, EternalTable eternal);

    @Nullable Task add(Task input, EternalTable eternal, NAR nar);

    void removeIf(Predicate<Task> o);

    public long min();
    public long max();

    void min(long minT);
    void max(long maxT);
}

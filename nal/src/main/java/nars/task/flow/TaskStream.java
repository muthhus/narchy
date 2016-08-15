package nars.task.flow;

import nars.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.stream.Stream;

public class TaskStream implements Input {

    @NotNull private final Iterator<Task> stream;

    public TaskStream(@NotNull Stream<Task> s) {
        this(s.iterator());
    }
    public TaskStream(@NotNull Iterator<Task> s) {
        stream = s;
    }

    @Nullable
    @Override
    public final Task get() {
        Iterator<Task> stream = this.stream;
        return stream.hasNext() ? stream.next() : null;
    }

}

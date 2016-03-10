package nars.task.flow;

import nars.task.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.stream.Stream;

public class TaskStream implements Input {

    private final Iterator<Task> stream;

    public TaskStream(@NotNull Stream<Task> s) {
        this(s.iterator());
    }
    public TaskStream(Iterator<Task> s) {
        stream = s;
    }

    @Nullable
    @Override
    public Task get() {
        Iterator<Task> stream = this.stream;
        if (!stream.hasNext()) return null;
        return stream.next();
    }

}

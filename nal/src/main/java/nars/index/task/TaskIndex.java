package nars.index.task;

import nars.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by me on 10/14/16.
 */
abstract public class TaskIndex {

    /**
     *
     * @param x
     * @return null if no existing task alredy present, non-null of the pre-existing one
     */
    @Nullable
    public abstract Task addIfAbsent(@NotNull Task x);

    public final void remove(@NotNull Task tt) {
        removeInternal(tt);
        tt.delete();
    }

    protected abstract void removeInternal(@NotNull Task tt);

    public abstract void clear();

    public final void remove(@NotNull List<Task> tt) {
        int s = tt.size();
        for (int i = 0; i < s; i++) {
            this.remove(tt.get(i));
        }
    }

    abstract public void forEach(Consumer<Task> each);

    public void change(List<Task> toAdd, List<Task> toRemove) {
        remove(toRemove);
        addIfAbsent(toAdd);
    }

    public void addIfAbsent(List<Task> toAdd) {
        for (int i = 0, toAddSize = toAdd.size(); i < toAddSize; i++) {
            addIfAbsent(toAdd.get(i));
        }
    }

    abstract public boolean contains(Task t);
}

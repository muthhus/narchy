package nars.index.task;

import nars.Task;
import nars.index.term.tree.TermKey;
import nars.util.MyConcurrentRadixTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Created by me on 10/14/16.
 */
public class TreeTaskIndex extends TaskIndex {


    public final MyConcurrentRadixTree<Task> tasks = new MyConcurrentRadixTree<>();

    @Override
    public @Nullable final Task addIfAbsent(@NotNull Task x) {

        Task y = tasks.putIfAbsent(key(x), x);
        return y == x ? null : y;
    }

    @Override
    public final void remove(@NotNull Task tt) {
        tasks.remove(key(tt));
    }

    static CharSequence key(Task x) {
        return new TermKey(x);
    }


    @Override
    public void clear() {
        tasks.clear();
    }

    @Override
    public void forEach(Consumer<Task> each) {
        tasks.forEach(each);
    }
}

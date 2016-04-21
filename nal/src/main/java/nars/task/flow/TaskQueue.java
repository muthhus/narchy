package nars.task.flow;

import nars.NAR;
import nars.task.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

/** an input that generates tasks in batches, which are stored in a buffer */
public class TaskQueue extends ArrayDeque<Task> implements Input , Consumer<Task> {

    //private On reg=null;

    public TaskQueue() {
        this(1);
    }

    public TaskQueue(int initialCapacity) {
        super(initialCapacity);
    }

    public TaskQueue(@NotNull Collection<Task> x) {
        super(x);
    }
    public TaskQueue(@NotNull Task[] x) {
        super(x.length);
        Collections.addAll(this, x);
    }

    @Override
    public final boolean add(@Nullable Task task) {
        if (task == null) return true;
        return super.add(task);
    }

    /*protected int accept(Iterator<Task> tasks) {
        if (tasks == null) return 0;
        int count = 0;
        while (tasks.hasNext()) {
            Task t = tasks.next();
            if (t==null)
                continue;
            queue.add(t);
            count++;
        }
        return count;
    }*/

    @Override
    public void accept(@Nullable Task task) {
        if (task==null) return;

        add(task);
    }

    @Nullable
    @Override
    public Task get() {
        if (!isEmpty()) {
            return removeFirst();
        }
        return null;
    }

    public void addIfNotNull(@Nullable Task task) {
        if (task!=null)
            add(task);
    }

    public void input(@NotNull NAR nar) {
        nar.input((Input)this);
    }


//    @Override
//    public void stop() {
//        clear();
//    }



}

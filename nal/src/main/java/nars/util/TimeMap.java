package nars.util;

import nars.NAR;
import nars.Task;
import nars.util.time.IntervalTree;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static java.lang.System.out;


public class TimeMap extends IntervalTree<Long, Task> implements Consumer<Task> {

    @NotNull
    private final NAR nar;

    public TimeMap(@NotNull NAR n) {
        this.nar = n;
        n.forEachConceptTask(this, true, true, false, false);
    }

    @Override
    public void accept(@NotNull Task task) {
        if (!task.isEternal()) {
            put(task.occurrence(), task);
        }
    }

    public void print() {
        out.println(nar.time() + ": " + "Total tasks: " + size() + "\t" + keySetSorted().toString());
    }


}

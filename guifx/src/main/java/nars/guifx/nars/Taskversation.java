package nars.guifx.nars;

import nars.Global;
import nars.NAR;
import nars.task.Task;
import nars.truth.Stamp;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Conversation of Tasks: Groups derivations by their corresponding input tasks
 */
public class Taskversation {

    private final NAR nar;

    final LinkedHashMap<Task,Consumer<Task>> tasks = new LinkedHashMap<>();

    public Taskversation(NAR n, Function<Task,Consumer<Task>> receiver) {
        this.nar = n;
        n.onTask(t -> {
            if (t.isInput()) {
                tasks.put(t, receiver.apply(t));
            } else {
                tasks.forEach( (i,r) -> {
                    if (relevant(i, t)) {
                        r.accept(t);
                    }
                });
            }
        });
    }

    protected boolean relevant(Task input, Task derivation) {
        return Stamp.overlapping(input, derivation);
        //TODO other kinds of relevancies
    }
}

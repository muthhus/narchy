package nars.op.sys;

import nars.nal.nal8.operator.ImmediateOperator;
import nars.task.Task;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Resets memory, @see memory.reset()
 */
public class reset extends ImmediateOperator {

    @NotNull
    @Override
    public String toString() {
        return "Reset";
    }

    @Override
    public void execute(@NotNull List<Task> e) {
        nar.reset();
        //nar.reset()
    }
}

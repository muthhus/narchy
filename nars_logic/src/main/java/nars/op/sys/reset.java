package nars.op.sys;

import nars.nal.nal8.operator.ImmediateOperator;
import nars.task.Task;
import org.jetbrains.annotations.NotNull;

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
    public void execute(@NotNull Task e) {
        nar.memory.clear();
        //nar.reset()
    }
}

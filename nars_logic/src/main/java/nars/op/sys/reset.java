package nars.op.sys;

import nars.Memory;
import nars.NAR;
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
        nar.reset();
        //nar.reset()
    }
}

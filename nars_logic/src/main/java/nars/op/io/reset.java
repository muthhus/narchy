package nars.op.io;

import nars.nal.nal8.Execution;
import nars.nal.nal8.operator.ImmediateOperator;
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
    public void execute(@NotNull Execution e) {
        e.nar.memory.clear();
        //nar.reset()
    }
}

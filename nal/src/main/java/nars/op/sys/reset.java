package nars.op.sys;

import nars.concept.OperationConcept;
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
    public void execute(@NotNull OperationConcept e) {
        nar.reset();
        //nar.reset()
    }
}

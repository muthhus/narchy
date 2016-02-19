package nars.op.out;

import nars.nal.nal8.Execution;
import nars.nal.nal8.Operator;
import nars.nal.nal8.operator.ImmediateOperator;
import nars.task.Task;
import org.jetbrains.annotations.NotNull;

/**
 * explicitly repeated input (repetition of the content of input ECHO commands)
 */
public class echo extends ImmediateOperator {

    @Override
    public void execute(@NotNull Task e) {
        nar.memory.eventSpeak.emit( Operator.opArgs(e.term()) );
    }

}

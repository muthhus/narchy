package nars.op.out;

import nars.term.Operator;
import nars.nal.nal8.operator.ImmediateOperator;
import nars.task.Task;
import org.jetbrains.annotations.NotNull;

/**
 * explicitly repeated input (repetition of the content of input ECHO commands)
 */
public class echo extends ImmediateOperator {

    @Override
    public void execute(@NotNull Task e) {
        nar.eventSpeak.emit( Operator.opArgs(e.term()) );
    }

}

package nars.op.out;

import nars.nal.nal8.operator.ImmediateOperator;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Operator;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * explicitly repeated input (repetition of the content of input ECHO commands)
 */
public class echo extends ImmediateOperator {

    @Override
    public void execute(@NotNull List<Task> e) {
        Compound t = e.get(0).term();
        nar.eventSpeak.emit( Operator.opArgs(t) );
    }

}

package nars.op.out;

import nars.concept.OperationConcept;
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
    public void execute(@NotNull OperationConcept t) {
        nar.eventSpeak.emit( Operator.opArgs(t) );
    }

}

package nars.derive.op;

import nars.$;
import nars.Op;
import nars.control.Derivation;
import nars.derive.AbstractPred;

/**
 * Created by me on 5/19/17.
 */
public final class TaskBeliefOp extends AbstractPred<Derivation> {
    private final byte op;
    private final boolean task;
    private final boolean belief;

    public TaskBeliefOp(Op op, boolean testTask, boolean testBelief) {
        super($.func("op", $.quote(op.str), $.the(testTask ? "1" : "0"), $.the(testBelief ? "1" : "0")));
        this.op = op.id;
        this.task = testTask;
        this.belief = testBelief;
    }

    @Override
    public boolean test(Derivation derivation) {
        if (task && derivation.termSub0op != op)
            return false;
        return !belief || derivation.termSub1op == op;
    }
}

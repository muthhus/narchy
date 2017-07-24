package nars.derive.op;

import nars.$;
import nars.control.Derivation;
import nars.derive.AbstractPred;


/**
 * Created by me on 5/19/17.
 */
public final class TaskBeliefHas extends AbstractPred<Derivation> {
    private final int structure;
    private final boolean task;
    private final boolean belief;

    public TaskBeliefHas(int structure, boolean testTask, boolean testBelief) {
        super($.func("opHas", $.the(structure), $.the(testTask ? "1" : "0"), $.the(testBelief ? "1" : "0")));
        this.structure = structure;
        this.task = testTask;
        this.belief = testBelief;
    }

    @Override
    public boolean test(Derivation derivation) {
        return (!task || derivation.taskTerm.hasAny(structure))
                &&
               (!belief || derivation.beliefTerm.hasAny(structure));
    }
}


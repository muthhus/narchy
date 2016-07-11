package nars.nal.meta.op;

import nars.$;
import nars.Op;
import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.PremiseEval;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

/** task term is NOT negation */
public final class TaskPositive extends AtomicBoolCondition {

    public static final TaskPositive the = new TaskPositive();
    public static final Term proto = $.$("task(positive)"); //used in permutation phase

    final static int negateOrd = Op.NEG.ordinal();

    @Override
    public boolean booleanValueOf(@NotNull PremiseEval e) {
        return e.termSub0op != negateOrd;// e.taskTerm.op()!= Op.NEGATE;
    }


    //    @Override
//    public boolean booleanValueOf(@NotNull PremiseEval m) {
//        Truth t = m.premise.task().truth();
//        return (t!=null && t.freq() >= 0.5f);
//    }

    @NotNull
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}

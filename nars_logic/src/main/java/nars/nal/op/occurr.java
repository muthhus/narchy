package nars.nal.op;


import nars.nal.PremiseMatch;
import nars.nal.meta.AtomicBooleanCondition;
import nars.nal.nal7.Tense;
import nars.process.ConceptProcess;
import nars.task.Task;
import nars.term.Term;

/**
 * occurrsRelative(target, variable, direction)
 * target: pass through
 * direction= +1, -1, 0
 * variable: term to modify occurrence relative to
 */
public class occurr extends AtomicBooleanCondition<PremiseMatch> {

    final boolean taskOrBelief, forward;
    final String str;

    public occurr(Term var1, Term var2) {
        taskOrBelief = var1.toString().equals("task"); //TODO check else case
        forward = var2.toString().equals("forward"); //TODO check else case
        str = getClass().getSimpleName() + ":(" +
                (taskOrBelief ? "task" : "belief") + "," +
                (forward ? "forward" : "reverse");
    }

    @Override
    public String toString() {
        return str;
    }

    @Override
    public boolean booleanValueOf(PremiseMatch m) {

        ConceptProcess p = m.premise;

        Task eventTask = taskOrBelief ? p.getTask() : p.getBelief();

        if (eventTask != null) {
            int t = eventTask.term().t();
            if (t != Tense.ITERNAL) {
                if (!forward) t = -t;
                m.occDelta.set(t);
                return true;
            }
        }

        return false;
    }
}

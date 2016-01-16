package nars.nal.op;


import nars.concept.ConceptProcess;
import nars.nal.Tense;
import nars.nal.meta.AtomicBooleanCondition;
import nars.nal.meta.PremiseMatch;
import nars.term.Term;
import nars.term.compound.Compound;
import org.jetbrains.annotations.NotNull;

/**
 * occurrsRelative(target, variable, direction)
 * target: pass through
 * direction= +1, -1, 0
 * variable: term to modify occurrence relative to
 */
public class occurr extends AtomicBooleanCondition<PremiseMatch> {

    final boolean taskOrBelief, forward;
    @NotNull
    final String str;

    public occurr(@NotNull Term var1, @NotNull Term var2) {
        taskOrBelief = var1.toString().equals("task"); //TODO check else case
        forward = var2.toString().equals("forward"); //TODO check else case
        str = getClass().getSimpleName() + ":(" +
                (taskOrBelief ? "task" : "belief") + "," +
                (forward ? "forward" : "reverse") + ")";
    }

    @NotNull
    @Override
    public String toString() {
        return str;
    }

    @Override
    public boolean booleanValueOf(@NotNull PremiseMatch m) {

        ConceptProcess p = m.premise;

        Term tt = taskOrBelief ? p.getTaskTerm() :
                p.getBeliefTerm().term();
                //p.getBelief()!=null ? p.getBelief().term() : null;


        if (tt != null && (tt instanceof Compound)) {
            int t = ((Compound)tt).t();
            if (t != Tense.ITERNAL) {
                if (!forward) t = -t;

                m.occDelta.set(t);
                return true;
            }
        }

        return true;
    }
}

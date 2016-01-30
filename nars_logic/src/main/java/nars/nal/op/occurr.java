package nars.nal.op;


import nars.concept.ConceptProcess;
import nars.nal.Tense;
import nars.nal.meta.AtomicBooleanCondition;
import nars.nal.meta.PremiseMatch;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

/**
 * occurrsRelative(target, variable, direction)
 * target: pass through
 * direction= +1, -1, 0
 * variable: term to modify occurrence relative to
 */
public class occurr extends AtomicBooleanCondition<PremiseMatch> {

    final boolean taskOrBelief;
    @NotNull
    final String str;

    final int mult;

    public occurr(@NotNull Term var1, @NotNull Term var2) {
        taskOrBelief = var1.toString().equals("task"); //TODO check else case

        switch (var2.toString()) {
            case "forward": mult = 1; break;
            case "reverse": mult = -1; break;
            case "zero": mult = 0; break;
            default:
                throw new RuntimeException("invalid occurrence multiplier parameter");
        }

        str = getClass().getSimpleName() + ":(" +
                (taskOrBelief ? "task" : "belief") + "," +
                mult + ")";
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
            int t = ((Compound) tt).t();
            if (t != Tense.ITERNAL) {
                int docc;
                if (mult!=0) {
                    docc = t * mult;
                } else {
                    docc = 0;
                }
                m.occDelta.set(docc);
            }
        }


        return true;
    }
}

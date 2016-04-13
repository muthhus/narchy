package nars.concept;

import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.Op;
import nars.concept.table.BeliefTable;
import nars.concept.table.DynamicBeliefTable;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

/**
 * Accelerated Conjunction/Disjunction for full or partial Boolean Logic emulation
 */
abstract public class JunctionConcept extends CompoundConcept {


    @NotNull
    public final NAR nar;

    public JunctionConcept(@NotNull String compoundTermString, @NotNull NAR n) throws Narsese.NarseseException {
        this((Compound) $.$(compoundTermString), n);
    }

    public JunctionConcept(@NotNull Compound c, @NotNull NAR n)  {
        super(c, n);
        if (term.op()!= Op.CONJUNCTION && term.op()!= Op.DISJUNCTION)
            throw new RuntimeException("must be && or || expression");

        n.on(this);

        this.nar = n;

        this.beliefs = newBeliefTable(0);
        this.goals = newGoalTable(0);

        //initial value
        solve(true, n.time(), n.time());
        solve(false, n.time(), n.time());
    }

    @NotNull @Override protected BeliefTable newBeliefTable(int cap) {
        return newTable(true);
    }
    @NotNull @Override protected BeliefTable newGoalTable(int cap) {
        return newTable(false);
    }

    @NotNull
    private BeliefTable newTable(boolean beliefOrGoal) {
        return new DynamicBeliefTable(this, nar) {
            @NotNull
            @Override protected Task update(long now) {
                return new MutableTask(term, '.').truth(solve(beliefOrGoal, now, now)).present(now).normalize(nar);
            }
        };
    }

    @NotNull
    abstract public Truth solve(boolean beliefOrGoal, long when, long now);

    public static class ConjunctionConcept extends JunctionConcept {

        public ConjunctionConcept(@NotNull Compound junction, @NotNull NAR n)  {
            super(junction, n);

        }


        @NotNull
        @Override
        public Truth solve(boolean beliefOrGoal, long when, long now) {
            //HACK todo use a real truth aggregation formula

            float f = 1f, c = 1f;
            for (Term t : terms()) {

                Concept subtermConcept = nar.concept(t);
                if (subtermConcept == null) {
                    f = c = 0;break;
                    //continue;
                }

                BeliefTable b = beliefOrGoal ? subtermConcept.beliefs() : subtermConcept.goals();
                Truth ct = b.truth(now);
                f *= ct.freq();
                c *= ct.conf();
            }
            return new DefaultTruth(f, c);
        }
    }
}

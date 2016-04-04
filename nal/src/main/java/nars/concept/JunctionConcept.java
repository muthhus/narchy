package nars.concept;

import com.google.common.collect.Iterators;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.Op;
import nars.concept.util.BeliefTable;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

/**
 * Accelerated Conjunction/Disjunction for full or partial Boolean Logic emulation
 */
abstract public class JunctionConcept extends CompoundConcept {


    abstract public class EvalBeliefTable implements BeliefTable {

        Task current = null;

        public EvalBeliefTable() {

        }

        @Nullable
        @Override
        public Task add(@NotNull Task input, NAR nar) {
            return current;
        }

        @Nullable
        @Override
        public Task topEternal() {
            return null;
        }

        @Nullable
        @Override
        public Task topTemporal(long when, long now) {
            return updateTask(now);
        }

        public Task updateTask(long now) {
            if (current == null || current.occurrence()!=now) {
                current = update(now);
            }
            return current;
        }

        abstract protected Task update(long now);

        @Nullable
        @Override
        public Truth truth(long when, long now, float dur) {
            return topTemporal(when, now).projectTruth(when, now, false);
        }

        @Override
        public int getCapacity() {
            return 0;
        }

        @Override
        public void setCapacity(int newCapacity) {

        }

        @Override
        public int size() {
            return isEmpty() ? 0 : 1;
        }

        @Override
        public void clear() {
            current = null;
        }

        @Override
        public boolean isEmpty() {
            return updateTask(nar.time())==null;
        }

        @Override
        public Iterator<Task> iterator() {
            return !isEmpty() ? Iterators.singletonIterator(current) : Iterators.emptyIterator();
        }
    }

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
        return new EvalBeliefTable() {
            @Override protected Task update(long now) {
                return new MutableTask(term, '.').truth(solve(beliefOrGoal, now, now)).present(now).normalize(nar);
            }
        };
    }

    abstract public Truth solve(boolean beliefOrGoal, long when, long now);

    public static class ConjunctionConcept extends JunctionConcept {

        public ConjunctionConcept(@NotNull Compound junction, @NotNull NAR n)  {
            super(junction, n);

        }


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
                Truth ct = b.truth(now, nar.duration());
                f *= ct.freq();
                c *= ct.conf();
            }
            return new DefaultTruth(f, c);
        }
    }
}

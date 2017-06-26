package nars.conceptualize;

import nars.NAR;
import nars.conceptualize.state.ConceptState;
import nars.table.BeliefTable;
import nars.table.TemporalBeliefTable;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Created by me on 3/23/16.
 */
public interface ConceptBuilder extends Function<Term, Termed> {




    @NotNull ConceptState init();
    @NotNull ConceptState awake();
    @NotNull ConceptState sleep();

    TemporalBeliefTable newTemporalBeliefTable(Compound c);

    void start(NAR nar);


    /** passes through terms without creating any concept anything */
    ConceptBuilder Null = new ConceptBuilder() {

        @Override
        public Termed apply(Term term) {
            return term;
        }

        @Override
        public @NotNull ConceptState init() {
            return ConceptState.Deleted;
        }

        @Override
        public @NotNull ConceptState awake() {
            return null;
        }

        @Override
        public @NotNull ConceptState sleep() {
            return null;
        }

        @Override
        public TemporalBeliefTable newTemporalBeliefTable(Compound c) {
            return null;
        }

        @Override
        public void start(NAR nar) {

        }

        @Override
        public BeliefTable newBeliefTable(Compound t, boolean beliefOrGoal) {
            return null;
        }

    };

    BeliefTable newBeliefTable(Compound t, boolean beliefOrGoal);
}

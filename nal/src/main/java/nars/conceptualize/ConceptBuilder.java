package nars.conceptualize;

import jcog.bag.Bag;
import nars.NAR;
import nars.conceptualize.state.ConceptState;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.table.TemporalBeliefTable;
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

    QuestionTable newQuestionTable();
    BeliefTable newBeliefTable(Term t, boolean beliefOrGoal);
    TemporalBeliefTable newTemporalBeliefTable(Term c);

    void start(NAR nar);


    /** passes through terms without creating any concept anything */
    ConceptBuilder Null = new ConceptBuilder() {

        @Override
        public Termed apply(Term term) {
            return term;
        }

        @Override
        public @NotNull ConceptState init() {
            return ConceptState.Abstract;
        }

        @Override
        public @NotNull ConceptState awake() {
            return ConceptState.Abstract;
        }

        @Override
        public @NotNull ConceptState sleep() {
            return ConceptState.Abstract;
        }

        @Override
        public void start(NAR nar) {

        }

        @Override
        public TemporalBeliefTable newTemporalBeliefTable(Term c) {
            return TemporalBeliefTable.Empty;
        }

        @Override
        public BeliefTable newBeliefTable(Term t, boolean beliefOrGoal) {
            return BeliefTable.Empty;
        }

        @Override
        public QuestionTable newQuestionTable() {
            return QuestionTable.Empty;
        }

        @Override
        public Bag[] newLinkBags(@NotNull Term term) {
            return new Bag[] { Bag.EMPTY, Bag.EMPTY };
        }
    };


    Bag[] newLinkBags(@NotNull Term term);
}

package nars.concept;

import nars.NAR;
import nars.Task;
import nars.conceptualize.DefaultConceptBuilder;
import nars.conceptualize.state.ConceptState;
import nars.table.BeliefTable;
import nars.table.HijackQuestionTable;
import nars.table.QuestionTable;
import nars.term.Compound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;
import static nars.table.QuestionTable.StorelessQuestionTable;

/**
 * concept of a compound term which can name a task, and thus have associated beliefs, goals, questions, and quests
 */
public class TaskConcept extends CompoundConcept {

//    @Nullable
//    private QuestionTable questions;
//    @Nullable
//    private QuestionTable quests;
    @NotNull
    protected final BeliefTable beliefs;
    @NotNull
    protected final BeliefTable goals;


    public TaskConcept(@NotNull Compound term, @NotNull NAR n) {
        this(term, null, null, n);
    }

    public TaskConcept(@NotNull Compound term, BeliefTable beliefs, BeliefTable goals, @NotNull NAR n) {
        super(term, ((DefaultConceptBuilder)n.terms.conceptBuilder()).newLinkBags(term));
        this.beliefs = beliefs!=null ? beliefs : n.terms.conceptBuilder().newBeliefTable(this, true);
        this.goals = goals!=null ? goals: n.terms.conceptBuilder().newBeliefTable(this, false);
    }


    @NotNull
    @Override
    public final QuestionTable quests() {
        return StorelessQuestionTable;
        //return questionTableOrEmpty(quests);
    }

    @NotNull
    @Override
    public final QuestionTable questions() {
        return StorelessQuestionTable;
        //return questionTableOrEmpty(questions);
    }


    @NotNull
    static QuestionTable questionTableOrEmpty(@Nullable QuestionTable q) {
        return q != null ? q : StorelessQuestionTable;
    }

//
//    @NotNull
//    final QuestionTable questionsOrNew() {
//        //TODO this isnt thread safe
//        return (questions == null) ? ((questions =
//                //new ArrayQuestionTable(state.questionCap(true)))
//                //new HijackQuestionTable(state.questionCap(true), 3))
//                StorelessQuestionTable) : questions;
//
//    }
//
//    @NotNull
//    final QuestionTable questsOrNew() {
//        return quests == null ? (quests =
//                //new ArrayQuestionTable(state.questionCap(false)))
//                new HijackQuestionTable(state.questionCap(false), 3))
//                : quests;
//    }


    /**
     * Judgments directly made about the term Use ArrayList because of access
     * and insertion in the middle
     */
    @NotNull
    @Override
    public final BeliefTable beliefs() {
        return beliefs;
    }

    /**
     * Desire values on the term, similar to the above one
     */
    @NotNull
    @Override
    public final BeliefTable goals() {
        return goals;
    }

    protected final void beliefCapacity(int be, int bt, int ge, int gt) {

        beliefs().capacity(be, bt);
        goals().capacity(ge, gt);

    }


    @Override
    public ConceptState state(@NotNull ConceptState p) {
        ConceptState current = this.state;
        if (current != p) {
            super.state(p);

            int be = p.beliefCap(this, true, true);
            int bt = p.beliefCap(this, true, false);

            int ge = p.beliefCap(this, false, true);
            int gt = p.beliefCap(this, false, false);

            beliefCapacity(be, bt, ge, gt);
            questions().capacity(p.questionCap(true));
            quests().capacity(p.questionCap(false));
        }
        return current;
    }


    /**
     * Directly process a new task, if belief tables agree to store it.
     * Called exactly once on each task.
     */
    public void process(@NotNull Task t, @NotNull NAR n) {
        switch (t.punc()) {

            case BELIEF:
                beliefs.add(t, this, n);
                break;

            case GOAL:
                goals.add(t, this, n);
                break;

            case QUESTION:
            case QUEST:
                QuestionTable.StorelessQuestionTable.add(t, this, n);
//                (t.isQuestion() ? questionsOrNew() : questsOrNew())
//                        .add(t, this, n);
                break;

            default:
                throw new RuntimeException("Invalid sentence type: " + t);
        }

    }

    @Override
    public void delete(@NotNull NAR nar) {
        super.delete(nar);
        beliefs.clear();
        goals.clear();
        //questions = quests = null;
    }

}

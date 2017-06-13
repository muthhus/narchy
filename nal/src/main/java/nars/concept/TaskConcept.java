package nars.concept;

import jcog.bag.Bag;
import nars.NAR;
import nars.Task;
import nars.conceptualize.state.ConceptState;
import nars.table.*;
import nars.term.Compound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;

/**
 * concept of a compound term which can name a task, and thus have associated beliefs, goals, questions, and quests
 */
public class TaskConcept extends CompoundConcept {

    @Nullable
    private QuestionTable questions;
    @Nullable
    private QuestionTable quests;
    @Nullable
    protected BeliefTable beliefs;
    @Nullable
    protected BeliefTable goals;

    public TaskConcept(@NotNull Compound term, @NotNull Bag termLinks, @NotNull Bag taskLinks, @NotNull NAR nar) {
        super(term, termLinks, taskLinks, nar);
    }

    @Deprecated
    public TaskConcept(@NotNull Compound term, @NotNull NAR n) {
        super(term, n);
    }


    /**
     * Pending Quests to be answered by new desire values
     */
    @NotNull
    @Override
    public final QuestionTable quests() {
        return questionTableOrEmpty(quests);
    }

    @NotNull
    @Override
    public final QuestionTable questions() {
        return questionTableOrEmpty(questions);
    }


    @NotNull
    static QuestionTable questionTableOrEmpty(@Nullable QuestionTable q) {
        return q != null ? q : QuestionTable.EMPTY;
    }

    @NotNull
    static BeliefTable beliefTableOrEmpty(@Nullable BeliefTable b) {
        return b != null ? b : BeliefTable.EMPTY;
    }

    @NotNull
    final QuestionTable questionsOrNew(@NotNull NAR nar) {
        //TODO this isnt thread safe
        return questions == null ? (questions =
                //new ArrayQuestionTable(state.questionCap(true)))
                new HijackQuestionTable(state.questionCap(true), 3))
                : questions;

    }

    @NotNull
    final QuestionTable questsOrNew(@NotNull NAR nar) {
        return quests == null ? (quests =
                //new ArrayQuestionTable(state.questionCap(false)))
                new HijackQuestionTable(state.questionCap(false), 3))
                : quests;
    }

    @NotNull
    final BeliefTable tableOrNew(@NotNull NAR nar, boolean beliefOrGoal) {
        @Nullable BeliefTable t = beliefOrGoal ? beliefs : goals;
        if (t == null) {
            t = newBeliefTable(nar, beliefOrGoal);
            if (beliefOrGoal) beliefs = t;
            else goals = t;
        }
        return t;
    }

    @NotNull
    protected BeliefTable newBeliefTable(NAR nar, boolean beliefOrGoal) {
        return new DefaultBeliefTable();
//        int eCap = state.beliefCap(this, beliefOrGoal, true);
//        int tCap = state.beliefCap(this, beliefOrGoal, false);
//        return newBeliefTable(nar, beliefOrGoal, eCap, tCap);
    }


    public TemporalBeliefTable newTemporalTable(final int tCap, NAR nar) {
        return new HijackTemporalBeliefTable(tCap);
    }

    public EternalTable newEternalTable(int eCap) {
        return eCap > 0 ? new EternalTable(eCap) : EternalTable.EMPTY;
    }


    /**
     * Judgments directly made about the term Use ArrayList because of access
     * and insertion in the middle
     */
    @NotNull
    @Override
    public final BeliefTable beliefs() {
        return beliefTableOrEmpty(beliefs);
    }

    /**
     * Desire values on the term, similar to the above one
     */
    @NotNull
    @Override
    public final BeliefTable goals() {
        return beliefTableOrEmpty(goals);
    }

    protected void beliefCapacity(@NotNull ConceptState p, NAR nar) {

        int be = p.beliefCap(this, true, true);
        int bt = p.beliefCap(this, true, false);

        int ge = p.beliefCap(this, false, true);
        int gt = p.beliefCap(this, false, false);

        beliefCapacity(be, bt, ge, gt, nar);
    }

    protected final void beliefCapacity(int be, int bt, int ge, int gt, NAR nar) {

        beliefs().capacity(be, bt);
        goals().capacity(ge, gt);

    }

    protected void questionCapacity(@NotNull ConceptState p, NAR nar) {
        questions().capacity((byte) p.questionCap(true), nar);
        quests().capacity((byte) p.questionCap(false), nar);
    }



    @Override
    public ConceptState state(@NotNull ConceptState p, NAR nar) {
        super.state(p, nar);
        ConceptState current = this.state;
        if (current != p) {

            beliefCapacity(p, nar);
            questionCapacity(p, nar);

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
                tableOrNew(n, true).add(t, this, n);
                break;
            case GOAL:
                tableOrNew(n, false).add(t, this, n);
                break;

            case QUESTION:
            case QUEST:
                (t.isQuestion() ? questionsOrNew(n) : questsOrNew(n))
                        .add(t, this, n);
                break;

            default:
                throw new RuntimeException("Invalid sentence type: " + t);
        }

//        return inserted;
//        if ((accepted != null) &&
//                (this == accepted || !this.equals(accepted))) {
//
//            n.terms.commit(c);
//
//            if (!isInput()) //dont count direct input as learning
//                n.emotion.learn(accepted.pri(), accepted.volume());
//
//            n.eventTaskProcess.emit(/*post*/accepted);
//
//            return new ITask[]{new SpreadingActivation(accepted, c)};
//        }
//
//        // REJECTED DUE TO PRE-EXISTING REDUNDANCY,
//        // INSUFFICIENT CONFIDENCE/PRIORITY/RELEVANCE
//        // OR OTHER REASON
//
//        return null;



    }

    @Override
    public void delete(@NotNull NAR nar) {
        super.delete(nar);
        beliefs = goals = null;
        questions = quests = null;
    }

    public int taskCount() {
        int s = 0;
        if (beliefs != null)
            s += beliefs.size();
        if (goals != null)
            s += goals.size();
        if (questions != null)
            s += questions.size();
        if (quests != null)
            s += quests.size();

        return s;
    }
}

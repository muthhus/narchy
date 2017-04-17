package nars.concept;

import jcog.bag.Bag;
import jcog.pri.PriMerge;
import nars.NAR;
import nars.Task;
import nars.attention.Activation;
import nars.conceptualize.state.ConceptState;
import nars.control.ConceptBagFocus;
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
    @Nullable
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
                new HijackQuestionTable(state.questionCap(true), 3, PriMerge.avgBlend, nar.random))
                : questions;

    }

    @NotNull
    final QuestionTable questsOrNew(@NotNull NAR nar) {
        return quests == null ? (quests =
                //new ArrayQuestionTable(state.questionCap(false)))
                new HijackQuestionTable(state.questionCap(false), 3, PriMerge.avgBlend, nar.random))
                : quests;
    }

    @NotNull
    final BeliefTable beliefsOrNew(@NotNull NAR nar) {
        return beliefs == null ? (beliefs = newBeliefTable(nar, true)) : beliefs;
    }


    @NotNull
    final BeliefTable goalsOrNew(@NotNull NAR nar) {
        return goals == null ? (goals = newBeliefTable(nar, false)) : goals;
    }

    @NotNull
    protected BeliefTable newBeliefTable(NAR nar, boolean beliefOrGoal) {
        int eCap = state.beliefCap(this, beliefOrGoal, true);
        int tCap = state.beliefCap(this, beliefOrGoal, false);
        return newBeliefTable(nar, beliefOrGoal, eCap, tCap);
    }


    protected BeliefTable newBeliefTable(NAR nar, boolean beliefOrGoal, int eCap, int tCap) {

        return new DefaultBeliefTable();
    }

    public HijackTemporalBeliefTable newTemporalTable(final int tCap, NAR nar) {
        return new HijackTemporalBeliefTable(tCap, nar.random);
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


    public
    @Nullable
    Task processQuest(@NotNull Task task, @NotNull NAR nar) {
        return processQuestion(task, nar);
    }

    /**
     * To accept a new judgment as belief, and check for revisions and solutions
     * Returns null if the task was not accepted, else the goal which was accepted and somehow modified the state of this concept
     */
    @Nullable Task processBelief(@NotNull Task belief, @NotNull NAR nar) {
        return processBeliefOrGoal(belief, beliefsOrNew(nar), nar);
    }

    /**
     * To accept a new goal, and check for revisions and realization, then
     * decide whether to actively pursue it
     * Returns null if the task was not accepted, else the goal which was accepted and somehow modified the state of this concept
     */
    @Nullable Task processGoal(@NotNull Task goal, @NotNull NAR nar) {
        return processBeliefOrGoal(goal, goalsOrNew(nar), nar);
    }

    private final Task processBeliefOrGoal(@NotNull Task belief, @NotNull BeliefTable target, @NotNull NAR nar) {

        return target.add(belief, this, nar);

    }

    protected void beliefCapacity(@NotNull ConceptState p, NAR nar) {

        int be = p.beliefCap(this, true, true);
        int bt = p.beliefCap(this, true, false);

        int ge = p.beliefCap(this, false, true);
        int gt = p.beliefCap(this, false, false);

        beliefCapacity(be, bt, ge, gt, nar);
    }

    protected final void beliefCapacity(int be, int bt, int ge, int gt, NAR nar) {

        beliefs().capacity(be, bt, nar);
        goals().capacity(ge, gt, nar);

    }

    protected void questionCapacity(@NotNull ConceptState p, NAR nar) {
        questions().capacity((byte) p.questionCap(true), nar);
        quests().capacity((byte) p.questionCap(false), nar);
    }

    /**
     * To answer a quest or q by existing beliefs
     *
     * @param q         The task to be processed
     * @param nar
     * @param displaced
     * @return the relevant task
     */
    Task processQuestion(@NotNull Task q, @NotNull NAR nar) {

        final QuestionTable questionTable;
        //final BeliefTable answerTable;
        if (q.isQuestion()) {
            //if (questions == null) questions = new ArrayQuestionTable(nar.conceptQuestionsMax.intValue());
            questionTable = questionsOrNew(nar);
            //answerTable = beliefs();
        } else { // else if (q.isQuest())
            //if (quests == null) quests = new ArrayQuestionTable(nar.conceptQuestionsMax.intValue());
            questionTable = questsOrNew(nar);
            //answerTable = goals();
        }


        return questionTable.add(q, nar);
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
     * Directly process a new task. Called exactly once on each task. Using
     * local information and finishing in a constant time. Provide feedback in
     * the taskBudget value of the task.
     * <p>
     * called in Memory.immediateProcess only
     *
     * @return null if not processed, or an Activation instance to continue with link activation and feedback
     */
    @Nullable
    public final Activation process(@NotNull Task input, @NotNull NAR nar) {


        Task inserted = null;

        switch (input.punc()) {
            case BELIEF:
                inserted = processBelief(input, nar);
                break;

            case GOAL:
                inserted = processGoal(input, nar);
                break;

            case QUESTION:
                inserted = processQuestion(input, nar);
                break;

            case QUEST:
                inserted = processQuest(input, nar);
                break;

            default:
                throw new RuntimeException("Invalid sentence type: " + input);
        }

        if (inserted == null)
            return null;

        //HACK
        float activation =
                (nar.focus() instanceof ConceptBagFocus) ? ((ConceptBagFocus) (nar.focus())).activationRate.floatValue() : 1
                //1
        ;
        Activation a = nar.activateTask(inserted, this, activation);
        if (input != inserted) {
            return null; //dont process further aside from this re-activation
        } else {
            return a;
        }

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

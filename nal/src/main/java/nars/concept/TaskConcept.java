package nars.concept;

import nars.NAR;
import nars.Task;
import nars.conceptualize.ConceptBuilder;
import nars.conceptualize.DefaultConceptBuilder;
import nars.conceptualize.state.ConceptState;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.table.TaskTable;
import nars.term.Compound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Consumer;

import static nars.Op.*;

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
    private QuestionTable quests,questions;

    public TaskConcept(@NotNull Compound term, @Nullable BeliefTable beliefs, @Nullable BeliefTable goals, @NotNull NAR n) {
        this(term, beliefs, goals, n.conceptBuilder);
    }

    public TaskConcept(@NotNull Compound term, @Nullable BeliefTable beliefs, @Nullable BeliefTable goals, ConceptBuilder cb) {
        super(term, ((DefaultConceptBuilder) cb).newLinkBags(term));
        this.beliefs = beliefs != null ? beliefs : cb.newBeliefTable(this, true);
        this.goals = goals != null ? goals : cb.newBeliefTable(this, false);
        this.questions = cb.newQuestionTable();
        this.quests = cb.newQuestionTable();
    }


    @NotNull
    public QuestionTable quests() {
        return quests;
    }

    @NotNull
    public QuestionTable questions() {
        return questions;
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
    public final BeliefTable beliefs() {
        return beliefs;
    }

    /**
     * Desire values on the term, similar to the above one
     */
    @NotNull
    public final BeliefTable goals() {
        return goals;
    }

    protected final void beliefCapacity(int be, int bt, int ge, int gt) {

        beliefs().setCapacity(be, bt);
        goals().setCapacity(ge, gt);

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
        return p;
    }


    /**
     * Directly process a new task, if belief tables agree to store it.
     * Called exactly once on each task.
     */
    public void process(@NotNull Task t, @NotNull NAR n) {
        table(t.punc()).add(t, this, n);
    }

    public float valueIfProcessed(@NotNull Task t, float activation, NAR n) {
        //positive value based on the conf but also multiplied by the activation in case it already was known
        return 0.001f * activation * (t.isBeliefOrGoal() ? t.conf(n.time(), n.dur()) : 0.5f);

//            @Override
//    public float value(@NotNull Task t, NAR n) {
//        byte p = t.punc();
//        if (p == BELIEF || p == GOAL) {// isGoal()) {
//            //example value function
//            long s = t.end();
//
//            if (s!=ETERNAL) {
//                long now = n.time();
//                long relevantTime = p == GOAL ?
//                        now - n.dur() : //present or future goal
//                        now; //future belief prediction
//
//                if (s > relevantTime) //present or future TODO irrelevance discount for far future
//                    return (float) (0.1f + Math.pow(t.conf(), 0.25f));
//            }
//        }
//
//        //return super.value(t, activation, n);
//        return 0;
//    }
    }


    @Override
    public void print(@NotNull Appendable out, boolean showbeliefs, boolean showgoals, boolean showtermlinks, boolean showtasklinks) {
        super.print(out, showbeliefs, showgoals, showtermlinks, showtasklinks);

        Consumer<Task> printTask = s -> {
            try {
                out.append(printIndent);
                out.append(s.toString());
                out.append(" ");
                Object ll = s.lastLogged();
                if (ll != null)
                    out.append(ll.toString());
                out.append('\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        try {
            if (showbeliefs) {
                out.append(" Beliefs:");
                if (beliefs().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    beliefs().forEachTask(printTask);
                }
                out.append(" Questions:");
                if (questions().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    questions().forEachTask(printTask);
                }
            }

            if (showgoals) {
                out.append(" Goals:");
                if (goals().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    goals().forEachTask(printTask);
                }
                out.append(" Quests:");
                if (questions().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    quests().forEachTask(printTask);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Nullable
    public TaskTable table(byte punc) {
        switch (punc) {
            case BELIEF:
                return beliefs;
            case GOAL:
                return goals;
            case QUESTION:
                return questions;
            case QUEST:
                return quests;
            default:
                throw new UnsupportedOperationException();
        }
    }

    public void forEachTask(boolean includeConceptBeliefs, boolean includeConceptQuestions, boolean includeConceptGoals, boolean includeConceptQuests, @NotNull Consumer<Task> each) {
        if (includeConceptBeliefs) beliefs.forEachTask(each);
        if (includeConceptQuestions) questions.forEachTask(each);
        if (includeConceptGoals) goals.forEachTask(each);
        if (includeConceptQuests) quests.forEachTask(each);
    }

    public void forEachTask(@NotNull Consumer<Task> each) {
        beliefs.forEachTask(each);
        questions.forEachTask(each);
        goals.forEachTask(each);
        quests.forEachTask(each);
    }


    @Override
    public void delete(@NotNull NAR nar) {
        super.delete(nar);
        beliefs.clear();
        goals.clear();
        questions.clear();
        quests.clear();
        //questions = quests = null;
    }

}

package nars.concept;

import nars.Memory;
import nars.NAR;
import nars.Symbols;
import nars.bag.Bag;
import nars.budget.Budget;
import nars.concept.util.*;
import nars.nal.LocalRules;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;


public class DefaultConcept extends AtomConcept {

//    public static final BiPredicate<Task, Task> questionEquivalence = new BiPredicate<Task, Task>() {
//
//        @Override
//        public boolean test(@NotNull Task a, Task b) {
//            return (a.equals(b));
//        }
//
////        //N/
////        @Override public int compare(Task task, Task t1) {  return 0;        }
////        @Override public int hashCodeOf(Task task) { return task.hashCode(); }
//    };
    /**
     * how incoming budget is merged into its existing duplicate quest/question
     */

    @Nullable
    private List<Termed> termLinkTemplates = null;

    @Nullable
    protected QuestionTaskTable questions = null;
    @Nullable
    protected QuestionTaskTable quests = null;
    @Nullable
    protected BeliefTable beliefs = null;
    @Nullable
    protected BeliefTable goals = null;


//    public DefaultConcept(Term term, Memory p) {
//        this(term, new NullBag(), new NullBag(), p);
//    }

    /**
     * Constructor, called in Memory.getConcept only
     *
     * @param term      A term corresponding to the concept
     * @param taskLinks
     * @param termLinks
     */
    public DefaultConcept(Term term, Bag<Task> taskLinks, Bag<Termed> termLinks, @NotNull NAR n) {
        super(term, termLinks, taskLinks);
    }

    /**
     * Pending Quests to be answered by new desire values
     */
    @Nullable
    @Override
    public final QuestionTaskTable quests() {
        return tableOrEmpty(quests);
    }

    @Nullable
    @Override
    public final QuestionTaskTable questions() {
        return tableOrEmpty(questions);
    }

    private static QuestionTaskTable tableOrEmpty(QuestionTaskTable q) {
        if (q == null) return TaskTable.EMPTY;
        return q;
    }


    /**
     * Judgments directly made about the term Use ArrayList because of access
     * and insertion in the middle
     */
    @Nullable
    @Override
    public final BeliefTable beliefs() {
        return beliefs == null ? BeliefTable.EMPTY : beliefs;
    }

    /**
     * Desire values on the term, similar to the above one
     */
    @Nullable
    @Override
    public final BeliefTable goals() {
        return goals == null ? BeliefTable.EMPTY : goals;
    }


//    /** updates the concept-has-questions index if the concept transitions from having no questions to having, or from having to not having */
//    public void onTableUpdated(char punctuation, int originalSize) {
//
//        switch (punctuation) {
//            /*case Symbols.GOAL:
//                break;*/
//            case Symbols.QUEST:
//            case Symbols.QUESTION:
//                if (getQuestions().isEmpty() && getQuests().isEmpty()) {
//                    //if (originalSize > 0) //became empty
//                        //getMemory().updateConceptQuestions(this);
//                } else {
//                    //if (originalSize == 0) //became non-empty
//                        //getMemory().updateConceptQuestions(this);
//
//                }
//                break;
//        }
//    }

    /* ---------- direct processing of tasks ---------- */


    //
//    /**
//     * for batch processing a collection of tasks; more efficient than processing them individually
//     */
//    //TODO untested
//    public void link(Collection<Task> tasks) {
//
//        final int s = tasks.size();
//        if (s == 0) return;
//
//        if (s == 1) {
//            link(tasks.iterator().next());
//            return;
//        }
//
//
//        //aggregate a merged budget, allowing a maximum of (1,1,1)
//        Budget aggregateBudget = null;
//        for (Task t : tasks) {
//            if (linkTask(t)) {
//                if (aggregateBudget == null) aggregateBudget = new Budget(t, false);
//                else {
//                    //aggregateBudget.merge(t);
//                    aggregateBudget.accumulate(t);
//                }
//            }
//        }
//
//        //linkToTerms the aggregate budget, rather than each task's budget separately
//        if (aggregateBudget != null) {
//            linkTerms(aggregateBudget, true);
//        }
//    }


    /**
     * To accept a new judgment as belief, and check for revisions and solutions
     *
     * @param belief The task to be processed
     * @param nar
     * @return Whether to continue the processing of the task
     */
    @Nullable
    @Override
    public Task processBelief(@NotNull Task belief, @NotNull NAR nar) {

        //long now = nar.time();

        //boolean hasGoals = hasGoals();
        //float successBefore = hasGoals ? getSuccess(now) : 0;

        Memory memory = nar.memory;

        BeliefTable beliefs = this.beliefs;
        if (beliefs == null)
            beliefs = this.beliefs = new DefaultBeliefTable(memory.conceptBeliefsMax.intValue(), memory);

        belief = beliefs.add(belief, nar);

        //TODO only compute updates if belief was actually added, not merged with duplicate

        {
            if (belief!=null && hasQuestions()) {
                //TODO move this to a subclass of TaskTable which is customized for questions. then an arraylist impl of TaskTable can iterate by integer index and not this iterator/lambda
                final Task solution = belief;
                questions().forEach(question -> {
                    LocalRules.forEachSolution(question, solution, nar, nar);
                });
            }

//            if (hasGoals) {
//                updateSuccess(null, successBefore, memory);
//            }

        }

        return belief;

//        if (belief.isInput() && !belief.isEternal()) {
//            this.put(Anticipate.class, true);
//        }
    }

    private float updateSuccess(@Nullable Task inputGoal, float successBefore, @NotNull Memory memory) {
        /** update happiness meter on solution  TODO revise */
        float successAfter = getSuccess(memory.time());

        if (inputGoal != null)
            successAfter = Math.max(inputGoal.expectation(), successAfter);

        float delta = successAfter - successBefore;
        if (delta != 0) //more satisfaction of a goal due to belief, more happiness
            memory.emotion.happyPlus(delta);
        return delta;
    }


    /**
     * To accept a new goal, and check for revisions and realization, then
     * decide whether to actively pursue it
     *
     * @param goal The task to be processed
     * @param task
     * @return Whether to continue the processing of the task
     */
    @Nullable
    @Override
    public final Task processGoal(@NotNull Task inputGoal, @NotNull NAR nar) {

        Memory memory = nar.memory;

        BeliefTable g = goals();
        if (goals == null) {
            g = this.goals = new DefaultBeliefTable(
                memory.conceptGoalsMax.intValue(), memory);
        }

        return g.add(inputGoal, nar);
    }


    //long then = goal.getOccurrenceTime();
    //int dur = nal.duration();

//        //this task is not up to date (now is ahead of then) we have to project it first
//        if(TemporalRules.after(then, now, dur)) {
//
//            nal.singlePremiseTask(task.projection(nal.memory, now, then) );
//
//            return true;
//
//        }

//         if (goal.getBudget().summaryGreaterOrEqual(memory.questionFromGoalThreshold)) {
//
//                // check if the Goal is already satisfied
//                //Task beliefSatisfied = getBeliefs().topRanked();
//
//            /*float AntiSatisfaction = 0.5f; //we dont know anything about that goal yet, so we pursue it to remember it because its maximally unsatisfied
//            if (beliefSatisfied != null) {
//
//                Truth projectedTruth = beliefSatisfied.projection(goal.getOccurrenceTime(), dur);
//                //Sentence projectedBelief = belief.projectionSentence(goal.getOccurrenceTime(), dur);
//
//                boolean solved = null!=trySolution(beliefSatisfied, projectedTruth, goal, nal); // check if the Goal is already satisfied (manipulate budget)
//                if (solved) {
//                    AntiSatisfaction = goal.getTruth().getExpDifAbs(beliefSatisfied.getTruth());
//                }
//            }
//
//            float Satisfaction = 1.0f - AntiSatisfaction;
//            Truth T = BasicTruth.clone(goal.getTruth());
//
//            T.setFrequency((float) (T.getFrequency() - Satisfaction)); //decrease frequency according to satisfaction value
//
//            if (AntiSatisfaction >= Global.SATISFACTION_TRESHOLD && goal.sentence.truth.getExpectation() > nal.memory.param.executionThreshold.get()) {
//*/
//
//                questionFromGoal(goal, nal);
//
//                //TODO
//                //InternalExperience.experienceFromTask(nal, task, false);
//
//
//                //}
//            }


//    public static void questionFromGoal(final Task task, final Premise p) {
//        if (Global.QUESTION_GENERATION_ON_DECISION_MAKING || Global.HOW_QUESTION_GENERATION_ON_DECISION_MAKING) {
//            //ok, how can we achieve it? add a question of whether it is fullfilled
//
//            List<Compound> qu = Global.newArrayList(3);
//
//            final Compound term = task.getTerm();
//
//            if (Global.HOW_QUESTION_GENERATION_ON_DECISION_MAKING) {
//                if (!(term instanceof Equivalence) && !(term instanceof Implication)) {
//
//                    Implication i1 = Implication.make(how, term, TemporalRules.ORDER_CONCURRENT);
//                    if (i1 != null)
//                        qu.add(i1);
//
//                    Implication i2 = Implication.make(how, term, TemporalRules.ORDER_FORWARD);
//                    if (i2 != null)
//                        qu.add(i2);
//
//                }
//            }
//
//            if (Global.QUESTION_GENERATION_ON_DECISION_MAKING) {
//                qu.add(term);
//            }
//
//            if (qu.isEmpty()) return;
//
//            p.input(
//                qu.stream().map(q -> p.newTask(q)
//                    .question()
//                    .parent(task)
//                    .occurr(task.getOccurrenceTime()) //set tense of question to goal tense)
//                    .budget(task.getPriority() * Global.CURIOSITY_DESIRE_PRIORITY_MUL, task.getDurability() * Global.CURIOSITY_DESIRE_DURABILITY_MUL, 1)
//            ));
//
//        }
//    }


    /**
     * To answer a quest or q by existing beliefs
     *
     * @param q   The task to be processed
     * @param nar
     * @return the relevant task
     */
    @Nullable
    @Override
    public Task processQuestion(@NotNull Task q, @NotNull NAR nar) {

        final QuestionTaskTable questionTable;
        if (q.isQuestion()) {
            if (questions == null) questions = new ArrayListTaskTable(nar.memory.conceptQuestionsMax.intValue());
            questionTable = questions();
        } else { // else if (q.isQuest())
            if (quests == null) quests = new ArrayListTaskTable(nar.memory.conceptQuestionsMax.intValue());
            questionTable = quests();
        }

        //if (Global.DEBUG) {
//            if (q.getTruth() != null) {
//                System.err.println(q + " has non-null truth");
//                System.err.println(q.getExplanation());
//                throw new RuntimeException(q + " has non-null truth");
//            }
//            questionTable.forEach(qq -> {
//                if (qq.getDeleted())
//                    throw new RuntimeException("question is Deleted: " + qq);
//            });
        //}

        /** execute the question, for any attached operators that will handle it */
        //getMemory().execute(q);

        //boolean tableAffected = false;
        //boolean newQuestion = table.isEmpty();

        q = questionTable.add(q, nar.memory);

        //TODO if the table was not affected, does the following still need to happen:

        if (q != null) {
            BeliefTable answerTable = q.isQuest() ?
                    goals() : beliefs();

            Task sol = answerTable.top(q.occurrence(), nar.time());
            if (sol != null) {
                LocalRules.forEachSolution(q, sol, nar, nar);
            }
        }

        return q;
    }




    /* ---------- insert Links for indirect processing ---------- */


    //    /**
//     * Recalculate the quality of the concept [to be refined to show
//     * extension/intension balance]
//     *
//     * @return The quality value
//     */
//    public float getAggregateQuality() {
//        float linkPriority = termLinks.getPriorityMean();
//        float termComplexityFactor = 1.0f / term.getComplexity();
//        float result = or(linkPriority, termComplexityFactor);
//        if (result < 0) {
//            throw new RuntimeException("Concept.getQuality < 0:  result=" + result + ", linkPriority=" + linkPriority + " ,termComplexityFactor=" + termComplexityFactor + ", termLinks.size=" + termLinks.size());
//        }
//        return result;
//    }


    //
//
//    /**
//     * Collect direct isBelief, questions, and goals for display
//     *
//     * @return String representation of direct content
//     */
//    public String displayContent() {
//        final StringBuilder buffer = new StringBuilder(18);
//        buffer.append("\n  Beliefs:\n");
//        if (!beliefsEternal.isEmpty()) {
//            for (Sentence s : beliefsEternal) {
//                buffer.append(s).append('\n');
//            }
//        }
//        if (!beliefsTemporal.isEmpty()) {
//            for (Sentence s : beliefsTemporal) {
//                buffer.append(s).append('\n');
//            }
//        }
//        if (!questions.isEmpty()) {
//            buffer.append("\n  Question:\n");
//            for (Task t : questions) {
//                buffer.append(t).append('\n');
//            }
//        }
//        return buffer.toString();
//    }


    //
//    public Collection<Sentence> getSentences(char punc) {
//        switch(punc) {
//            case Symbols.JUDGMENT: return beliefs;
//            case Symbols.GOAL: return goals;
//            case Symbols.QUESTION: return Task.getSentences(questions);
//            case Symbols.QUEST: return Task.getSentences(quests);
//        }
//        throw new RuntimeException("Invalid punctuation: " + punc);
//    }
//    public CharSequence getBeliefsSummary() {
//        if (beliefs.isEmpty())
//            return "0 beliefs";
//        StringBuilder sb = new StringBuilder();
//        for (Sentence s : beliefs)
//            sb.append(s.toString()).append('\n');
//        return sb;
//    }
//    public CharSequence getGoalSummary() {
//        if (goals.isEmpty())
//            return "0 goals";
//        StringBuilder sb = new StringBuilder();
//        for (Sentence s : goals)
//            sb.append(s.toString()).append('\n');
//        return sb;
//    }


//    @Override public synchronized boolean delete() {
//
//        if (!super.delete())
//            return false;
//
//        //dont delete the tasks themselves because they may be referenced from othe concepts.
//        beliefs.clear();
//        goals.clear();
//        questions.clear();
//        quests.clear();
//
//
//        getTermLinks().delete();
//        getTaskLinks().delete();
//
//        if (getTermLinkBuilder() != null)
//            getTermLinkBuilder().delete();
//
//        return true;
//    }


//    /**
//     * Recursively build TermLinks between a compound and its components
//     * <p>
//     * called only from Memory.continuedProcess
//     * activates termlinked concepts with fractions of the taskbudget
//
//     *
//     * @param b            The BudgetValue of the task
//     * @param updateTLinks true: causes update of actual termlink bag, false: just queues the activation for future application.  should be true if this concept calls it for itself, not for another concept
//     * @return whether any activity happened as a result of this invocation
//     */
//    public boolean linkTemplates(Budget b, float scale, boolean updateTLinks, NAR nar) {
//
//        if ((b == null) || (b.isDeleted())) return false;
//
//        Term[] tl = getTermLinkTemplates();
//        if (tl == null || tl.length == 0)
//            return false;
//
//        //subPriority = b.getPriority() / (float) Math.sqrt(recipients);
//        float factor = scale / (tl.length);
//
//        final Memory memory = nar.memory;
//
//        if (factor < memory.termLinkThreshold.floatValue())
//            return false;
//
//        boolean activity = false;
//
//        for (Term t : tl) {
//
//            /*if ((t.getTarget().equals(getTerm()))) {
//                //self
//                continue;
//            }*/
//
//
//            //only apply this loop to non-transform termlink templates
//            //PENDING_TERMLINK_BUDGET_MERGE.value(t, subBudget);
//
//            if (updateTLinks) {
//                //if (t.summaryGreaterOrEqual(termLinkThresh)) {
//
//                    if (link(t, b, factor, nar))
//                        activity = true;
//                //}
//            }
//
//        }
//
//        return activity;
//
//    }


//    /**
//     * Recursively build TermLinks between a compound and its components
//     * Process is started by one Task, and recurses only to templates
//     * creating bidirectional links between compound to components
//     */
//    @Override public void linkTemplates(Budget budget, float scale, NAR nar) {
//
//        Termed[] tl = getTermLinkTemplates();
//        int numTemplates;
//        if (tl == null || (numTemplates = tl.length) == 0)
//            return;
//
//        final Memory memory = nar.memory;
//
//        float subScale = scale / numTemplates;
//        if (subScale < memory.termLinkThreshold.floatValue())
//            return;
//
//        for (int i = 0; i < tl.length; i++) {
//            Termed t = tl[i];
//
//            final Concept target;
//            if (t instanceof Concept) {
//                target = (Concept) t;
//            } else {
//                target = nar.conceptualize(t);
//                if (target == null) continue;
//                tl[i] = target;
//            }
//
//            linkTemplate(target, budget, subScale, nar);
//            target.linkTemplates(budget, subScale, nar);
//        }
//
//    }


//    @Override public boolean link(Term t, Budget b, float scale, NAR nar) {
//
//        if (t.equals(term()))
//            throw new RuntimeException("looped activation");
//
//        Concept otherConcept = activateConcept(t, b, scale, nar);
//
//        //termLinkBuilder.set(t, false, nar.memory);
//
//        //activate this termlink to peer
//        // this concept termLink to that concept
//        getTermLinks().put(t, b, scale);
//
//        //activate peer termlink to this
//        //otherConcept.activateTermLink(termLinkBuilder.setIncoming(true)); // that concept termLink to this concept
//        otherConcept.getTermLinks().put(term(), b, scale);
//
//        //if (otherConcept.getTermLinkTemplates()) {
//        //UnitBudget termlinkBudget = termLinkBuilder.getBudget();
//        linkTemplates(b, scale, immediateTermLinkPropagation, nar);
//
//        return true;
//    }


    @Override
    public boolean link(@NotNull Task task, float scale, float minScale, @NotNull NAR nar) {
        if (super.link(task, scale, minScale, nar)) {


            List<Termed> templates = termlinkTemplates();
            if (templates == null) {
                templates = this.termLinkTemplates = TermLinkBuilder.build(this, nar);
            }

            Budget taskBudget = task.budget();

            float subScale;
            int numTemplates = templates.size();
            switch (numTemplates) {
                case 0: return true;
                //case 1: subScale = 0.5f; break; //HACK
                default:
                    subScale = scale / numTemplates;
                    if (subScale < minScale)
                        return true;
            }

            for (int i = 0, templatesSize = templates.size(); i < templatesSize; i++) {
                Termed linkTemplate = templates.get(i);

                Concept templateConcept = nar.conceptualize(linkTemplate, taskBudget, subScale, 0);
                if (templateConcept != null) {
                    linkTerm(templateConcept, taskBudget, subScale);

                    /** recursively activate the template's task tlink */
                    templateConcept.link(task, subScale, minScale, nar);
                }

            }

            return true;
        }

        return false;
    }

    @Nullable @Override
    public List<Termed> termlinkTemplates() {
        return termLinkTemplates;
    }

    /**
     * Directly process a new task. Called exactly once on each task. Using
     * local information and finishing in a constant time. Provide feedback in
     * the taskBudget value of the task.
     * <p>
     * called in Memory.immediateProcess only
     *
     * @return the relevant, non-null, non-Deleted Task, which will either be:
     * --the input
     * --an existing one which absorbed the input and will re-fire
     * --a revised/projected task which may or may not remain in the belief table
     */
    @Nullable
    public final Task process(@NotNull final Task task, @NotNull NAR nar) {

        task.onConcept(this);

        switch (task.punc()) {
            case Symbols.BELIEF:
                return processBelief(task, nar);

            case Symbols.GOAL:
                return processGoal(task, nar);

            case Symbols.QUESTION:
                return processQuestion(task, nar);

            case Symbols.QUEST:
                return processQuest(task, nar);

            default:
                throw new RuntimeException("Invalid sentence type: " + task);
        }

    }


}

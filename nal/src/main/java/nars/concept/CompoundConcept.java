package nars.concept;

import com.gs.collections.impl.tuple.Tuples;
import nars.Memory;
import nars.NAR;
import nars.Op;
import nars.Symbols;
import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.concept.util.*;
import nars.nal.Tense;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.container.TermContainer;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import static nars.nal.Tense.ITERNAL;


public class CompoundConcept extends AbstractConcept<Compound> implements Compound {

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
    private List<TermTemplate> termLinkTemplates;

    @Nullable
    protected QuestionTable questions;
    @Nullable
    protected QuestionTable quests;
    @Nullable
    protected BeliefTable beliefs;
    @Nullable
    protected BeliefTable goals;

    //    public DefaultConcept(Term term, Memory p) {
//        this(term, new NullBag(), new NullBag(), p);
//    }

    /**
     * Constructor, called in Memory.getConcept only
     *  @param term      A term corresponding to the concept
     * @param termLinks
     * @param taskLinks
     */
    public CompoundConcept(@NotNull Compound term, Bag<Termed> termLinks, Bag<Task> taskLinks) {
        super(term, taskLinks, termLinks);
    }

    /**
     * Pending Quests to be answered by new desire values
     */
    @Nullable
    @Override
    public final QuestionTable quests() {
        return tableOrEmpty(quests);
    }

    @NotNull
    @Override
    public final QuestionTable questions() {
        return tableOrEmpty(questions);
    }

    @NotNull private static QuestionTable tableOrEmpty(@Nullable QuestionTable q) {
        return q == null ? TaskTable.EMPTY : q;
    }
    @NotNull private static BeliefTable tableOrEmpty(@Nullable BeliefTable q) {
        return q == null ? BeliefTable.EMPTY : q;
    }


    /**
     * Judgments directly made about the term Use ArrayList because of access
     * and insertion in the middle
     */
    @NotNull
    @Override
    public final BeliefTable beliefs() {
        return tableOrEmpty(beliefs);
    }

    /**
     * Desire values on the term, similar to the above one
     */
    @NotNull
    @Override
    public final BeliefTable goals() {
        return tableOrEmpty(goals);
    }

    @Override
    public
    @Nullable
    Task processQuest(@NotNull Task task, @NotNull NAR nar) {
        return processQuestion(task, nar);
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

        BeliefTable beliefs = this.beliefs;
        if (beliefs == null)
            beliefs = this.beliefs = new ArrayBeliefTable(nar.conceptBeliefsMax.intValue(), nar);

        belief = beliefs.add(belief, nar);

        //TODO only compute updates if belief was actually added, not merged with duplicate

        //            if (belief!= null && hasQuestions()) {
//                //TODO move this to a subclass of TaskTable which is customized for questions. then an arraylist impl of TaskTable can iterate by integer index and not this iterator/lambda
//                final Task solution = belief;
//                questions().forEach(question -> {
//                    LocalRules.forEachSolution(question, solution, nar);
//                });
//            }

//            if (hasGoals) {
//                updateSuccess(null, successBefore, memory);
//            }

        if (belief!=null) {


            Task parentTask = belief.getParentTask();
            if (parentTask != null && parentTask.isQuestion()) {

                if (parentTask.isInput()) //filter
                    nar.eventAnswer.emit(Tuples.twin(parentTask, belief));

            }

        }

        return belief;

//        if (belief.isInput() && !belief.isEternal()) {
//            this.put(Anticipate.class, true);
//        }
    }

//    private float updateSuccess(@Nullable Task inputGoal, float successBefore, @NotNull Memory memory) {
//        /** update happiness meter on solution  TODO revise */
//        float successAfter = getSuccess(memory.time());
//
//        if (inputGoal != null)
//            successAfter = Math.max(inputGoal.expectation(), successAfter);
//
//        float delta = successAfter - successBefore;
//        if (delta != 0) //more satisfaction of a goal due to belief, more happiness
//            memory.emotion.happyPlus(delta);
//        return delta;
//    }


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
    public Task processGoal(@NotNull Task inputGoal, @NotNull NAR nar) {

        BeliefTable g = this.goals;
        if (g == null) {
            g = this.goals = new ArrayBeliefTable(
                nar.conceptGoalsMax.intValue(), nar);
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

        final QuestionTable questionTable;
        if (q.isQuestion()) {
            if (questions == null) questions = new ArrayQuestionTable(nar.conceptQuestionsMax.intValue());
            questionTable = questions();
        } else { // else if (q.isQuest())
            if (quests == null) quests = new ArrayQuestionTable(nar.conceptQuestionsMax.intValue());
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

        q = questionTable.add(q, nar);

        //TODO if the table was not affected, does the following still need to happen:

//        if (q != null) {
//            BeliefTable answerTable = q.isQuest() ?
//                    goals() : beliefs();
//
//            Task sol = answerTable.top(q.occurrence(), nar.time());
//            if (sol != null) {
//                LocalRules.forEachSolution(q, sol, nar);
//            }
//        }

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
    public boolean link(@NotNull Budgeted b, float scale, float minScale, @NotNull NAR nar, @Nullable MutableFloat conceptOverflow) {
        //1. Link the Task

        if (super.link(b, scale, minScale, nar, conceptOverflow)) {

            //3. Link the termlink templates
            List<TermTemplate> templates = termlinkTemplates();
            if (templates == null) {
                templates = this.termLinkTemplates = TermLinkBuilder.build(term, nar);
            }

//            float subScale;
//            int numTemplates = templates.size();
//            switch (numTemplates) {
//                case 0: return true;
//                //case 1: subScale = 0.5f; break; //HACK
//                default:
//                    subScale = scale / numTemplates;
//            }

            /*if (subScale >= minScale)*/ {
                MutableFloat subConceptOverflow = new MutableFloat(/*0*/);

                //int numUnder = 0;

                for (int i = 0, templatesSize = templates.size(); i < templatesSize; i++) {
                    TermTemplate tt = templates.get(i);
                    float subScale = scale * tt.strength;

                    //Link the peer termlink bidirectionally
                    if (subScale > minScale) //TODO use a min bound to prevent the iteration ahead of time
                        linkTerm(this, tt.term, b, subScale, true, subConceptOverflow, null, nar);
                }


                float scOver = subConceptOverflow.floatValue();
                if (scOver > minScale) {
                    // recursive overflow accumulated to callee's overflow


                    //Simple method: just dispense equal proportion of the overflow to all template concepts equally
                    for (int i = 0, templatesSize = templates.size(); i < templatesSize; i++) {
                        TermTemplate tt = templates.get(i);
                        float subScale = scOver * tt.strength;
                        if (subScale > minScale)
                            linkTerm(this, tt.term, b, subScale, true, conceptOverflow, null, nar);
                    }

                    //TODO More fair method:
//                    //iterate over templates, psuedorandomly by choosing a random start index and visiting each modulo N
//                    int i = nar.random.nextInt(numTemplates);
//                    for (int n = 0; n < numTemplates; n++) {
//                        Termed nt = templates.get((i + n) % numTemplates);
//
//                    }

                }
                //logger.debug("{} link: {} budget overflow {}", this, b, overflow);


//                //redistribute overflow to termlink templates:
//                if ((sumOver > 0) && (numUnder > 0)) {
//
//                    /** the last visited termlink will have the opportunity to receive the biggest bonus,
//                     *  so ordering the templates by volume could allow the most complex ones to receive the most bonus */
//                    for (int i = 0; i < numTemplates; i++) {
//                        float subScale1 = sumOver / (numUnder--);
//                        Concept target = nar.conceptualize(templates.get(i), b, subScale1);
//                        assert(target!=null);
//
//                        //2. Link the peer termlink bidirectionally
//                        linkTerm(this, target, b, subScale1, true, true, null);
//                        sumOver += linkTemplate(b, templates.get(i),
//                                sumOver / (numUnder--), nar);
//
//                        if (numUnder == 0) break; //finished
//                    }
//
//                }
            }

            return true;
        }

        return false;
    }


    @Nullable @Override
    public final List<TermTemplate> termlinkTemplates() {
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
    @Override
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


    @Override
    public @NotNull TermContainer subterms() {
        return term.subterms();
    }

    @Override
    public final int relation() {
        return term.relation();
    }

    @Override @NotNull
    public final Op op() {
        return term.op();
    }

    @Override
    public final int volume() {
        return term.volume();
    }

    @Override
    public final int complexity() {
        return term.complexity();
    }

    @Override
    public final int structure() {
        return term.structure();
    }

    @Override
    public final int size() {
        return term.size();
    }

    @Override
    public final boolean containsTerm(Term t) {
        return term.containsTerm(t);
    }

    @Override
    public final boolean isCommutative() {
        return term.isCommutative();
    }

    @Override
    public final int varIndep() {
        return term.varIndep();
    }

    @Override
    public final int varDep() {
        return term.varDep();
    }

    @Override
    public final int varQuery() {
        return term.varQuery();
    }

    @Override
    public final int varPattern() {
        return term.varPattern();
    }

    @Override
    public final int vars() {
        return term.vars();
    }

    @Nullable
    @Override
    public final Term term(int i) {
        return term.term(i);
    }

    @Override
    public final boolean equalTerms(TermContainer c) {
        return term.equalTerms(c);
    }

    @NotNull
    @Override
    public final Term[] terms() {
        return term.terms();
    }

    @Override
    public void forEach(Consumer action, int start, int end) {
        term.forEach(action, start, end);
    }

    @Nullable
    @Override
    public TermContainer replacing(int subterm, Term replacement) {
        return term.replacing(subterm, replacement);
    }

    @Override
    public void addAllTo(@NotNull Collection set) {
        Collections.addAll(set, term);
    }

    @Override
    public final boolean isNormalized() {
        return true; //must be normalized to create the concept
    }

    @NotNull
    @Override
    public final Compound dt(int cycles) {
        //the concept will not hold any particular temporality in its ID term. however its tasks may utliize them
        throw new UnsupportedOperationException();
    }

    @Override
    public final int dt() {
        //concept itself is eternal
        return ITERNAL;
    }

//    @NotNull
//    @Override
//    public final Compound anonymous() {
//        //concept itself is eternal
//        return this;
//    }

    @Override
    public Iterator iterator() {
        return term.iterator();
    }
}

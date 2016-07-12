package nars.concept;

import nars.NAR;
import nars.Symbols;
import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.budget.policy.ConceptPolicy;
import nars.concept.table.ArrayQuestionTable;
import nars.concept.table.BeliefTable;
import nars.concept.table.DefaultBeliefTable;
import nars.concept.table.QuestionTable;
import nars.link.TermLinkBuilder;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Termlike;
import nars.term.container.TermSet;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;


public class CompoundConcept<T extends Compound> implements AbstractConcept<T>,Termlike {

    @NotNull
    private final Bag<Task> taskLinks;
    @NotNull
    private final Bag<Term> termLinks;

    /**
     * how incoming budget is merged into its existing duplicate quest/question
     */

    @NotNull
    final TermSet templates;
    @NotNull
    private final T term;

    @Nullable
    private QuestionTable questions;
    @Nullable
    private QuestionTable quests;
    @Nullable
    private BeliefTable beliefs;
    @Nullable
    private BeliefTable goals;

    private float satisfaction = 0;
    private @NotNull Map meta;
    private transient ConceptPolicy policy;


    /**
     * Constructor, called in Memory.getConcept only
     *  @param term      A term corresponding to the concept
     * @param termLinks
     * @param taskLinks
     */
    public CompoundConcept(@NotNull T term, @NotNull Bag<Term> termLinks, @NotNull Bag<Task> taskLinks) {
        this.term = term;

        this.templates = TermSet.the(TermLinkBuilder.components(term));

        this.termLinks = termLinks;
        this.taskLinks = taskLinks;
    }

    @NotNull
    @Override
    public T term() {
        return term;
    }


    @Override
    public void setMeta(@NotNull Map newMeta) {
        this.meta = newMeta;
    }

    @NotNull
    @Override
    public <C> C meta(@NotNull Object key, @NotNull BiFunction value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable Map<Object, Object> meta() {
        return meta;
    }

    @Override
    public @NotNull Bag<Task> tasklinks() {
        return taskLinks;
    }

    @NotNull
    @Override
    public Bag<Term> termlinks() {
        return termLinks;
    }


    /** used for setting an explicit OperationConcept instance via java; activates it on initialization */
    public CompoundConcept(@NotNull T term, @NotNull NAR n) {
        this(term, n.index.conceptBuilder());
    }

    /** default construction by a NAR on conceptualization */
    public CompoundConcept(@NotNull T term, @NotNull ConceptBuilder b) {
        this(term, b.termbag(), b.taskbag());
    }


    @Override
    public boolean contains(@NotNull Task t) {
        return tableFor(t.punc()).get(t)!=null;
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
        return q !=null ? q : QuestionTable.EMPTY;
    }
    @NotNull
    static BeliefTable beliefTableOrEmpty(@Nullable BeliefTable b) {
        return b !=null ? b : BeliefTable.EMPTY;
    }

    @NotNull
    final QuestionTable questionsOrNew() {
        return questions == null ? (questions = new ArrayQuestionTable(policy.questionCap(true))) : questions;
    }
    @NotNull
    final QuestionTable questsOrNew() {
        return quests == null ? (quests = new ArrayQuestionTable(policy.questionCap(false))) : quests;
    }
    @NotNull
    final BeliefTable beliefsOrNew() {
        return beliefs == null ? (beliefs = newBeliefTable()) : beliefs;
    }


    @NotNull
    final BeliefTable goalsOrNew() {
        return goals == null ? (goals = newGoalTable()) : goals;
    }

    @NotNull
    protected BeliefTable newBeliefTable() {
        int eCap = policy.beliefCap(this, true, true);
        int tCap = policy.beliefCap(this, true, false);
        return newBeliefTable(eCap, tCap);
    }

    @NotNull
    protected BeliefTable newBeliefTable(int eCap, int tCap) {
        return new DefaultBeliefTable(eCap, tCap);
    }

    @NotNull
    protected BeliefTable newGoalTable() {
        int eCap = policy.beliefCap(this, false, true);
        int tCap = policy.beliefCap(this, false, false);
        return newGoalTable(eCap, tCap);
    }

    @NotNull
    protected BeliefTable newGoalTable(int eCap, int tCap) {
        return new DefaultBeliefTable(eCap, tCap);
    }

    /**
     * Judgments directly made about the term Use ArrayList because of access
     * and insertion in the middle
     */
    @NotNull
    @Override
    public BeliefTable beliefs() {
        return beliefTableOrEmpty(beliefs);
    }

    /**
     * Desire values on the term, similar to the above one
     */
    @NotNull
    @Override
    public BeliefTable goals() {
        return beliefTableOrEmpty(goals);
    }


    public
    @Nullable
    Task processQuest(@NotNull Task task, @NotNull NAR nar, List<Task> displaced) {
        return processQuestion(task, nar, displaced);
    }

    @Override public void delete() {
        termlinks().clear();
        tasklinks().clear();
        beliefs().clear();
        goals().clear();
        questions().clear();
        quests().clear();
    }



    /**
     * To accept a new judgment as belief, and check for revisions and solutions
     * Returns null if the task was not accepted, else the goal which was accepted and somehow modified the state of this concept
     */
    public Task processBelief(@NotNull Task belief, @NotNull NAR nar, List<Task> displaced) {
        return processBeliefOrGoal(belief, nar, beliefsOrNew(), questions(), displaced);
    }

    /**
     * To accept a new goal, and check for revisions and realization, then
     * decide whether to actively pursue it
     * Returns null if the task was not accepted, else the goal which was accepted and somehow modified the state of this concept
     */
    public Task processGoal(@NotNull Task goal, @NotNull NAR nar, List<Task> displaced) {
        return processBeliefOrGoal(goal, nar, goalsOrNew(), quests(), displaced);
    }

    /**
     * @return null if the task was not accepted, else the goal which was accepted and somehow modified the state of this concept
     * TODO remove synchronized by lock-free technique
     */
    synchronized private final Task processBeliefOrGoal(@NotNull Task belief, @NotNull NAR nar, @NotNull BeliefTable target, @NotNull QuestionTable questions, List<Task> displaced) {

        if (belief.temporal() && (hasBeliefs()&&hasGoals())) {

            //finds the temporal intersection of the two temporal belief tables:
            //this affects the temporal belief compression's focus in time

            long minT = Long.MAX_VALUE;
            long maxT = Long.MIN_VALUE;

                //max of the min
                minT = Math.max( minT,  (((DefaultBeliefTable)beliefs()).temporal.min() ));
                minT = Math.max( minT,  (((DefaultBeliefTable)goals()).temporal.min() ));

                //..and min of the max
                maxT = Math.min( maxT,  (((DefaultBeliefTable)beliefs()).temporal.max() ));
                maxT = Math.min( maxT,  (((DefaultBeliefTable)goals()).temporal.max() ));

            ((DefaultBeliefTable)beliefs()).temporal.min(minT);
            ((DefaultBeliefTable)beliefs()).temporal.max(maxT);
            ((DefaultBeliefTable)goals()).temporal.min(minT);
            ((DefaultBeliefTable)goals()).temporal.max(maxT);
        }

        //synchronized (target) {
            Task b = target.add(belief, questions, displaced, nar);
            if (b != null)
                updateSatisfaction(nar);
            return b;
        //}
    }

    protected final void updateSatisfaction(@NotNull NAR nar) {
        BeliefTable b = beliefs();
        BeliefTable g = goals();

        long now = nar.time();

        float nextSatisfaction = b.motivation(now) - g.motivation(now);

        float deltaSatisfaction = nextSatisfaction - satisfaction;

        this.satisfaction = nextSatisfaction;

        nar.emotion.happy(deltaSatisfaction);
    }


    @Override
    public final ConceptPolicy policy() {
        return policy;
    }

    @Override public final void policy(@NotNull ConceptPolicy p) {
        this.policy = p;
        linkCapacity(p);
        beliefCapacity(p);
        questionCapacity(p);
    }

    protected void questionCapacity(@NotNull ConceptPolicy p) {
        questions().capacity((byte)p.questionCap(true));
        quests().capacity((byte)p.questionCap(false));
    }

    protected void beliefCapacity(@NotNull ConceptPolicy p) {
        beliefs().capacity(
                (byte)p.beliefCap(this, true, true),
                p.beliefCap(this, true, false));
        goals().capacity(
                (byte)p.beliefCap(this, false, true),
                p.beliefCap(this, false, false));
    }



    /**
     * To answer a quest or q by existing beliefs
     *
     * @param q   The task to be processed
     * @param nar
     * @param displaced
     * @return the relevant task
     */
    public Task processQuestion(@NotNull Task q, @NotNull NAR nar, List<Task> displaced) {

        final QuestionTable questionTable;
        final BeliefTable answerTable;
        if (q.isQuestion()) {
            //if (questions == null) questions = new ArrayQuestionTable(nar.conceptQuestionsMax.intValue());
            questionTable = questionsOrNew();
            answerTable = beliefs();
        } else { // else if (q.isQuest())
            //if (quests == null) quests = new ArrayQuestionTable(nar.conceptQuestionsMax.intValue());
            questionTable = questsOrNew();
            answerTable = goals();
        }


        return questionTable.add(q, answerTable, displaced, nar);
    }

    @Override
    public void linkAny(@NotNull Budgeted b, float scale, float minScale, @NotNull NAR nar, @Nullable MutableFloat conceptOverflow) {
        linkSubs(b, scale, minScale, nar, conceptOverflow);
        //linkPeers(b, scale, nar, false);
    }


    /** link to subterms, hierarchical downward */
    public void linkSubs(@NotNull Budgeted b, float scale, float minScale, @NotNull NAR nar, @Nullable MutableFloat conceptOverflow) {



        linkDistribute(b, scale, minScale, nar, templates, conceptOverflow);


//            /*if (subScale >= minScale)*/
//            MutableFloat subConceptOverflow = new MutableFloat(/*0*/);
//
//            //int numUnder = 0;
//
//            linkDistribute(b, scale, minScale, nar, templates, subConceptOverflow);
//
//
//            float scOver = subConceptOverflow.floatValue();
//            if (scOver > minScale) {
//                // recursive overflow accumulated to callee's overflow
//
//
//                //Simple method: just dispense equal proportion of the overflow to all template concepts equally
//                linkDistribute(b, scOver, minScale, nar, templates, conceptOverflow);
//
//                //TODO More fair method:
////                    //iterate over templates, psuedorandomly by choosing a random start index and visiting each modulo N
////                    int i = nar.random.nextInt(numTemplates);
////                    for (int n = 0; n < numTemplates; n++) {
////                        Termed nt = templates.get((i + n) % numTemplates);
////
////                    }
//
//            }
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


    final void linkDistribute(@NotNull Budgeted b, float scale, float minScale, @NotNull NAR nar, @NotNull TermSet templates, MutableFloat subConceptOverflow) {

        int n = templates.size();
        float tStrength = 1f/n;

        Term[] t = templates.terms();
        for (int i = 0; i < n; i++) {
            Termed tt = t[i];
            float subScale = scale * tStrength;

            //Link the peer termlink bidirectionally
            if (subScale > minScale) { //TODO use a min bound to prevent the iteration ahead of time
                Concept target = AbstractConcept.linkSub(this, tt, b, subScale, true, subConceptOverflow, null, nar);

                if (target!=null && b instanceof Task) {
                    //insert 2nd-order tasklink
                    target.linkTask((Task)b, subScale);
                }
            }
        }
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
    public final Task process(@NotNull final Task task, @NotNull NAR nar, List<Task> displaced) {

        switch (task.punc()) {
            case Symbols.BELIEF:
                return processBelief(task, nar, displaced);

            case Symbols.GOAL:
                return processGoal(task, nar, displaced);

            case Symbols.QUESTION:
                return processQuestion(task, nar, displaced);

            case Symbols.QUEST:
                return processQuest(task, nar, displaced);

            default:
                throw new RuntimeException("Invalid sentence type: " + task);
        }

    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj || term.equals(obj);
    }

    @Override
    public final int hashCode() {
        return term.hashCode();
    }

    @Override
    public final String toString() {
        return term.toString();
    }

    @NotNull
    public Term term(int i) {
        return term.term(i);
    }

    @Override
    public int size() {
        return term.size();
    }

    @Deprecated @Override
    public boolean containsTerm(@NotNull Termlike t) {
        return term.containsTerm(t);
    }

    @Deprecated @Override
    public boolean hasTemporal() {
        return term.hasTemporal();
    }

    @Nullable
    @Deprecated @Override
    public Term termOr(int i, @Nullable Term ifOutOfBounds) {
        return term.termOr(i, ifOutOfBounds);
    }

    @Deprecated @Override
    public boolean and(@NotNull Predicate<Term> v) {
        return term.and(v);
    }

    @Deprecated @Override
    public boolean or(@NotNull Predicate<Term> v) {
        return term.or(v);
    }

    @Deprecated @Override
    public int vars() {
        return term.vars();
    }

    @Deprecated @Override
    public int varIndep() {
        return term.varIndep();
    }

    @Deprecated @Override
    public int varDep() {
        return term.varDep();
    }

    @Deprecated @Override
    public int varQuery() {
        return term.varQuery();
    }

    @Deprecated @Override
    public int varPattern() {
        return term.varPattern();
    }

    @Deprecated @Override
    public int complexity() {
        return term.complexity();
    }

    @Deprecated @Override
    public int structure() {
        return term.structure();
    }

    @Override
    public int volume() {
        return term.volume();
    }
}

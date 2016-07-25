package nars.concept;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import nars.$;
import nars.NAR;
import nars.Symbols;
import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.budget.merge.BudgetMerge;
import nars.budget.policy.ConceptPolicy;
import nars.concept.table.ArrayQuestionTable;
import nars.concept.table.BeliefTable;
import nars.concept.table.DefaultBeliefTable;
import nars.concept.table.QuestionTable;
import nars.link.TermLinkBuilder;
import nars.nar.util.DefaultConceptBuilder;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Termlike;
import nars.term.container.TermSet;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;


public class CompoundConcept<T extends Compound> implements AbstractConcept<T>, Termlike {

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


    final HashMap<Task, Task> tasks = new HashMap<>();

    /**
     * Constructor, called in Memory.getConcept only
     *
     * @param term      A term corresponding to the concept
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


    /**
     * used for setting an explicit OperationConcept instance via java; activates it on initialization
     */
    public CompoundConcept(@NotNull T term, @NotNull NAR n) {
        this(term, (DefaultConceptBuilder) n.index.conceptBuilder(), new HashMap());
    }

    /**
     * default construction by a NAR on conceptualization
     */
    CompoundConcept(@NotNull T term, @NotNull DefaultConceptBuilder b, Map bagMap) {
        this(term, b.termbag(bagMap), b.taskbag(bagMap));
    }


//    @Override
//    public final boolean contains(@NotNull Task t) {
//        return tasks.containsKey(t);
//    }


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

    @Override
    public void delete() {
        termlinks().clear();
        tasklinks().clear();
        beliefs = null;
        goals = null;
        questions = null;
        quests = null;
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
    private final Task processBeliefOrGoal(@NotNull Task belief, @NotNull NAR nar, @NotNull BeliefTable target, @NotNull QuestionTable questions, List<Task> displaced) {

        //this may be helpful but we need a different way of applying it to keep the two table's ranges consistent

//        if (belief.temporal() && (hasBeliefs()&&hasGoals())) {
//
//            //finds the temporal intersection of the two temporal belief tables:
//            //this affects the temporal belief compression's focus in time
//
//            long minT = Long.MAX_VALUE;
//            long maxT = Long.MIN_VALUE;
//
//                //max of the min
//                minT = Math.max( minT,  (((DefaultBeliefTable)beliefs()).temporal.minTime() ));
//                minT = Math.max( minT,  (((DefaultBeliefTable)goals()).temporal.minTime() ));
//
//                //..and min of the max
//                maxT = Math.min( maxT,  (((DefaultBeliefTable)beliefs()).temporal.maxTime() ));
//                maxT = Math.min( maxT,  (((DefaultBeliefTable)goals()).temporal.maxTime() ));
//
//            ((DefaultBeliefTable)beliefs()).temporal.minTime(minT);
//            ((DefaultBeliefTable)beliefs()).temporal.maxTime(maxT);
//            ((DefaultBeliefTable)goals()).temporal.minTime(minT);
//            ((DefaultBeliefTable)goals()).temporal.maxTime(maxT);
//        }

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

    @Override
    public final void policy(@Nullable ConceptPolicy p) {
        ConceptPolicy current = this.policy;
        if (current!=p) {
            if ((this.policy = p) != null) {
                linkCapacity(p);
                //synchronized (tasks) {
                beliefCapacity(p);
                questionCapacity(p);
                //}
            }
        }
    }


    private final void removeAndDelete(@NotNull List<Task> tt) {

        int s = tt.size();
        if (s == 0)
            return;

        synchronized (this.tasks) {
            for (int i = 0; i < s; i++) {
                Task x = tt.get(i), y;
                if ((y = this.tasks.remove(x)) == null)
                    throw new RuntimeException(x + " not in tasks map");
                y.delete();
                if (x!=y)
                    x.delete();

                /*
                TODO eternalization for non-deleted temporal tasks that reach here:

                float eternalizationFactor = Param.ETERNALIZE_FORGOTTEN_TEMPORAL_TASKS_CONFIDENCE_FACTOR;
                if (eternalizationFactor > 0f && displaced.size() > 0 && eternal.capacity() > 0) {
                    eternalizeForgottenTemporals(displaced, nar, eternalizationFactor);
                }
                 */

            }
        }

    }

    protected void beliefCapacity(@NotNull ConceptPolicy p) {

        int be = p.beliefCap(this, true, true);
        int bt = p.beliefCap(this, true, false);

        int ge = p.beliefCap(this, false, true);
        int gt = p.beliefCap(this, false, false);

        beliefCapacity(be, bt, ge, gt);
    }

    protected final void beliefCapacity(int be, int bt, int ge, int gt) {
        List<Task> displ = $.newArrayList(0);

        beliefs().capacity(be, bt, displ);
        goals().capacity(ge, gt, displ);

        removeAndDelete(displ);
    }

    protected void questionCapacity(@NotNull ConceptPolicy p) {
        List<Task> displ = $.newArrayList(0);
        questions().capacity((byte) p.questionCap(true), displ);
        quests().capacity((byte) p.questionCap(false), displ);
        removeAndDelete(displ);
    }

    /**
     * To answer a quest or q by existing beliefs
     *
     * @param q         The task to be processed
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


    /**
     * link to subterms, hierarchical downward
     */
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
        float tStrength = 1f / n;

        Term[] t = templates.terms();
        for (int i = 0; i < n; i++) {
            Termed tt = t[i];
            float subScale = scale * tStrength;

            //Link the peer termlink bidirectionally
            if (subScale > minScale) { //TODO use a min bound to prevent the iteration ahead of time
                Concept target = AbstractConcept.linkSub(this, tt, b, subScale, true, subConceptOverflow, null, nar);

                if (target != null && b instanceof Task) {
                    //insert 2nd-order tasklink
                    target.linkTask((Task) b, subScale);
                }
            }
        }
    }

    public static final String DUPLICATE_BELIEF_GOAL = "Duplicate Belief/Goal";
    public static final BudgetMerge DuplicateMerge = BudgetMerge.max; //this should probably always be max otherwise incoming duplicates may decrease the existing priority

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
    public final Task process(@NotNull Task input, @NotNull NAR nar) {


        List<Task> displaced = $.newArrayList(1);

        Task output;

        /* if a duplicate exists, it will merge the incoming task and return true.
          otherwise false */
        synchronized (tasks) {

            //checkConsistency(); //TEMPORARY =-=============

            Task existing = tasks.putIfAbsent(input, input);
            if (existing != null) {
                if (existing != input) {
                    DuplicateMerge.merge(existing.budget(), input, 1f);
                    input.delete(DUPLICATE_BELIEF_GOAL);
                }

                //checkConsistency(); //TEMPORARY =-=============

                return existing;
            }


            switch (input.punc()) {
                case Symbols.BELIEF:
                    output = processBelief(input, nar, displaced);
                    break;

                case Symbols.GOAL:
                    output = processGoal(input, nar, displaced);
                    break;

                case Symbols.QUESTION:
                    output = processQuestion(input, nar, displaced);
                    break;

                case Symbols.QUEST:
                    output = processQuest(input, nar, displaced);
                    break;

                default:
                    throw new RuntimeException("Invalid sentence type: " + input);
            }


            if (output == null) {
                //which was added above
                displaced.add(input);
            } else {
                if (input == output)
                    displaced.remove(input); //incase it or an equivalent was added to the displacement list
                else {
                    throw new RuntimeException(input + " task was transformed " + output);
                }
            }

            removeAndDelete(displaced);

            //checkConsistency(); //TEMPORARY =-=============

        }


        return output;
    }

    private void checkConsistency() {
        synchronized (tasks) {
            int mapSize = tasks.size();
            int tableSize = beliefs().size() + goals().size() + questions().size() + quests().size();

            int THRESHOLD = 50; //to catch when the table explodes and not just an off-by-one inconsistency that will correct itself in the next cycle
            if (Math.abs(mapSize - tableSize) > THRESHOLD) {
                //List<Task> mapTasks = new ArrayList(tasks.keySet());
                Set<Task> mapTasks = tasks.keySet();
                ArrayList<Task> tableTasks = Lists.newArrayList(
                        Iterables.concat(beliefs(), goals(), questions(), quests())
                );
                //Collections.sort(mapTasks);
                //Collections.sort(tableTasks);

                System.err.println(mapSize + " vs " + tableSize + "\t\t" + mapTasks.size() + " vs " + tableTasks.size());
                System.err.println(Joiner.on('\n').join(mapTasks));
                System.err.println("----");
                System.err.println(Joiner.on('\n').join(tableTasks));
                System.err.println("----");
            }
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

    @Deprecated
    @Override
    public boolean containsTerm(@NotNull Termlike t) {
        return term.containsTerm(t);
    }

    @Deprecated
    @Override
    public boolean hasTemporal() {
        return term.hasTemporal();
    }

    @Nullable
    @Deprecated
    @Override
    public Term termOr(int i, @Nullable Term ifOutOfBounds) {
        return term.termOr(i, ifOutOfBounds);
    }

    @Deprecated
    @Override
    public boolean and(@NotNull Predicate<Term> v) {
        return term.and(v);
    }

    @Deprecated
    @Override
    public boolean or(@NotNull Predicate<Term> v) {
        return term.or(v);
    }

    @Deprecated
    @Override
    public int vars() {
        return term.vars();
    }

    @Deprecated
    @Override
    public int varIndep() {
        return term.varIndep();
    }

    @Deprecated
    @Override
    public int varDep() {
        return term.varDep();
    }

    @Deprecated
    @Override
    public int varQuery() {
        return term.varQuery();
    }

    @Deprecated
    @Override
    public int varPattern() {
        return term.varPattern();
    }

    @Deprecated
    @Override
    public int complexity() {
        return term.complexity();
    }

    @Deprecated
    @Override
    public int structure() {
        return term.structure();
    }

    @Override
    public int volume() {
        return term.volume();
    }
}

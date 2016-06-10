package nars.concept;

import nars.Global;
import nars.NAR;
import nars.Symbols;
import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.budget.policy.ConceptBudgeting;
import nars.concept.table.ArrayQuestionTable;
import nars.concept.table.BeliefTable;
import nars.concept.table.DefaultBeliefTable;
import nars.concept.table.QuestionTable;
import nars.link.TermLinkBuilder;
import nars.link.TermTemplate;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.compound.GenericCompound;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.util.List;
import java.util.Map;


public class CompoundConcept extends GenericCompound<Term> implements AbstractConcept , Compound<Term> {

    private final Bag<Task> taskLinks;
    private final Bag<Termed> termLinks;

    /**
     * how incoming budget is merged into its existing duplicate quest/question
     */

    @Nullable Reference<List<TermTemplate>> termLinkTemplates;

    @Nullable
    private final QuestionTable questions;
    @Nullable
    private final QuestionTable quests;
    @Nullable
    private final BeliefTable beliefs;
    @Nullable
    private final BeliefTable goals;

    private float satisfaction = 0;
    private @NotNull Map meta;


    /**
     * Constructor, called in Memory.getConcept only
     *  @param term      A term corresponding to the concept
     * @param termLinks
     * @param taskLinks
     */
    public CompoundConcept(@NotNull Compound term, @NotNull Bag<Termed> termLinks, @NotNull Bag<Task> taskLinks) {
        super(term.op(), term.relation(), term.subterms());

//        if (!term.isNormalized())
//            throw new RuntimeException(term + " unnormalized");
        setNormalized();

        this.termLinks = termLinks;
        this.taskLinks = taskLinks;

        beliefs = newBeliefTable();
        goals = newGoalTable();
        questions = newQuestionTable();
        quests = newQuestionTable();

    }

    @Override
    public void setMeta(@NotNull Map newMeta) {
        this.meta = newMeta;
    }

    @Override
    public @Nullable Map<Object, Object> meta() {
        return meta;
    }

    @Override
    public @NotNull Bag<Task> tasklinks() {
        return taskLinks;
    }

    @Override
    public @NotNull Bag<Termed> termlinks() {
        return termLinks;
    }

    /** used for questions and quests */
    @NotNull protected QuestionTable newQuestionTable() {
        return new ArrayQuestionTable(1);
    }

//    public CompoundConcept(@NotNull String compoundTermString, @NotNull NAR n) throws Narsese.NarseseException {
//        this((Compound) $.$(compoundTermString), n);
//    }


    /** used for setting an explicit OperationConcept instance via java; activates it on initialization */
    public CompoundConcept(@NotNull Compound term, @NotNull NAR n) {
        this(term, n.index.conceptBuilder());
    }

    /** default construction by a NAR on conceptualization */
    public CompoundConcept(@NotNull Compound term, @NotNull ConceptBuilder b) {
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
        return (quests);
    }

    @NotNull
    @Override
    public final QuestionTable questions() {
        return (questions);
    }


    /**
     * Judgments directly made about the term Use ArrayList because of access
     * and insertion in the middle
     */
    @NotNull
    @Override
    public BeliefTable beliefs() {
        return (beliefs);
    }

    /**
     * Desire values on the term, similar to the above one
     */
    @NotNull
    @Override
    public BeliefTable goals() {
        return (goals);
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
    @Nullable
    public Task processBelief(@NotNull Task belief, @NotNull NAR nar) {
        return processBeliefOrGoal(belief, nar, beliefs, questions);
    }

    /**
     * To accept a new goal, and check for revisions and realization, then
     * decide whether to actively pursue it
     * Returns null if the task was not accepted, else the goal which was accepted and somehow modified the state of this concept
     */
    @Nullable
    public Task processGoal(@NotNull Task goal, @NotNull NAR nar) {
        return processBeliefOrGoal(goal, nar, goals, quests);
    }

    /**
     * @return null if the task was not accepted, else the goal which was accepted and somehow modified the state of this concept
     * TODO remove synchronized by lock-free technique
     */
    synchronized private final Task processBeliefOrGoal(@NotNull Task belief, @NotNull NAR nar, @NotNull BeliefTable target, @NotNull QuestionTable questions) {
        //synchronized (target) {
            Task b = target.add(belief, questions, nar);
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

    @NotNull
    protected BeliefTable newBeliefTable() {
        //2 for a task and a pending revision
        return new DefaultBeliefTable(2,2);
    }
    @NotNull
    protected BeliefTable newGoalTable() {
        return new DefaultBeliefTable(2,2);
    }


    @Override public void capacity(@NotNull ConceptBudgeting p) {
        linkCapacity(p);
        beliefCapacity(p);
    }

    protected void beliefCapacity(@NotNull ConceptBudgeting p) {
        beliefs().capacity(p.beliefCap(this, true, true), p.beliefCap(this, true, false));
        goals().capacity(p.beliefCap(this, false, true), p.beliefCap(this, false, false));
    }



    /**
     * To answer a quest or q by existing beliefs
     *
     * @param q   The task to be processed
     * @param nar
     * @return the relevant task
     */
    @Nullable
    public Task processQuestion(@NotNull Task q, @NotNull NAR nar) {

        final QuestionTable questionTable;
        final BeliefTable answerTable;
        if (q.isQuestion()) {
            //if (questions == null) questions = new ArrayQuestionTable(nar.conceptQuestionsMax.intValue());
            questionTable = questions();
            answerTable = beliefs();
        } else { // else if (q.isQuest())
            //if (quests == null) quests = new ArrayQuestionTable(nar.conceptQuestionsMax.intValue());
            questionTable = quests();
            answerTable = goals();
        }


        return questionTable.add(q, answerTable, nar);
    }

    @Override
    public void linkAny(Budgeted b, float scale, float minScale, NAR nar, @Nullable MutableFloat conceptOverflow) {
        linkSubs(b, scale, minScale, nar, conceptOverflow);
        //linkPeers(b, scale, nar, false);
    }


    /** link to subterms, hierarchical downward */
    public void linkSubs(@NotNull Budgeted b, float scale, float minScale, @NotNull NAR nar, @Nullable MutableFloat conceptOverflow) {
        //3. Link the termlink templates
        List<TermTemplate> templates = Global.dereference(this.termLinkTemplates);
        if (templates == null) {
            templates = TermLinkBuilder.buildFlat(this, nar);
//                if (this.termLinkTemplates!=null) {
//                    System.err.println("GC'd");
//                }
            this.termLinkTemplates = Global.reference(templates);
        } /*else {
//                if (this.termLinkTemplates!=null) {
//                    System.err.println("exist");
//                }
        }*/

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

    final void linkDistribute(@NotNull Budgeted b, float scale, float minScale, @NotNull NAR nar, @NotNull List<TermTemplate> templates, MutableFloat subConceptOverflow) {
        for (int i = 0, templatesSize = templates.size(); i < templatesSize; i++) {
            TermTemplate tt = templates.get(i);
            float subScale = scale * tt.strength;

            //Link the peer termlink bidirectionally
            if (subScale > minScale) { //TODO use a min bound to prevent the iteration ahead of time
                Concept target = AbstractConcept.linkSub(this, tt.term, b, subScale, true, subConceptOverflow, null, nar);

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
    @Nullable
    public final Task process(@NotNull final Task task, @NotNull NAR nar) {

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

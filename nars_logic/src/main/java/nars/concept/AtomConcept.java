package nars.concept;

import nars.NAR;
import nars.bag.Bag;
import nars.budget.Budget;
import nars.concept.util.BeliefTable;
import nars.concept.util.TaskTable;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;

/**
 * Created by me on 9/2/15.
 */
public class AtomConcept extends AbstractConcept  {

    protected final Bag<Task> taskLinks;
    protected final Bag<Termed> termLinks;


//    /** creates with no termlink and tasklink ability */
//    public AtomConcept(Term atom, Budget budget) {
//        this(atom, budget, new NullBag(), new NullBag());
//    }

    public AtomConcept(Term atom, Bag<Termed> termLinks, Bag<Task> taskLinks) {
        super(atom);
        this.termLinks = termLinks;
        this.taskLinks = taskLinks;
    }



    /**
     * Task links for indirect processing
     */
    @Override
    public final Bag<Task> getTaskLinks() {
        return taskLinks;
    }

    /**
     * Term links between the term and its components and compounds; beliefs
     */
    @Override
    public final Bag<Termed> getTermLinks() {
        return termLinks;
    }


    @Override
    public BeliefTable getBeliefs() {
        return BeliefTable.EMPTY;
    }

    @Override
    public BeliefTable getGoals() {
        return BeliefTable.EMPTY;
    }

    @Override
    public TaskTable getQuestions() {
        return BeliefTable.EMPTY;
    }

    @Override
    public TaskTable getQuests() {
        return BeliefTable.EMPTY;
    }

    static final String shouldntProcess = "should not have attempted to process task here";

    @Override
    public Task processBelief(Task task, NAR nar) {
        throw new RuntimeException(shouldntProcess);
    }
    @Override
    public Task processGoal(Task task, NAR nar) {
        throw new RuntimeException(shouldntProcess);
    }
    @Override
    public boolean processQuestion(Task task, NAR nar) {
        throw new RuntimeException(shouldntProcess);
    }
    @Override
    public final boolean processQuest(Task task, NAR nar) {
        return processQuestion(task, nar );
    }


    /** atoms have no termlink templates, they are irreducible */
    @Override public Termed[] getTermLinkTemplates() {
        return null;
    }

    /**
     * when a task is processed, a tasklink
     * can be created at the concept of its term
     */
    @Override public final boolean link(Task t, float scale, float minScale, NAR nar) {

        //activate tasklink locally
        Budget taskBudget = t.getBudget();

        getTaskLinks().put(t, taskBudget, scale);

        Termed[] templates = getTermLinkTemplates();
        if (templates == null) return false;

        int numTemplates = templates.length;
        if (numTemplates == 0) return false;

        float subScale = scale / numTemplates;
        if (subScale < minScale)
            return false;

        for (Termed linkTemplate : templates) {

            Concept templateConcept = nar.conceptualize(linkTemplate, taskBudget, subScale);
            if (templateConcept == null) {
                continue;
            }

            linkTemplate(t, templateConcept, taskBudget, minScale, subScale, nar);

        }

        //linkTemplates(t.getBudget(), scale, nar);

        return true;
    }

    protected final void linkTemplate(Task t, Concept target, Budget b, float minScale, float subScale, NAR nar) {

        /** recursively activate the template's task tlink */
        target.link(t, subScale, minScale, nar);

        /** activate local's termlink to template */
        termLinks.put(target, b, subScale);

        /** activate (reverse) template's termlink to local */
        target.getTermLinks().put(this, b, subScale);
    }


    @Override
    public Task process(Task task, NAR nar) {
        return null;
    }
}

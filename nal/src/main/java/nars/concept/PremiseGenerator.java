package nars.concept;

import nars.Global;
import nars.NAR;
import nars.Op;
import nars.bag.BLink;
import nars.bag.Bag;
import nars.budget.Forget;
import nars.concept.table.BeliefTable;
import nars.data.Range;
import nars.nal.meta.PremiseEval;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import nars.util.data.MutableInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static nars.nal.Tense.ETERNAL;

/**
 * TODO abstraction for dynamic link iterator / generator allowing a concept to
 *      programmatically supply the tasklinks/termlinks it fires.  bag selection being
 *      the default but overridable on a per-concept basis.
 *      ex:
 *         --ranges of values (numbers, strings, etc..)
 *         --without and/or with memory of prior iterations from which to continue
 *              (ex: database cursors)
 *
 *  may determine the iteration "power" according to
 *       some budgeting feature (ex: Concept BLink)
 */
abstract public class PremiseGenerator implements Consumer<BLink<? extends Concept>> {

    // Note:  this implementation is not thread safe; call from only one thread at a time */


    @NotNull
    public final NAR nar;


    /**
     * re-used, not to be used outside of this
     */
    protected final
    @NotNull
    PremiseEval matcher;

//    private BLink<? extends Termed> termLink;
//    private BLink<? extends Concept> concept;
//    @Nullable
//    private BLink<? extends Task> taskLink;
//
    //private final Map<Term, Task> beliefCache = Global.newHashMap();
    //long lastMatch = Tense.TIMELESS;

    @NotNull
    public final Forget.BudgetForgetFilter<Task> taskLinkForget;
    @NotNull
    public final Forget.BudgetForget<Termed> termLinkForget;

    @Range(min = 0, max = 16, unit = "TaskLink") //TODO use float percentage
    public final MutableInteger tasklinksFiredPerFiredConcept = new MutableInteger(1);

    @Range(min = 0, max = 16, unit = "TermLink")
    public final MutableInteger termlinksFiredPerFiredConcept = new MutableInteger(1);




    public PremiseGenerator(@NotNull NAR nar, @NotNull PremiseEval matcher, @NotNull Forget.BudgetForgetFilter<Task> taskLinkForget, @NotNull Forget.BudgetForget<Termed> termLinkForget) {

        this.nar = nar;
        this.matcher = matcher;
        this.taskLinkForget = taskLinkForget;
        this.termLinkForget = termLinkForget;
    }

    /**
     * temporary re-usable array for batch firing
     */
    private final Collection<BLink<? extends Termed>> terms =
            Global.newArrayList();
            //Global.newHashSet(1);

    /**
     * temporary re-usable array for batch firing
     */
    private final Collection<BLink<Task>> tasks =
            Global.newArrayList();
            //Global.newHashSet(1);

    @NotNull
    private BLink[] termsArray = new BLink[0];
    @NotNull
    private BLink[] tasksArray = new BLink[0];

    @Override
    public final void accept(@NotNull BLink<? extends Concept> c) {
        firePremiseSquared(
            c,
            tasklinksFiredPerFiredConcept.intValue(),
            termlinksFiredPerFiredConcept.intValue()
        );
    }

    /** to be overridden by subclasses, called on each frame to update parameters */
    public void frame(NAR nar) {
    }

    /**
     * main entry point:
     * iteratively supplies a matrix of premises from the next N tasklinks and M termlinks
     * (recycles buffers, non-thread safe, one thread use this at a time)
     */
    final void firePremiseSquared(
            @NotNull BLink<? extends Concept> conceptLink,
            int tasklinks, int termlinks) {

        Concept concept = conceptLink.get();

        Bag<Task> taskLinks = concept.tasklinks();
        if (taskLinks.filter(taskLinkForget).commit().isEmpty())
            return; //no tasklinks

        Bag<Termed> termLinks = concept.termlinks();
        if (termLinks.forEachThen(termLinkForget).commit().isEmpty())
            return; //no termlinks

        Collection<BLink<? extends Termed>> termsBuffer;
        termsBuffer = this.terms;
        termsBuffer.clear();
        termLinks.sample(termlinks, termsBuffer::add);
        assert (!termsBuffer.isEmpty());


        Collection<BLink<Task>> tasksBuffer;
        tasksBuffer = this.tasks;
        tasksBuffer.clear();
        taskLinks.sample(tasklinks, tasksBuffer::add);
        assert (!tasksBuffer.isEmpty());


        //convert to array for fast for-within-for iterations
        BLink<Task>[] tasksArray = this.tasksArray = tasksBuffer.toArray(this.tasksArray);

        BLink<Termed>[] termsArray = this.termsArray = termsBuffer.toArray(this.termsArray);

        for (BLink<Task> taskLink : tasksArray) {
            if (taskLink == null) break; //null-terminated array, ends


            premiseTask(conceptLink, termsArray, taskLink);

        }

    }

    /** begin matching the task half of a premise */
    private void premiseTask(@NotNull BLink<? extends Concept> concept, @Nullable BLink<Termed>[] termsArray, BLink<Task> taskLink) {

        Task task = taskLink.get();

        long occ = task.occurrence();

        Compound taskTerm = task.term();

        for (BLink<Termed> termLink : termsArray) {

            if (termLink == null || taskLink.isDeleted() || task.isDeleted())
                break; //end of termsArray, or task has become deleted in the previous iteration, cancel

            Termed tl = termLink.get();
            Term termLinkTerm  = tl.term();


            if (!Terms.equalSubTermsInRespectToImageAndProduct( taskTerm, termLinkTerm )) {

                matcher.run(
                    newPremise(concept, taskLink, termLink,
                            match(task, tl, occ) //TODO cache this, if occ is same then the belief probably is also, in same cycle
                    )
                );
            }
        }
    }

    @NotNull
    protected ConceptProcess newPremise(BLink<? extends Concept> concept, BLink<? extends Task> taskLink, BLink<? extends Termed> termLink, Task belief) {
        return new DefaultConceptProcess(nar, taskLink, termLink, belief, nar::process);
    }

    /** resolves the most relevant belief of a given term/concept */
    @Nullable
    public Task match(@NotNull Task task, @NotNull Termed beliefConceptTerm, long taskOcc) {

        //atomic concepts will have no beliefs to match
        if (!(beliefConceptTerm instanceof Compound))
            return null;

        //DEBUG---
        if (task.isDeleted())
            throw new RuntimeException("Deleted task: " + task);
        //----

        Concept beliefConcept = nar.concept(beliefConceptTerm);

        @Nullable BeliefTable table = task.isQuest() ? beliefConcept.goals() : beliefConcept.beliefs();

        if (table.isEmpty()) {
            return null;
        }

        Task belief = table.match(task.term(), taskOcc, nar);
        if (belief == null)
            return null;

        if (task.isQuestOrQuestion()) {

            //project the belief to the question's time
            if (taskOcc!=ETERNAL) {
                belief = belief.projectMatched(task /*question*/, nar, Global.TRUTH_EPSILON);
            }

            if (belief!=null) { //may have become null as a result of projection

                //attempt to Unify any Query variables; answer if unifies
                if (task.term().hasVarQuery()) {
                    matchQueryQuestion(task, belief);
                } else if (task.term().equalsAnonymously(beliefConcept.term())) {
                    nar.answer(task, belief);
                }

            }


        }

        return belief;


    }

    public void matchQueryQuestion(@NotNull Task task, Task belief) {
        List<Termed> result = Global.newArrayList(1);
        new UnifySubst(Op.VAR_QUERY, nar, result, 1).matchAll(
                task.term(), belief.term()
        );
        if (!result.isEmpty()) {
            nar.answer(task, belief);
        }
    }


//    /** uses the query-unified term to complete a premise */
//    @Override public final boolean accept(@NotNull Term beliefTerm, Term unifiedBeliefTerm) {
//
//        Task belief = beliefTerm instanceof Compound ?
//
//                //beliefCache.computeIfAbsent(beliefTerm, this) :
//                apply(beliefTerm) :
//
//                null; //atomic terms will have no beliefs anyway
//
//        //if the unified belief term is different, then clone the known belief with it as the new belief
//        if (belief!=null && (unifiedBeliefTerm instanceof Compound) && !beliefTerm.equals(unifiedBeliefTerm)) {
//            Task unifiedBelief = new MutableTask(belief, (Compound)unifiedBeliefTerm).normalize(memory);
//            if (unifiedBelief!=null) {
//                belief = unifiedBelief;
//            }
//
//        }
//
//        //TODO also modify the termlink / "belief term" in the premise? or leave as variable
//
//        premise(concept, taskLink, termLink, belief);
//
////        Task task = taskLink.get();
////        //Report questions/solution pairs involving query variable that have been matched
////        if (belief != null && task.isQuestOrQuestion() && !belief.isQuestOrQuestion())
////            memory.onSolve(task, belief);
//
//        return true;
//    }



}

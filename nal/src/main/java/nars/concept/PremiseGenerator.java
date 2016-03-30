package nars.concept;

import nars.Global;
import nars.NAR;
import nars.bag.BLink;
import nars.bag.Bag;
import nars.budget.Forget;
import nars.data.Range;
import nars.nal.Tense;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import nars.util.data.MutableInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

/** not thread safe; call from only one thread at a time */
abstract public class PremiseGenerator /*extends UnifySubst */implements Function<Term, Task>, Consumer<BLink<? extends Concept>> {

    @NotNull
    public final NAR nar;
//    private BLink<? extends Termed> termLink;
//    private BLink<? extends Concept> concept;
//    @Nullable
//    private BLink<? extends Task> taskLink;
//
    //private final Map<Term, Task> beliefCache = Global.newHashMap();
    long lastMatch = Tense.TIMELESS;

    @NotNull
    public final Forget.BudgetForgetFilter<Task> taskLinkForget;
    @NotNull
    public final Forget.BudgetForget<Termed> termLinkForget;

    @Range(min = 0, max = 16, unit = "TaskLink") //TODO use float percentage
    public final MutableInteger tasklinksFiredPerFiredConcept = new MutableInteger(1);

    @Range(min = 0, max = 16, unit = "TermLink")
    public final MutableInteger termlinksFiredPerFiredConcept = new MutableInteger(1);



    public PremiseGenerator(@NotNull NAR nar, @NotNull Forget.BudgetForgetFilter<Task> taskLinkForget, @NotNull Forget.BudgetForget<Termed> termLinkForget) {
        //super(Op.VAR_QUERY, nar, true);
        this.nar = nar;
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
    public final void accept(BLink<? extends Concept> c) {
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
        //if (termsBuffer.isEmpty()) return; //no termlinks sampled
        assert (!termsBuffer.isEmpty());


        Collection<BLink<Task>> tasksBuffer;
        tasksBuffer = this.tasks;
        tasksBuffer.clear();
        taskLinks.sample(tasklinks, tasksBuffer::add);
        //if (tasksBuffer.isEmpty()) return; //no tasklink available
        assert (!tasksBuffer.isEmpty());


        //convert to array for fast for-within-for iterations
        BLink[] tasksArray = this.tasksArray = tasksBuffer.toArray(this.tasksArray);

        BLink[] termsArray = this.termsArray = termsBuffer.toArray(this.termsArray);

        for (BLink<? extends Termed> termLink : (BLink<? extends Termed>[]) termsArray) {

            if (termLink == null) break;

            match(conceptLink, tasksArray, termLink);

        }

    }

    final void match(BLink<? extends Concept> concept, @NotNull BLink<? extends Task>[] taskLinks, @NotNull BLink<? extends Termed> termLink) {

        for (BLink<? extends Task> taskLink : taskLinks) {

            if (taskLink == null)
                break; //null-terminated array, ends

            Task task = taskLink.get();
            if (task.isDeleted())
                continue;

            Term termLinkTerm  = termLink.get().term();
            if (Terms.equalSubTermsInRespectToImageAndProduct(task.term(), termLinkTerm ))
                continue;

            //matchAll(taskLinkTerm, termLinkTerm );
            premise(concept, taskLink, termLink, apply(termLinkTerm));
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

    abstract protected void premise(BLink<? extends Concept> concept, BLink<? extends Task> taskLink, BLink<? extends Termed> termLink, Task belief);

    /** resolves the most relevant belief of a given term/concept */
    @Nullable
    @Override public final Task apply(@NotNull Term beliefTerm) {

        Concept beliefConcept = nar.concept(beliefTerm);
        if ((beliefConcept != null) && (beliefConcept.hasBeliefs())) {

            Task belief = beliefConcept.beliefs().top(nar.time());

            assert(belief != null && !belief.isDeleted());
            //  if (belief == null || belief.isDeleted())  throw new RuntimeException("Deleted belief: " + belief + " " + beliefConcept.hasBeliefs());

            return belief;
        }

        return null;
    }

}

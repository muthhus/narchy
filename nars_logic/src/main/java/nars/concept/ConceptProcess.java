/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.concept;

import nars.Global;
import nars.NAR;
import nars.Op;
import nars.Premise;
import nars.bag.BLink;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Stamp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/** Firing a concept (reasoning event). Derives new Tasks via reasoning rules
 *
 *  Concept
 *     Task
 *     TermLinks
 *
 * */
public final class ConceptProcess implements Premise {


    public final NAR nar;
    public final BLink<Task> taskLink;
    public final BLink<Concept> conceptLink;
    public final BLink<Termed> termLink;

    @Nullable
    private final Task belief;
    private final boolean cyclic;

    @Override
    public final Task getTask() {
        return taskLink.get();
    }



    public Concept getConcept() {
        return conceptLink.get();
    }

    public ConceptProcess(NAR nar, BLink<Concept> conceptLink, BLink<Task> taskLink, BLink<Termed> termLink, @Nullable Task belief) {
        this.nar = nar;

        this.taskLink = taskLink;
        this.conceptLink = conceptLink;
        this.termLink = termLink;

        this.belief = belief;
        this.cyclic = (belief != null) && Stamp.overlapping(getTask(), belief);
    }


    public static int fireAll(@NotNull NAR nar, BLink<Concept> concept, @NotNull BLink<Task> taskLink, @NotNull BLink<Termed> termLink, @NotNull Consumer<ConceptProcess> cp) {

        Task task = taskLink.get();
        Term tel = termLink.get().term();
        if (tel!=null) /*&& (task.term().hasVarQuery()))*/ {
             Premise.unify(Op.VAR_QUERY, task.term(), tel, nar.memory, (u) -> {
                 if (!tel.equals(u)) {
                     fireAll(nar, concept, taskLink, termLink, u, cp);
                 }
             });
        }

        fireAll(nar, concept, taskLink, termLink, tel, cp);

        return 1; //HACK
    }

    public static void fireAll(@NotNull NAR nar, BLink<Concept> concept, @NotNull BLink<Task> taskLink, @NotNull BLink<Termed> termLink, Term beliefTerm, @NotNull Consumer<ConceptProcess> cp) {
        Concept beliefConcept = nar.concept(beliefTerm);

        Task belief = null;
        if ((beliefConcept != null) && (beliefConcept.hasBeliefs())) {
            belief = beliefConcept.getBeliefs().top(nar.time());
            if (belief == null || belief.getDeleted()) {
                throw new RuntimeException("deleted belief: " + belief + " " + beliefConcept.hasBeliefs());
            }
        }

        cp.accept(new ConceptProcess(nar, concept,
                taskLink, termLink, belief));
    }

//    /**
//     * @return the current termLink aka BeliefLink
//     */
//    @Override
//    public final BagBudget<Termed> getTermLink() {
//        return termLink;
//    }


    public final Termed getBeliefTerm() {
        Task x = getBelief();
        return x== null ? termLink.get() :
                x.term();
    }


//    protected void beforeFinish(final long now) {
//
//        Memory m = nar.memory();
//        m.logic.TASKLINK_FIRE.hit();
//        m.emotion.busy(getTask(), this);
//
//    }

//    @Override
//    final protected Collection<Task> afterDerive(Collection<Task> c) {
//
//        final long now = nar.time();
//
//        beforeFinish(now);
//
//        return c;
//    }


    @Nullable
    @Override
    public final Task getBelief() {
        return belief;
    }

    @Override
    public final boolean isCyclic() {
        return cyclic;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(
                getClass().getSimpleName())
                .append('[').append(conceptLink).append(',')
                            .append(taskLink).append(',')
                            .append(termLink).append(',')
                            .append(getBelief())
                .append(']')
                .toString();
    }

    @Override
    public NAR nar() {
        return nar;
    }

    public int getMaxMatches() {
        final float min = Global.MIN_TERMUTATIONS_PER_MATCH, max = Global.MAX_TERMUTATIONS_PER_MATCH;
        return (int)Math.ceil(getTask().getPriority() * (max-min) + min);
    }

//    /** max(tasktime, belieftime) */
//    public long getMaxOccurrenceTime() {
//        long occ= getTask().getOccurrenceTime();
//        Task b = getBelief();
//        if (b!=null) {
//            occ = Math.max(occ, b.getOccurrenceTime());
//        }
//        return occ;
//    }


    //    /** supplies at most 1 premise containing the pair of next tasklink and termlink into a premise */
//    public static Stream<Task> nextPremise(NAR nar, final Concept concept, float taskLinkForgetDurations, Function<ConceptProcess,Stream<Task>> proc) {
//
//        TaskLink taskLink = concept.getTaskLinks().forgetNext(taskLinkForgetDurations, nar.memory());
//        if (taskLink == null) return Stream.empty();
//
//        TermLink termLink = concept.getTermLinks().forgetNext(nar.memory().termLinkForgetDurations, nar.memory());
//        if (termLink == null) return Stream.empty();
//
//
//        return proc.apply(premise(nar, concept, taskLink, termLink));
//
//    }

//    public static ConceptProcess premise(NAR nar, Concept concept, TaskLink taskLink, TermLink termLink) {
////        if (Terms.equalSubTermsInRespectToImageAndProduct(taskLink.getTerm(), termLink.getTerm()))
////            return null;
//
////        if (taskLink.isDeleted())
////            throw new RuntimeException("tasklink null"); //bag should not have returned this
//
//    }



//    public abstract Stream<Task> derive(final Deriver p);

//    public static void forEachPremise(NAR nar, @Nullable final Concept concept, @Nullable TaskLink taskLink, int termLinks, float taskLinkForgetDurations, Consumer<ConceptProcess> proc) {
//        if (concept == null) return;
//
//        concept.updateLinks();
//
//        if (taskLink == null) {
//            taskLink = concept.getTaskLinks().forgetNext(taskLinkForgetDurations, concept.getMemory());
//            if (taskLink == null)
//                return;
//        }
//
//
//
//
//        proc.accept( new ConceptTaskLinkProcess(nar, concept, taskLink) );
//
//        if ((termLinks > 0) && (taskLink.type!=TermLink.TRANSFORM))
//            ConceptProcess.forEachPremise(nar, concept, taskLink,
//                    termLinks,
//                    proc
//            );
//    }

//    /** generates a set of termlink processes by sampling
//     * from a concept's TermLink bag
//     * @return how many processes generated
//     * */
//    public static int forEachPremise(NAR nar, Concept concept, TaskLink t, final int termlinksToReason, Consumer<ConceptProcess> proc) {
//
//        int numTermLinks = concept.getTermLinks().size();
//        if (numTermLinks == 0)
//            return 0;
//
//        TermLink[] termlinks = new TermLink[termlinksToReason];
//
//        //int remainingProcesses = Math.min(termlinksToReason, numTermLinks);
//
//        //while (remainingProcesses > 0) {
//
//            Arrays.fill(termlinks, null);
//
//            concept.getPremiseGenerator().nextTermLinks(concept, t, termlinks);
//
//            int created = 0;
//            for (TermLink tl : termlinks) {
//                if (tl == null) break;
//
//                proc.accept(
//                    new ConceptTaskTermLinkProcess(nar, concept, t, tl)
//                );
//                created++;
//            }
//
//
//          //  remainingProcesses--;
//
//
//        //}
//
//        /*if (remainingProcesses == 0) {
//            System.err.println(now + ": " + currentConcept + ": " + remainingProcesses + "/" + termLinksToFire + " firings over " + numTermLinks + " termlinks" + " " + currentTaskLink.getRecords() + " for TermLinks "
//                    //+ currentConcept.getTermLinks().values()
//            );
//            //currentConcept.taskLinks.printAll(System.out);
//        }*/
//
//        return created;
//
//    }

//    /** override-able filter for derivations which can be applied
//     * once the term and the truth value are known */
//    public boolean validJudgment(Term derivedTerm, Truth truth) {
//        return true;
//    }
//
//    /** override-able filter for derivations which can be applied
//     * once the term and the truth value are known */
//    public boolean validGoal(Term derivedTerm, Truth truth) {
//        return true;
//    }

}

package nars.nal;

import nars.Global;
import nars.NAR;
import nars.Op;
import nars.concept.Concept;
import nars.concept.table.BeliefTable;
import nars.concept.table.QuestionTable;
import nars.link.BLink;
import nars.nal.meta.PremiseEval;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.subst.UnifySubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static nars.nal.Tense.ETERNAL;

/**
 * Entry point for using the NAL+ Internal Reasoner
 * <p>
 * TODO abstraction for dynamic link iterator / generator allowing a concept to
 * programmatically supply the tasklinks/termlinks it fires.  bag selection being
 * the default but overridable on a per-concept basis.
 * ex:
 * --ranges of values (numbers, strings, etc..)
 * --without and/or with memory of prior iterations from which to continue
 * (ex: database cursors)
 * <p>
 * may determine the iteration "power" according to
 * some budgeting feature (ex: Concept BLink)
 */
public enum PremiseBuilder {
    ;


    /**
     * Main Entry point: begin matching the task half of a premise
     */
    @NotNull
    public static int run(@NotNull NAR nar, BLink<? extends Concept> conceptLink, @NotNull List<BLink<Term>> termsArray, @NotNull BLink<Task> taskLink, PremiseEval matcher) {

        int count = 0;

        Task task = taskLink.get(); //separate the task and hold ref to it so that GC doesnt lose it
        if (task != null) {

            Compound taskTerm = task.term();


            for (int i = 0, termsArraySize = termsArray.size(); i < termsArraySize; i++) {

                if (task.isDeleted())
                    break;

                BLink<? extends Termed> termLink = termsArray.get(i);

                Termed tl = termLink.get();
                if (tl == null)
                    continue;

                Term termLinkTerm = tl.term();

                if (!Terms.equalSubTermsInRespectToImageAndProduct(taskTerm, termLinkTerm)) {
                //if (!taskTerm.equals( termLinkTerm )) {
                    if (matcher.run(
                        newPremise(nar, conceptLink, termLink, taskLink, task, tl)
                    )) {
                        count++;
                    }
                } /*else {
                    if (!taskTerm.equals(termLinkTerm))
                        System.err.println(taskTerm + "\n" + termLinkTerm + "\n\tunmatchable");
                }*/
            }
        }

        return count;
    }

    @Nullable
    static ConceptProcess newPremise(@NotNull NAR nar, BLink<? extends Concept> conceptLink, BLink<? extends Termed> termLink, BLink<Task> taskLink, @NotNull Task task, @NotNull Termed tl) {
        return new ConceptProcess(conceptLink, taskLink, termLink, match(nar, task, tl));
    }


    /**
     * resolves the most relevant belief of a given term/concept
     *
     *   patham9 project-eternalize
         patham9 depending on 4 cases
         patham9 https://github.com/opennars/opennars2/blob/a143162a559e55c456381a95530d00fee57037c4/src/nal/deriver/projection_eternalization.clj
         sseehh__ ok ill add that in a bit
         patham9 you need  project-eternalize-to
         sseehh__ btw i disabled immediate eternalization entirely
         patham9 so https://github.com/opennars/opennars2/blob/a143162a559e55c456381a95530d00fee57037c4/src/nal/deriver/projection_eternalization.clj#L31
         patham9 especially try to understand the "temporal temporal" case
         patham9 its using the result of higher confidence
     */
    @Nullable
    static Task match(@NotNull NAR nar, @NotNull Task task, @NotNull Termed beliefConceptTerm) {

        //atomic concepts will have no beliefs to match
        if (!(beliefConceptTerm instanceof Compound))
            return null;

        Concept beliefConcept = nar.concept(beliefConceptTerm);
        if (beliefConcept == null)
            return null;

        @Nullable BeliefTable table = task.isQuest() ? beliefConcept.goals() : beliefConcept.beliefs();

        if (table.isEmpty())
            return null;

        //Task belief = project(task, table.match(task), nar.time());
        Task belief = table.match(task);

        if (belief!=null && task.isQuestOrQuestion()) {

            long taskOcc = task.occurrence();

            //project the belief to the question's time
            if (taskOcc != ETERNAL) {
                @Nullable Concept cbel = nar.concept(belief);
                belief = cbel != null ? cbel.merge(task, belief, taskOcc, nar) : null;
            }

            if (belief != null) { //may have become null as a result of projection

                //attempt to Unify any Query variables; answer if unifies
                if (task.term().hasVarQuery()) {
                    matchQueryQuestion(nar, task, belief);
                } else if (beliefConcept instanceof Compound && Term.equalAtemporally(task, beliefConcept)) {
                    matchAnswer(nar, task, belief);
                }

            }


        }

        return belief;


    }

//    /**
//           (case [(get-eternal target-time) (get-eternal source-time)]
//                 [:eternal :eternal] t
//                 [:temporal :eternal] t
//                 [:eternal :temporal] (eternalize t)
//                 [:temporal :temporal] (let [t-eternal (eternalize t)
//                         t-project (project-to target-time t cur-time)]
//                         (if (> (confidence t-eternal)
//                             (confidence t-project))
//                             t-eternal
//                             t-project))))))
//     * @param target
//     * @param matched
//     * @return
//     */
//    private static Task project(Task target, Task matched, long now) {
//        boolean me = matched.isEternal();
//        if (me) {
//            return matched;
//        } else {
//            ProjectedTruth newTruth = matched.projectTruth(target.occurrence(), now, true);
//            return new MutableTask(matched, newTruth, now, newTruth.when);
//        }
//    }

    static void matchAnswer(@NotNull NAR nar, @NotNull Task q, Task a) {
        @Nullable Concept c = nar.concept(q);
        if (c != null)
            ((QuestionTable)c.tableFor(q.punc())).answer(a, nar);
    }

    static void matchQueryQuestion(@NotNull NAR nar, @NotNull Task task, @NotNull Task belief) {
        List<Termed> result = Global.newArrayList(1);
        new UnifySubst(Op.VAR_QUERY, nar, result, Global.QUERY_ANSWERS_PER_MATCH).matchAll(
                task.term(), belief.term()
        );
        if (!result.isEmpty()) {
            matchAnswer(nar, task, belief);
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

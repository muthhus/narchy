package nars.nal;

import nars.$;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.concept.Concept;
import nars.concept.table.BeliefTable;
import nars.concept.table.QuestionTable;
import nars.link.BLink;
import nars.nal.meta.PremiseEval;
import nars.task.AnswerTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.subst.UnifySubst;
import nars.util.data.list.FasterList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;

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
    public static int run(@NotNull NAR nar, Concept conceptLink, @NotNull List<Term> termsArray, @NotNull Task task, @NotNull PremiseEval matcher, BiConsumer<Premise,Conclusion> each) {

        int count = 0;
        long now = nar.time();

        Compound taskTerm = task.term();

        for (int i = 0, termsArraySize = termsArray.size(); i < termsArraySize; i++) {

            if (task.isDeleted())
                break;

            Term termLink;
            if (Terms.equalSubTermsInRespectToImageAndProduct(taskTerm, termLink = termsArray.get(i)))
                continue;


            Premise p = newPremise(nar, now, conceptLink, task, termLink);

            Conclusion c = matcher.run( p, new Conclusion() );

            each.accept(p, c);

            count++;
        }

        return count;
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
    static @NotNull Premise newPremise(@NotNull NAR nar, long now, Concept conceptLink, @NotNull Task taskLink, @NotNull Term termLink) {


        Task belief = null;

        if (termLink instanceof Compound) { //atomic concepts will have no beliefs to match

            Concept beliefConcept = nar.concept(termLink);
            if (beliefConcept != null) {

                if ( taskLink.isQuestOrQuestion()) {

                    //TODO is this correct handling for quests? this means a belief task may be a goal which may contradict deriver semantics
                    BeliefTable table = taskLink.isQuest() ? beliefConcept.goals() : beliefConcept.beliefs();

                    Task solution = table.match(taskLink, now);
                    if (solution!=null) {
                        Task answered = answer(nar, taskLink, solution, beliefConcept);
                        if (taskLink.isQuestion())
                            belief = answered;
                    }


                } else {

                    belief = beliefConcept.beliefs().match(taskLink, now);

                }
            }
        }

        return new Premise(conceptLink.term(), taskLink, termLink, belief);
    }

    private static Task answer(@NotNull NAR nar, @NotNull Task taskLink, @NotNull Task solution, @NotNull Concept beliefConcept) {

        long taskOcc = taskLink.occurrence();

        //project the belief to the question's time
        if (taskOcc != ETERNAL) {
            @Nullable Concept cbel = nar.concept(solution);
            solution = cbel != null ? cbel.merge(taskLink, solution, taskOcc, nar) : null;
        }

        if (solution != null) { //may have become null as a result of projection

            //attempt to Unify any Query variables; answer if unifies
            if (taskLink.term().hasVarQuery()) {
                matchQueryQuestion(nar, taskLink, solution);
            } else if (beliefConcept instanceof Compound && Term.equalAtemporally(taskLink, beliefConcept)) {
                matchAnswer(nar, taskLink, solution);
            }

        }
        return solution;
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
        if (a instanceof AnswerTask)
            return; //already an answer

        @Nullable Concept c = nar.concept(q);
        if (c != null) {
            List<Task> displ = $.newArrayList(0);
            ((QuestionTable)c.tableFor(q.punc())).answer(a, nar, displ );
        }
    }

    static void matchQueryQuestion(@NotNull NAR nar, @NotNull Task task, @NotNull Task belief) {
        List<Termed> result = $.newArrayList(1);
        new UnifySubst(Op.VAR_QUERY, nar, result, Param.QUERY_ANSWERS_PER_MATCH).matchAll(
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

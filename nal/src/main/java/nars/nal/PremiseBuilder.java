package nars.nal;

import nars.*;
import nars.budget.Budget;
import nars.budget.RawBudget;
import nars.budget.policy.TaskBudgeting;
import nars.concept.Concept;
import nars.concept.table.BeliefTable;
import nars.concept.table.QuestionTable;
import nars.index.TermIndex;
import nars.link.BLink;
import nars.task.AnswerTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.subst.UnifySubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static nars.budget.Activation.linkable;
import static nars.time.Tense.ETERNAL;

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

    private static final Logger logger = LoggerFactory.getLogger(PremiseBuilder.class);



    public static boolean budget(@NotNull RawBudget pBudget, @NotNull BLink<Task> taskLink, @NotNull BLink<Term> termLink, float minDur) {
        TaskBudgeting.premise(pBudget, taskLink, termLink);
        return pBudget.dur() >= minDur;
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
    public static @NotNull Premise newPremise(@NotNull NAR nar, @NotNull Concept c, long now, @NotNull Task task, @NotNull Term termLinkTerm, Budget b) {


        Task belief = null;


        Term termLinkTermConceptTerm = $.unneg(termLinkTerm).term();
        if (termLinkTerm instanceof Compound && linkable(termLinkTermConceptTerm)) { //atomic concepts will have no beliefs to match

            Concept beliefConcept = nar.concept(termLinkTermConceptTerm);
            if (beliefConcept != null) {

                if ( task.isQuestOrQuestion()) {

                    //TODO is this correct handling for quests? this means a belief task may be a goal which may contradict deriver semantics
                    BeliefTable table = task.isQuest() ? beliefConcept.goals() : beliefConcept.beliefs();

                    Task solution = table.match(task, now);
                    if (solution!=null) {
                        try {
                            Task answered = answer(nar, task, solution, beliefConcept);
                            if (task.isQuestion())
                                belief = answered;
                            else
                                belief = beliefConcept.beliefs().match(task, now); //in case of quest, proceed with matching belief

                        } catch (TermIndex.InvalidConceptException e) {
                            logger.warn("{}", e.getMessage());
                        }

                    }


                } else {

                    belief = beliefConcept.beliefs().match(task, now);

                }
            }
        }

        Premise p = new Premise(c.term(), task, termLinkTerm, belief);
        p.budget(b);
        return p;
    }


    @Nullable
    private static Task answer(@NotNull NAR nar, @NotNull Task taskLink, @NotNull Task solution, @NotNull Concept beliefConcept) {

        long taskOcc = taskLink.occurrence();

        //project the belief to the question's time
        if (taskOcc != ETERNAL) {
            solution = beliefConcept.merge(taskLink, solution, taskOcc, nar);
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
        new UnifySubst(Op.VAR_QUERY, nar, result, Param.QUERY_ANSWERS_PER_MATCH).unifyAll(
                task.term(), belief.term()
        );
        if (!result.isEmpty()) {
            matchAnswer(nar, task, belief);
        }
    }



}

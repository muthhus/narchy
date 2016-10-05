/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.nal;

import nars.*;
import nars.budget.Budget;
import nars.budget.RawBudget;
import nars.concept.Concept;
import nars.link.BLink;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.task.Tasked;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.subst.UnifySubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static nars.nal.UtilityFunctions.or;
import static nars.time.Tense.ETERNAL;

/**
 * Defines the conditions used in an instance of a derivation
 * Contains the information necessary for generating derivation Tasks via reasoning rules.
 *
 * It is meant to be disposable and should not be kept referenced longer than necessary
 * to avoid GC loops, so it may need to be weakly referenced.
 *
 *
 */
public final class Premise extends RawBudget implements Tasked {
    private static final Logger logger = LoggerFactory.getLogger(Premise.class);


    //@NotNull private final Term concept;

    @NotNull public final Task task;

    @NotNull public final Term term;

    @Nullable public final Task belief;

    @NotNull public final Term concept;


    ///** not used in creating a Premise key, because the same premise components may be generated from different originating concepts or even other methods of forming them*/
    //@NotNull transient private final Term conceptLink;


    public Premise(@NotNull Term concept, @NotNull Task taskLink,
                   @NotNull Term termLink,
                   @Nullable Task belief, float p, float d, float q) {

        super(p, d, q);

        this.concept = concept;

        this.task = taskLink;

        //use the belief's term and not the termlink because it is more specific if:
        // a) it contains temporal information that can be used in temporalization
        // b) a variable in the termlink was matched
        this.term = belief!=null ? belief.term() : termLink;

        this.belief = belief;

        //this.conceptLink = conceptLink;



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
    public static @Nullable Premise build(@NotNull NAR nar, @NotNull Concept c, long now, @NotNull Task task, @NotNull Budget taskLinkBudget,
                                          @NotNull BLink<Term> termLink) {

        Term term = termLink.get();
        if (term == null)
            return null;

        if (Terms.equalSubTermsInRespectToImageAndProduct(task.term(), term))
            return null;

        Budget taskBudget = task.budget().clone();
        if (taskBudget==null)
            return null;

        Budget termLinkBudget = termLink.clone();
        if (termLinkBudget==null)
            return null;


        Task belief = null;

        Concept beliefConcept = nar.concept(term);
        if (beliefConcept != null) {


            if ( task.isQuestOrQuestion()) {

                //TODO is this correct handling for quests? this means a belief task may be a goal which may contradict deriver semantics
                BeliefTable table = task.isQuest() ? beliefConcept.goals() : beliefConcept.beliefs();

                belief = table.match(task, now);
                if (belief !=null) {
                    //try {
                        Task answered = answer(nar, task, belief, beliefConcept);

//                            if (answered!=null && !answered.equals(belief)) {
//                                nar.inputLater(answered);
//                            }

                        if (answered!=null && task.isQuestion())
                            belief = answered;

                        if (task.isQuest())
                            belief = beliefConcept.beliefs().match(task, now); //in case of quest, proceed with matching belief


                    /*} catch (InvalidConceptException e) {
                        logger.warn("{}", e.getMessage());
                    }*/

                }


            } else {

                belief = beliefConcept.beliefs().match(task, now);

            }
        }


        Budget beliefBudget;
        if (belief!=null) {
            beliefBudget = belief.budget().clone();
            if (beliefBudget==null)
                belief = null;
        } else {
            beliefBudget = null;
        }

        float dur = belief==null ? taskBudget.dur() :     or(taskBudget.dur() , beliefBudget.dur());
        if (dur < nar.durMin.floatValue())
            return null;

        float pri = or(taskLinkBudget.pri() , termLinkBudget.pri());
        float qua = belief==null ? taskBudget.qua() :     or(taskBudget.qua() , beliefBudget.qua());
        return new Premise(c.term(), task, term, belief, pri, dur, qua);
    }

    @Nullable
    private static Task answer(@NotNull NAR nar, @NotNull Task question, @NotNull Task answer, @NotNull Concept answerConcept) {

        long taskOcc = question.occurrence();

        //project the belief to the question's time
        if (taskOcc != ETERNAL) {
            answer = answerConcept.merge(question, answer, taskOcc, nar);
        }

        if (answer != null) { //may have become null as a result of projection

            //attempt to Unify any Query variables; answer if unifies
            if (question.term().hasVarQuery()) {
                matchQueryQuestion(nar, question, answer, answerConcept);
            } else if (answerConcept instanceof Compound && Terms.equalAtemporally(question, answerConcept)) {
                matchAnswer(nar, question, answer, answerConcept);
            }



        }

        return answer;
    }

    static void matchAnswer(@NotNull NAR nar, @NotNull Task q, Task a, @NotNull Concept answerConcept) {
        @Nullable Concept questionConcept = nar.concept(q);
        if (questionConcept != null) {
            List<Task> displ = $.newArrayList(0);
            ((QuestionTable)questionConcept.tableFor(q.punc())).answer(a, answerConcept, nar, displ );
            nar.tasks.remove(displ);
        }
    }

    static void matchQueryQuestion(@NotNull NAR nar, @NotNull Task task, @NotNull Task belief, Concept answerConcept) {
        List<Termed> result = $.newArrayList(Param.QUERY_ANSWERS_PER_MATCH);
        new UnifySubst(Op.VAR_QUERY, nar, result, Param.QUERY_ANSWERS_PER_MATCH)
            .unifyAll(
                task.term(), belief.term()
            );

        if (!result.isEmpty()) {
            matchAnswer(nar, task, belief, answerConcept);
        }
    }


    @NotNull
    @Override
    public final Task task() {
        return task;
    }


    @NotNull
    public final Termed<?> beliefTerm() {
        Task x = belief();
        return x == null ? term : x;
    }

    @Nullable
    public final Task belief() {
        return belief;
    }


    @NotNull
    @Override
    public String toString() {
        return task + " | " + term + " | " + (belief!=null ?  belief : "");
    }


//    /** true if both task and (non-null) belief are temporal events */
//    public final boolean isEvent() {
//        /* TODO This part is used commonly, extract into its own precondition */
//        Task b = belief();
//        return (b!=null) && (!task().isEternal()) && (!b.isEternal());
//    }

//    @Nullable public final Concept concept(NAR n) {
//        return n.concept(conceptLink);
//    }

//    @Nullable public final BLink<? extends Termed> termlink(NAR nar) {
//        return termlink(nar.concept(concept));
//    }

    @Nullable public final BLink<? extends Termed> termlink(Concept c) {
        return c.termlinks().get(term);
    }

//    @Nullable public final BLink<? extends Task> tasklink(NAR nar) {
//        return tasklink(nar.concept(concept));
//    }

    @Nullable public final BLink<? extends Task> tasklink(Concept c) {
        return c.tasklinks().get(task);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Premise)) return false;

        Premise premise = (Premise) o;

        if (!task.equals(premise.task)) return false;
        if (!term.equals(premise.term)) return false;
        return belief != null ? belief.equals(premise.belief) : premise.belief == null;

    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + task.hashCode();
        result = 31 * result + term.hashCode();
        result = 31 * result + (belief != null ? belief.hashCode() : 0);
        return result;
    }
}

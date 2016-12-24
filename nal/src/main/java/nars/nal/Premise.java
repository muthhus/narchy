/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.nal;

import nars.$;
import nars.NAR;
import nars.Symbols;
import nars.Task;
import nars.budget.Budget;
import nars.budget.RawBudget;
import nars.budget.util.BudgetFunctions;
import nars.concept.Concept;
import nars.link.BLink;
import nars.table.BeliefTable;
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

import static nars.term.Terms.compoundOrNull;
import static nars.util.UtilityFunctions.or;

/**
 * Defines the conditions used in an instance of a derivation
 * Contains the information necessary for generating derivation Tasks via reasoning rules.
 * <p>
 * It is meant to be disposable and should not be kept referenced longer than necessary
 * to avoid GC loops, so it may need to be weakly referenced.
 */
public final class Premise extends RawBudget implements Tasked {
    private static final Logger logger = LoggerFactory.getLogger(Premise.class);

    @NotNull
    public final Task task;

    @NotNull
    public final Term term;

    @Nullable
    public final Task belief;

    @NotNull
    public final Termed concept;


    ///** not used in creating a Premise key, because the same premise components may be generated from different originating concepts or even other methods of forming them*/
    //@NotNull transient private final Term conceptLink;


    public Premise(@NotNull Termed concept, @NotNull Task taskLink,
                   @NotNull Term termLink,
                   @Nullable Task belief, float p, float q) {

        super(p, q);

        this.concept = concept;

        this.task = taskLink;

        //use the belief's term and not the termlink because it is more specific if:
        // a) it contains temporal information that can be used in temporalization
        // b) a variable in the termlink was matched
        this.term = (this.belief = belief) != null ? belief.term() : termLink;

        //this.conceptLink = conceptLink;


    }

    /**
     * resolves the most relevant belief of a given term/concept
     * <p>
     * patham9 project-eternalize
     * patham9 depending on 4 cases
     * patham9 https://github.com/opennars/opennars2/blob/a143162a559e55c456381a95530d00fee57037c4/src/nal/deriver/projection_eternalization.clj
     * sseehh__ ok ill add that in a bit
     * patham9 you need  project-eternalize-to
     * sseehh__ btw i disabled immediate eternalization entirely
     * patham9 so https://github.com/opennars/opennars2/blob/a143162a559e55c456381a95530d00fee57037c4/src/nal/deriver/projection_eternalization.clj#L31
     * patham9 especially try to understand the "temporal temporal" case
     * patham9 its using the result of higher confidence
     */
    public static Premise tryPremise(@NotNull Concept c, @NotNull final Task task, Term beliefTerm, long now, @NotNull NAR nar) {

        //if (Param.PREMISE_LOG)
        //logger.info("try: { concept:\"{}\",\ttask:\"{}\",\tbeliefTerm:\"{}\" }", c, task, beliefTerm);

//        if (Terms.equalSubTermsInRespectToImageAndProduct(task.term(), term))
//            return null;

        final Budget taskBudget = task.budget().clone();
        if (taskBudget == null)
            return null;

//        Budget termLinkBudget = termLink.clone();
//        if (termLinkBudget == null)
//            return null;


        Task belief = null;


        long when =
                task.occurrence();
        //nar.random.nextBoolean() ?
        // : now;
        //now;
        //(long)(now + dur);

        if (beliefTerm instanceof Compound && task.isQuestOrQuestion()) {

            Compound answerTerm = unify(task.term(), (Compound) beliefTerm, nar);
            if (answerTerm != null) {

                Concept answerConcept = nar.concept(answerTerm);
                if (answerConcept != null) {

                    BeliefTable table = task.isQuest() ? answerConcept.goals() : answerConcept.beliefs();


                    Task answered = table.answer(when, now, task, answerTerm, nar.confMin.floatValue());
                    if (answered != null) {

                        boolean exists = nar.tasks.contains(answered);
                        if (!exists) {
                            //transfer budget from question to answer
                            BudgetFunctions.transferPri(taskBudget, answered.budget(),
                                answered.conf() * (1f - taskBudget.qua()) //proportion of the taskBudget which the answer receives as a boost
                            );

                            boolean processed = nar.input(answered) != null;
                        }

                        //need to call this to handle pre-existing tasks matched to a newer question
                        answered = task.onAnswered(answered, nar);

                        if (answered != null) {
                            if (answered.punc() == Symbols.BELIEF)
                                belief = answered;
                        }
                    }
                }
            }

        }

        if (belief == null) {
            Concept beliefConcept = nar.concept(beliefTerm);
            if (beliefConcept != null) {

                belief = beliefConcept.beliefs().match(when, now, task, true); //in case of quest, proceed with matching belief
            }
        }

//                if (belief != null) {
//                    //try {
//                    Task answered = answer(nar, task, belief, beliefConcept);
//
////                    if (answered != null && !answered.equals(belief)) {
////                        nar.inputLater(answered);
////                    }
//
//                    if (answered != null && task.isQuestion())
//                        belief = answered;
//
//                    if (task.isQuest())
//                        belief = beliefConcept.beliefs().match(task, now); //in case of quest, proceed with matching belief
//
//
//                    /*} catch (InvalidConceptException e) {
//                        logger.warn("{}", e.getMessage());
//                    }*/
//
//                }
//
//
//            } else {
//
//                belief = beliefConcept.beliefs().match(task, now);
//
//            }


        Budget beliefBudget;
        if (belief != null) {
            beliefBudget = belief.budget().clone();
            if (beliefBudget == null)
                belief = null;
        } else {
            beliefBudget = null;
        }

        //TODO lerp by the two budget's qualities instead of aveAri,or etc ?


        float qua = belief == null ? taskBudget.qua() : or(taskBudget.qua(), beliefBudget.qua());
        if (qua < nar.quaMin.floatValue())
            return null;

        float pri =
                belief == null ? taskBudget.pri() : or(taskBudget.pri(), beliefBudget.pri());
        //aveAri(taskLinkBudget.pri(), termLinkBudget.pri());
        //nar.conceptPriority(c);

        return new Premise(c, task, beliefTerm, belief, pri, qua);
    }

    @Nullable
    private static Compound unify(@NotNull Compound q, @NotNull Compound a, NAR nar) {

        if (q.op() != a.op())
            return null; //no chance

        if (Terms.equal(q, a, false, true /* no need to unneg, task content is already non-negated */))
            return q;

        if ((q.vars() == 0) && (q.varPattern() == 0))
            return null; //since they are inequal, if the question has no variables then nothing would unify anyway

        List<Term> result = $.newArrayList(0);
        new UnifySubst(null /* all variables */, nar, result, 1 /*Param.QUERY_ANSWERS_PER_MATCH*/)
                .unifyAll(q, a);

        if (result.isEmpty())
            return null;

        return compoundOrNull(result.get(0));
    }

//    @Nullable
//    private static Task answer(@NotNull NAR nar, @NotNull Task question, @NotNull Task answer, @NotNull Concept answerConcept) {
//
//        long taskOcc = question.occurrence();
//
//        //project the belief to the question's time
//        if (taskOcc != ETERNAL) {
//            answer = answerConcept.merge(question, answer, taskOcc, nar);
//        }
//
//        if (answer != null) { //may have become null as a result of projection
//
//            //attempt to Unify any Query variables; answer if unifies
//            if (question.term().hasVarQuery()) {
//                matchQueryQuestion(nar, question, answer, answerConcept);
//            } else if (answerConcept instanceof Compound && Terms.equalAtemporally(question, answerConcept)) {
//                matchAnswer(nar, question, answer, answerConcept);
//            }
//
//
//        }
//
//        return answer;
//    }

    //static void matchAnswer(@NotNull NAR nar, @NotNull Task q, Task a, @NotNull Concept answerConcept) {
//        @Nullable Concept questionConcept = nar.concept(q);
//        if (questionConcept != null) {
//            List<Task> displ = $.newArrayList(0);
//            ((QuestionTable) questionConcept.tableFor(q.punc())).answer(a, answerConcept, nar, displ);
//            nar.tasks.remove(displ);
//        }
    //}

//    static void matchQueryQuestion(@NotNull NAR nar, @NotNull Task task, @NotNull Task belief, @NotNull Concept answerConcept) {
//        List<Termed> result = $.newArrayList(Param.QUERY_ANSWERS_PER_MATCH);
//        new UnifySubst(Op.VAR_QUERY, nar, result, Param.QUERY_ANSWERS_PER_MATCH)
//                .unifyAll(
//                        task.term(), belief.term()
//                );
//
//        if (!result.isEmpty()) {
//            matchAnswer(nar, task, belief, answerConcept);
//        }
//    }


    @NotNull
    @Override
    public final Task task() {
        return task;
    }


    @NotNull
    public final Term beliefTerm() {
        Task x = belief();
        return x == null ? term : x.term();
    }

    @Nullable
    public final Task belief() {
        return belief;
    }


    @NotNull
    @Override
    public String toString() {
        return task + " | " + term + " | " + (belief != null ? belief : "");
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

    @Nullable
    public final BLink<? extends Termed> termlink(@NotNull Concept c) {
        return c.termlinks().get(term);
    }

//    @Nullable public final BLink<? extends Task> tasklink(NAR nar) {
//        return tasklink(nar.concept(concept));
//    }

    @Nullable
    public final BLink<? extends Task> tasklink(@NotNull Concept c) {
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

package nars.premise;

import jcog.Util;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.attention.Crosslink;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.concept.Concept;
import nars.table.BeliefTable;
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
import static nars.time.Tense.ETERNAL;
import static nars.util.UtilityFunctions.aveAri;


abstract public class PremiseBuilder {

    private static final Logger logger = LoggerFactory.getLogger(PremiseBuilder.class);



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
    @Nullable
    public Premise premise(@NotNull Termed c, @NotNull final Task _task, Term _beliefTerm, long now, NAR nar, float priFactor, float priMin) {

        //if (Param.PREMISE_LOG)
        //logger.info("try: { concept:\"{}\",\ttask:\"{}\",\tbeliefTerm:\"{}\" }", c, task, beliefTerm);

//        if (Terms.equalSubTermsInRespectToImageAndProduct(task.term(), term))
//            return null;


        final Budget taskBudget = _task.budget().clone();
        if (taskBudget == null)
            return null;

        final Task task = nar.post(_task);
        Term beliefTerm = nar.post(_beliefTerm).unneg();
//        Budget termLinkBudget = termLink.clone();
//        if (termLinkBudget == null)
//            return null;


        Task belief = null;



        //temporal focus:
        long when = task.mid();
        if (when == ETERNAL || nar.random.nextBoolean())
            when = now;

        //nar.random.nextBoolean() ?
        // : now;
        //now;
        //(long)(now + dur);

        float dur = nar.time.dur();

        if (beliefTerm instanceof Compound && task.isQuestOrQuestion()) {

            Compound answerTerm = unify(task.term(), (Compound) beliefTerm, nar);
            if (answerTerm != null) {

                beliefTerm = (answerTerm = (Compound) answerTerm.unneg());

                Concept answerConcept = nar.concept(answerTerm);
                if (answerConcept != null) {

                    BeliefTable table = task.isQuest() ? answerConcept.goals() : answerConcept.beliefs();

                    Task answered = table.answer(when, now, dur, task, answerTerm, nar.confMin.floatValue());
                    if (answered != null) {

//                        boolean exists = nar.tasks.contains(answered);
//                        if (!exists) {
//                            boolean processed = nar.input(answered) != null;
//                        }

                        answered = task.onAnswered(answered, nar);
                        if (answered != null) {

                            //transfer budget from question to answer
                            //float qBefore = taskBudget.priSafe(0);
                            //float aBefore = answered.priSafe(0);
                            BudgetFunctions.transferPri(taskBudget, answered.budget(),
                                (float)Math.sqrt(answered.conf())
                                        //(1f - taskBudget.qua())
                                        //(1f - Util.unitize(taskBudget.qua()/answered.qua())) //proportion of the taskBudget which the answer receives as a boost
                            );

                            task.budget().set(taskBudget); //update the task budget

                            Crosslink.crossLink(task, answered, answered.conf(), nar);

                            /*
                            if (qBefore > 0) {
                                float qFactor = taskBudget.priSafe(0) / qBefore;
                                c.tasklinks().mul(task, qFactor); //adjust the tasklink's budget in the same proportion as the task was adjusted
                            }


                            if (aBefore > 0) {
                                float aFactor = answered.priSafe(0) / aBefore;
                                c.termlinks().mul(beliefTerm, aFactor);
                            }
                            */


                            if (answered.punc() == Op.BELIEF)
                                belief = answered;
                        }

                    }
                }
            }

        }

        if (belief == null) {
            Concept beliefConcept = nar.concept(beliefTerm);
            if (beliefConcept != null) {

                belief = beliefConcept.beliefs().match(when, now, dur, task, true); //in case of quest, proceed with matching belief
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


        float tq = taskBudget.qua();
        float bq = (beliefBudget!=null) ? beliefBudget.qua() : Float.NaN;
        float qua = belief == null ? tq : aveAri(tq, bq);
        if (qua < nar.quaMin.floatValue())
            return null;

        float pri =
                belief == null ? taskBudget.pri() : Util.lerp(tq / (tq + bq), taskBudget.pri(), beliefBudget.pri());
        if (pri < priMin)
            return null;

        //aveAri(taskLinkBudget.pri(), termLinkBudget.pri());
        //nar.conceptPriority(c);

        return newPremise(c, task, beliefTerm, belief, pri * priFactor, qua);
    }

    abstract protected Premise newPremise(@NotNull Termed c, @NotNull Task task, Term beliefTerm, Task belief, float pri, float qua);
//    {
//        return new Premise(c, task, beliefTerm, belief, pri, qua);
//    }


    @Nullable
    private static Compound unify(@NotNull Compound q, @NotNull Compound a, NAR nar) {

        if (q.op() != a.op())
            return null; //no chance


        if ((q.vars() > 0)/* || (q.varPattern() != 0)*/) {

            List<Term> result = $.newArrayList(1);
            new UnifySubst(null /* all variables */, nar, result, 1 /*Param.QUERY_ANSWERS_PER_MATCH*/)
                    .unifyAll(q, a);
            if (!result.isEmpty()) {
                Compound unified = compoundOrNull(result.get(0));
                if (unified != null)
                    return unified;
            }
        }

        if (Terms.equal(q, a, false, true /* no need to unneg, task content is already non-negated */))
            return q;
        else
            return null;
    }


//    /**
//     * attempt to revise / match a better premise task
//     */
//    private static Task match(Task task, NAR nar) {
//
//        if (!task.isInput() && task.isBeliefOrGoal()) {
//            Concept c = task.concept(nar);
//
//            long when = task.occurrence();
//
//            if (c != null) {
//                BeliefTable table = (BeliefTable) c.tableFor(task.punc());
//                long now = nar.time();
//                Task revised = table.match(when, now, task, false);
//                if (revised != null) {
//                    if (task.isDeleted() || task.conf() < revised.conf()) {
//                        task = revised;
//                    }
//                }
//
//            }
//
//        }
//
//        if (task.isDeleted())
//            return null;
//
//        return task;
//    }
}
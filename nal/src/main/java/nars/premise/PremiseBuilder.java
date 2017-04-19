package nars.premise;

import jcog.pri.PLink;
import jcog.pri.Priority;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.budget.BudgetFunctions;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.task.DerivedTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.subst.UnifySubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.ETERNAL;
import static nars.util.UtilityFunctions.aveAri;


abstract public class PremiseBuilder {



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
    public Derivation premise(@NotNull Concept concept, PLink<Task> taskLink, PLink<Term> termLink, long now, NAR nar, float priMin, @NotNull Consumer<DerivedTask> target) {

        Term beliefTerm = termLink.get();
        Task belief = null;

        int dur = nar.dur();

        Task task = taskLink.get();
        long when = task.isEternal() ? ETERNAL : task.nearestStartOrEnd(now);

        if (beliefTerm instanceof Compound) {

            Compound unifiedTerm = unify(task.term(), (Compound) beliefTerm, nar);

            if (unifiedTerm != null)  {
                unifiedTerm = compoundOrNull(unifiedTerm.unneg());
                if (unifiedTerm!=null)
                    beliefTerm = unifiedTerm;
            }

            Concept beliefConcept = nar.concept(beliefTerm);

            if ((unifiedTerm != null) &&
                    (beliefConcept instanceof TaskConcept) &&
                        (unifiedTerm.varQuery() == 0) &&
                            !unifiedTerm.equals(task.term())) {

                if (task.isQuestOrQuestion()) {

                    //nar.activate(answerConcept, task.priSafe(0));

                    BeliefTable table = task.isQuest() ? beliefConcept.goals() : beliefConcept.beliefs();

                    Task answered = table.answer(when, now, dur, task, unifiedTerm, nar);
                    if (answered != null) {

                        answered = task.onAnswered(answered, nar);
                        if (answered != null && !answered.isDeleted()) {


                            /*if (nar.input(answered)!=null)*/


                            //transfer budget from question to answer
                            //float qBefore = taskBudget.priSafe(0);
                            //float aBefore = answered.priSafe(0);
                            BudgetFunctions.transferPri(taskLink, answered.priority(),
                                    answered.conf()
                                    //(1f - taskBudget.qua())
                                    //(1f - Util.unitize(taskBudget.qua()/answered.qua())) //proportion of the taskBudget which the answer receives as a boost
                            );

                            //BudgetMerge.maxBlend.apply(theTaskLink, taskLinkCopy, 1f);

                            //task.budget().set(taskBudget); //update the task budget

                            //Crosslink.crossLink(task, answered, answered.conf(), nar);


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


                            if (answered.punc() == Op.BELIEF) {
                                belief = answered;
                                beliefTerm = answered.term();
                            }
                        }

                    }
                }




            }

            //if belief is still not known, match from the belief table
            if ((belief == null) && (beliefTerm.varQuery() == 0)) {
                if (beliefConcept instanceof TaskConcept) {
                    belief = beliefConcept.beliefs().match(when, now, dur, task, (Compound)beliefTerm,true); //in case of quest, proceed with matching belief
                }

            }

//            if (belief!=null  && !(task.isEternal() && belief.isEternal()) && !belief.contains(when)) {
//                //project it to the time
//                belief = ((ImmutableTask)belief).project(when, dur, nar.confMin.floatValue());
//            }

//            //prevent single eternal goal by projecting to now
//            if (belief == null && task.isGoal() && task.isEternal() && !task.contains(when)) {
//                task= ((ImmutableTask)task).project(when, dur, nar.confMin.floatValue());
//            }
        }


        Priority beliefPriority;
        if (belief != null) {
            beliefPriority = belief.priority().clone();
            if (beliefPriority == null)
                belief = null;
        } else {
            beliefPriority = null;
        }

        //TODO lerp by the two budget's qualities instead of aveAri,or etc ?


        //combine either the task or the tasklink. this makes tasks more competitive allowing the priority reduction to be applied to either the task (in belief table) or the tasklink's ordinary forgetting
        float taskPri =
                task.priSafe(-1);
                //taskLinkCopy.pri();
                //Math.max(task.priSafe(0), taskLinkCopy.priSafe(0));
                //taskLinkCopy.pri();
                //aveAri(taskLinkCopy.pri(), task.priSafe(0));

        if (taskPri < 0)
            return null; //task deleted

        float pri = beliefPriority==null ? taskPri :
                //Math.max
                aveAri
                    (taskPri, beliefPriority.priSafe(0));

        if (pri < priMin)
            return null;

        //aveAri(taskLinkBudget.pri(), termLinkBudget.pri());
        //nar.conceptPriority(c);

        nar.concepts.commit(concept);

        return newPremise(concept, task, beliefTerm, belief, pri, target, nar);
    }

    abstract protected Derivation newPremise(@NotNull Termed c, @NotNull Task task, @NotNull Term beliefTerm, @Nullable Task belief, float pri, @NotNull Consumer<DerivedTask> target, @NotNull NAR nar);

    @Nullable
    private static Compound unify(@NotNull Compound q, @NotNull Compound a, NAR nar) {

        if (q.op() != a.op())
            return null; //no chance

        if ((q.vars() > 0)/* || (q.varPattern() != 0)*/) {

            List<Term> result = $.newArrayList(1);
            new UnifySubst(null /* all variables */, nar, (r) -> {
                if (!r.equals(q) && !q.containsTermRecursively(r) /* HACK to prevent answering (#1 && x) with (x && x) === x */) {
                    return result.add(r);
                }
                return false;
            }, 1 /*Param.QUERY_ANSWERS_PER_MATCH*/).unifyAll(q, a);

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

}
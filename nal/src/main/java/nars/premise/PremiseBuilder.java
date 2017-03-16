package nars.premise;

import jcog.Util;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.budget.BLink;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.budget.BudgetMerge;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.task.DerivedTask;
import nars.task.ImmutableTask;
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

    //private static final Logger logger = LoggerFactory.getLogger(PremiseBuilder.class);

    @FunctionalInterface
    interface DerivationBuilder {
        @Nullable Derivation derive(@NotNull Premise p, @NotNull Consumer<DerivedTask> each, @NotNull NAR nar);
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
    @Nullable
    public Derivation premise(@NotNull Termed c, Task task, Budget taskLinkCopy, long when, Term beliefTerm, long now, NAR nar, float priFactor, float priMin, @NotNull Consumer<DerivedTask> target) {



        Task belief = null;

        float dur = nar.time.dur();



        if (beliefTerm instanceof Compound) {

            Compound unifiedTerm = unify(task.term(), (Compound) beliefTerm, nar);

            if ((unifiedTerm != null) && (unifiedTerm.varQuery() == 0)) {

                beliefTerm = (unifiedTerm = (Compound) unifiedTerm.unneg());
            }

            Concept beliefConcept = nar.concept(beliefTerm);

            if (unifiedTerm!=null && beliefConcept instanceof TaskConcept) {

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
                            BudgetFunctions.transferPri(taskLinkCopy, answered.budget(),
                                    (float) Math.sqrt(answered.conf() * answered.qua())
                                    //(1f - taskBudget.qua())
                                    //(1f - Util.unitize(taskBudget.qua()/answered.qua())) //proportion of the taskBudget which the answer receives as a boost
                            );

                            //BudgetMerge.maxBlend.apply(theTaskLink, taskLinkCopy, 1f);

                            //task.budget().set(taskBudget); //update the task budget

                            //Crosslink.crossLink(task, answered, answered.conf(), nar);

                            nar.input(answered);



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
                    belief = beliefConcept.beliefs().match(when, now, dur, task, true); //in case of quest, proceed with matching belief
                }

            }

            if (belief!=null && task.isGoal() && !(task.isEternal() && belief.isEternal()) && !belief.contains(when)) {
                //project it to the time
                belief = ((ImmutableTask)belief).project(when, dur, nar.confMin.floatValue());
            }

            //prevent single eternal goal by projecting to now
            if (belief == null && task.isGoal() && task.isEternal() && !task.contains(when)) {
                task= ((ImmutableTask)task).project(when, dur, nar.confMin.floatValue());
            }
        }


        Budget beliefBudget;
        if (belief != null) {
            beliefBudget = belief.budget().clone();
            if (beliefBudget == null)
                belief = null;
        } else {
            beliefBudget = null;
        }

        //TODO lerp by the two budget's qualities instead of aveAri,or etc ?


        float tq = taskLinkCopy.qua();

        float bq = (beliefBudget != null) ? beliefBudget.qua() : Float.NaN;
        float qua = belief == null ? tq : aveAri(tq, bq);
        if (qua < nar.quaMin.floatValue())
            return null;

        //combine either the task or the tasklink. this makes tasks more competitive allowing the priority reduction to be applied to either the task (in belief table) or the tasklink's ordinary forgetting
        float taskPri = aveAri(taskLinkCopy.pri(), task.priSafe(0));

        float pri =
                belief == null ? taskPri : Util.lerp(tq / (tq + bq), taskPri, beliefBudget.pri());
        if (pri < priMin)
            return null;

        //aveAri(taskLinkBudget.pri(), termLinkBudget.pri());
        //nar.conceptPriority(c);

        return newPremise(c, task, beliefTerm, belief, pri * priFactor, qua, target, nar);
    }

    @Nullable abstract protected Derivation newPremise(@NotNull Termed c, @NotNull Task task, @NotNull Term beliefTerm, @Nullable Task belief, float pri, float qua, @NotNull Consumer<DerivedTask> target, @NotNull NAR nar);
//    {
//        return new Premise(c, task, beliefTerm, belief, pri, qua);
//    }


    @Nullable
    private static Compound unify(@NotNull Compound q, @NotNull Compound a, NAR nar) {

        if (q.op() != a.op())
            return null; //no chance

        if (q.equals(a))
            return q;

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
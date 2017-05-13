package nars.premise;

import jcog.pri.PLink;
import jcog.pri.Priority;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.budget.BudgetFunctions;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.subst.UnifySubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jcog.Util.or;
import static nars.Op.NEG;
import static nars.time.Tense.ETERNAL;


public enum PremiseBuilder { ;


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
    public static Premise premise(@NotNull Concept concept, PLink<Task> taskLink, PLink<Term> termLink, long now, NAR nar, float priMin) {

        Term beliefTerm = termLink.get();
        Task belief = null;

        Task task = taskLink.get();

        if (beliefTerm instanceof Compound) {

            Compound taskTerm = task.term();

            boolean beliefIsTask =
                    beliefTerm.equals(taskTerm);
                    //Terms.equalAtemporally(task.term(), (beliefTerm));

            if (beliefTerm.varQuery() > 0 && !beliefIsTask) {
                assert(beliefTerm.op()!=NEG);
                beliefTerm = unify(taskTerm, (Compound) beliefTerm, nar);
                assert(beliefTerm.op()!=NEG);
            }

            Concept beliefConcept = nar.concept(beliefTerm);

            //QUESTION ANSWERING and TERMLINK -> TEMPORALIZED BELIEF TERM projection
            if (beliefConcept instanceof TaskConcept) { //beliefs/goals will only be in TaskConcepts

                BeliefTable table =
                        ((task.isQuestion() && task.isGoal()) || task.isQuest()) ?
                            beliefConcept.goals() :
                            beliefConcept.beliefs();

                int dur = nar.dur();

                Task match;
                if (task.isQuestOrQuestion()) {
                    long when = task.isEternal() ? ETERNAL : task.nearestStartOrEnd(now);
                    match = table.answer(when, now, dur, task, (Compound) beliefTerm, nar);
                } else {
                    long when = task.start();
                    match = table.match(when, now, dur, task, (Compound) beliefTerm, true);
                }

                if (match != null) {
                    if (match.isBelief() /* not Goal */) {
                        belief = match;
                    }

                    if (task.isQuestOrQuestion() /*&& beliefIsTask*/) {
                        answer(taskLink, match, nar);
                    }
                }
            }

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

        float pri = beliefPriority == null ? taskPri :
                //Math.max
                //aveAri
                or
                    (taskPri, beliefPriority.priSafe(0));

        if (pri < priMin)
            return null;

        //aveAri(taskLinkBudget.pri(), termLinkBudget.pri());
        //nar.conceptPriority(c);



        return new Premise(task, beliefTerm, belief, pri);
    }

    static void answer(PLink<Task> question /* or quest */, @NotNull Task match, NAR nar) {

        @Nullable Task answered = question.get().onAnswered(match, nar);

        if (answered != null && !answered.isDeleted()) {

            //transfer budget from question to answer
            //float qBefore = taskBudget.priSafe(0);
            //float aBefore = answered.priSafe(0);
            BudgetFunctions.transferPri(question, answered.priority(),
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
        }
    }


    /** unify any (and only) query variables
     * present in the 'a' term with any non-query terms in the 'q' term */
    @NotNull private static Compound unify(@NotNull Compound q, @NotNull Compound a, NAR nar) {

        if (q.op() != a.op())
            return a; //fast-fail: no chance

        final Compound[] result = { a };
        new UnifySubst(null, nar, (aa) -> {
            if (aa instanceof Compound) {

                aa = aa.eval(nar.concepts);

                if (!aa.equals(result[0])) {
                    result[0] = ((Compound) aa);
                    return false; //only this match
                }
            }

            return true; //keep trying

        }, Param.BeliefMatchTTL ).unifyAll(a, q);

        return result[0];

//        if (Terms.equal(q, a, false, true /* no need to unneg, task content is already non-negated */))
//            return q;
//        else
//            return null;
    }

}
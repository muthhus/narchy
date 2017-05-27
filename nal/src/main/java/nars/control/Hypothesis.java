package nars.control;

import jcog.pri.PLink;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.budget.BudgetFunctions;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.premise.Premise;
import nars.table.BeliefTable;
import nars.task.BinaryTask;
import nars.task.ITask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.UnifySubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.ETERNAL;
import static nars.util.UtilityFunctions.aveAri;

/** precursor and bulider of premises */
public class Hypothesis extends BinaryTask<PLink<Task>,PLink<Term>> {

    public Hypothesis(@Nullable PLink<Task> tasklink, @Nullable PLink<Term> termlink) {
        super(tasklink, termlink, 0);
    }

    /**
     * resolve the most relevant belief of a given term/concept
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
    @Override
    public ITask[] run(NAR nar) {

        PLink<Task> taskLink = getOne();

        Task task = taskLink.get();
        float taskPri = task.pri();
        if (taskPri != taskPri)
            return null; //task deleted


        Term beliefTerm = getTwo().get();
        Task belief = null;



        if (beliefTerm instanceof Compound) {

            Compound taskTerm = task.term();

            boolean beliefIsTask =
                    beliefTerm.equals(taskTerm);
                    //Terms.equalAtemporally(task.term(), (beliefTerm));

            boolean reUnified = false;
            if (beliefTerm.varQuery() > 0 && !beliefIsTask) {
                Term unified = unify(taskTerm, (Compound) beliefTerm, nar);
                if (unified!=null) {
                    beliefTerm = unified;
                    reUnified = true;
                }
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
                long now = nar.time();
                if (task.isQuestOrQuestion()) {
                    //answer projects to the question time
                    long when = task.isEternal() ? ETERNAL : task.nearestStartOrEnd(now);
                    match = table.answer(when, now, dur, task, (Compound) beliefTerm, (TaskConcept)beliefConcept, nar);
                } else {
                    //should not project the matched belief
                    long when = task.start();
                    match = table.match(when, now, dur, task, (Compound) beliefTerm, true, nar.random());
                }

                if (match != null) {
                    if (match.isBelief() /* not Goal */) {
                        belief = match;
                    }

                    if ((task.isQuestion() && match.isBelief()) || (task.isQuest() && match.isGoal()))  {
                            tryAnswer(reUnified, taskLink, match, nar);
                    }
                }
            }

        }


        float beliefPriority = belief!=null ? belief.pri() : Float.NaN;

        //TODO lerp by the two budget's qualities instead of aveAri,or etc ?


        //combine either the task or the tasklink. this makes tasks more competitive allowing the priority reduction to be applied to either the task (in belief table) or the tasklink's ordinary forgetting
        //taskLinkCopy.pri();
        //Math.max(task.priSafe(0), taskLinkCopy.priSafe(0));
        //taskLinkCopy.pri();
        //aveAri(taskLinkCopy.pri(), task.priSafe(0));


        float priFromTasks = beliefPriority != beliefPriority ? taskPri :
                //Math.max
                aveAri
                //or
                    (taskPri, beliefPriority);


        //aveAri(taskLinkBudget.pri(), termLinkBudget.pri());
        //nar.conceptPriority(c);


        ITask[] r = new ITask[]{new Premise(task, beliefTerm, belief,
                priFromTasks
                //pri() * priFromTasks //<-- this can reduce it too much
        )};

        delete(); //self destruct

        return r;
    }


    static boolean tryAnswer(boolean reUnified, PLink<Task> question /* or quest */, @NotNull Task answer, NAR nar) {
        Compound questionTerm = question.get().term();
        Compound answerTerm = answer.term();
        if (!reUnified && !nar.conceptTerm(answerTerm).equals(nar.conceptTerm(questionTerm))) {
            //see if belief unifies with task (in reverse of previous unify)
            if (questionTerm.varQuery()==0 || (unify(answerTerm, questionTerm, nar)==null)) {
                return false;
            }
        }


        @Nullable Task answered = question.get().onAnswered(answer, nar);

        if (answered != null) {

            //transfer budget from question to answer
            //float qBefore = taskBudget.priSafe(0);
            //float aBefore = answered.priSafe(0);
            BudgetFunctions.fund(question, answered, answered.conf(), false);
                    //(1f - taskBudget.qua())
                    //(1f - Util.unitize(taskBudget.qua()/answered.qua())) //proportion of the taskBudget which the answer receives as a boost

            return true;

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
        return false;
    }


    /** unify any (and only) query variables which may be present in
     * the 'a' term with any non-query terms in the 'q' term
     * returns non-null if unification succeeded and resulted in a transformed 'a' term
     * */
    @Nullable private static Compound unify(@NotNull Compound q, @NotNull Compound a, NAR nar) {

        if (q.op() != a.op())
            return null; //fast-fail: no chance

        final Compound[] result = { null };
        new UnifySubst(Op.VAR_QUERY, nar, (aa) -> {
            if (aa instanceof Compound) {

                aa = aa.eval(nar.terms);

                if (!aa.equals(a)) {
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
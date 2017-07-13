/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.control;

import jcog.pri.PriReference;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.budget.BudgetFunctions;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.control.premise.Derivation;
import nars.table.BeliefTable;
import nars.task.DerivedTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.UnifySubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static nars.time.Tense.ETERNAL;
import static nars.util.UtilityFunctions.aveAri;

/**
 * NOTE: this currently isnt input to the NAR like ITask's are even though it inherits
 * from that superclass. this is temporary until the Premise behavior is determined
 * to be either reified or virtual (executed within a conceptfire execution only)
 *
 * Defines the conditions used in an instance of a derivation
 * Contains the information necessary for generating derivation Tasks via reasoning rules.
 * <p>
 * It is meant to be disposable and should not be kept referenced longer than necessary
 * to avoid GC loops, so it may need to be weakly referenced.
 */
public class Premise  {

    final PriReference<Task> taskLink;
    final PriReference<Term> termLink;

    static final ThreadLocal<Derivation> derivation =
            ThreadLocal.withInitial(Derivation::new);

    transient private final Consumer<DerivedTask> target;

    public Premise(@Nullable PriReference<Task> tasklink, @Nullable PriReference<Term> termlink, Consumer<DerivedTask> target) {
        this.taskLink = tasklink;
        this.termLink = termlink;
        this.target = target;
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
     *
     * returns ttl used, -1 if failed before starting
     */
    public int run(NAR nar, int ttlMax) {

        //nar.emotion.count("Premise_run");

        PriReference<Task> taskLink = this.taskLink;
        Task task = taskLink.get();
        float taskPri = task.priElseZero();

        int dur = nar.dur();
        long now = nar.time();


        Term beliefTerm = termLink.get();
        Task belief = null;
        if (beliefTerm instanceof Compound) {

            Compound taskTerm = task.term();

            boolean beliefIsTask =
                    beliefTerm.equals(taskTerm);
            //Terms.equalAtemporally(task.term(), (beliefTerm));

            boolean reUnified = false;
            if (beliefTerm.varQuery() > 0 && !beliefIsTask) {
                Term unified = unify(taskTerm, (Compound) beliefTerm, nar);
                if (unified != null) {
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


                Task match;

                if (task.isQuestOrQuestion()) {
                    long when = whenAnswer(task, now);
                    match = table.answer(when, now, dur, task, (Compound) beliefTerm, (TaskConcept) beliefConcept, nar);
                    if (match!=null)
                        tryAnswer(reUnified, taskLink, match, nar);
                } else {
                    long when = whenMatch(task, now);
                    match = table.match(when, task, (Compound) beliefTerm, true, nar);
                }

                if (match != null && match.isBelief()) {
                    belief = match;
                }
            }

        }

        if (belief != null && belief.equals(task)) //do not repeat the same task for belief
            belief = null;

        float beliefPriority;
        if (belief != null) {
            beliefPriority = belief.pri();
            if (beliefPriority != beliefPriority) {
                belief = null; //belief was deleted
            } else {
                beliefTerm = belief.term();
            }
        } else {
            beliefPriority = Float.NaN;
        }

        //TODO lerp by the two budget's qualities instead of aveAri,or etc ?

        float parentTaskPri = beliefPriority != beliefPriority ? taskPri :
                //max
                aveAri
                //or
                        (taskPri, beliefPriority);

        Derivation d = derivation.get();

        d.restartA(nar, now, dur);

        d.restartB(this, task, belief, beliefTerm, parentTaskPri,
                //Util.lerp(parentTaskPri, Param.UnificationTTLMin, Param.UnificationTTLMax)
                ttlMax
        );


        nar.deriver().test(d);
        int ttlAfter = d.ttl();

        return ttlMax - ttlAfter;

    }

    /**
     * temporal focus control: determines when a matching belief or answer should be projected to
     */
    static long whenMatch(Task task, long now) {
        if (task.isEternal()) {
            return ETERNAL;
        } else //if (task.isInput()) {
            return task.nearestStartOrEnd(now);
//        } else {
//            if (task.isBelief()) {
//                return now +
//                        nar.dur() *
//                            nar.random().nextInt(2*Param.PREDICTION_HORIZON)-Param.PREDICTION_HORIZON; //predictive belief
//            } else {
//                return Math.max(now, task.start()); //the corresponding belief for a goal or question task
//            }
//        }

        //now;
        //now + dur;
    }

    protected static long whenAnswer(Task task, long now) {
        return task.nearestStartOrEnd(now);
    }


    static boolean tryAnswer(boolean reUnified, PriReference<Task> question /* or quest */, @NotNull Task answer, NAR nar) {
        Task Q = question.get();
        Compound questionTerm = Q.term();
        Compound answerTerm = answer.term();
        if (!reUnified && !nar.conceptTerm(answerTerm).equals(nar.conceptTerm(questionTerm))) {
            //see if belief unifies with task (in reverse of previous unify)
            if (questionTerm.varQuery() == 0 || (unify(answerTerm, questionTerm, nar) == null)) {
                return false;
            }
        }


        @Nullable Task answered = Q.onAnswered(answer, nar);

        if (answered != null) {

            //transfer budget from question to answer
            //float qBefore = taskBudget.priSafe(0);
            //float aBefore = answered.priSafe(0);
            BudgetFunctions.fund(question, answered, (float) Math.sqrt(answered.conf()), false);
            //(1f - taskBudget.qua())
            //(1f - Util.unitize(taskBudget.qua()/answered.qua())) //proportion of the taskBudget which the answer receives as a boost

            if (Q.isInput())
                nar.eventTaskProcess.emit(answer);

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


    /**
     * unify any (and only) query variables which may be present in
     * the 'a' term with any non-query terms in the 'q' term
     * returns non-null if unification succeeded and resulted in a transformed 'a' term
     */
    @Nullable
    private static Compound unify(@NotNull Compound q, @NotNull Compound a, NAR nar) {

        if (q.op() != a.op())
            return null; //fast-fail: no chance

        final Compound[] result = {null};
        new UnifySubst(Op.VAR_QUERY, nar, (aa) -> {
            if (aa instanceof Compound) {

                aa = aa.eval(nar.terms);

                if (!aa.equals(a)) {
                    result[0] = ((Compound) aa);
                    return false; //only this match
                }
            }

            return true; //keep trying

        }, Param.BeliefMatchTTL).unifyAll(a, q);

        return result[0];

//        if (Terms.equal(q, a, false, true /* no need to unneg, task content is already non-negated */))
//            return q;
//        else
//            return null;
    }

    public boolean accept(DerivedTask nt) {
        target.accept(nt);
        //return (priElseZero() > Pri.EPSILON);
        return true;
    }
}

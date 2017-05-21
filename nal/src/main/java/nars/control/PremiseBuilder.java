package nars.control;

import jcog.Util;
import jcog.pri.PLink;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.budget.BudgetFunctions;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.derive.DefaultDeriver;
import nars.premise.Premise;
import nars.table.BeliefTable;
import nars.task.BinaryTask;
import nars.task.ITask;
import nars.task.UnaryTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.UnifySubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.Op.NEG;
import static nars.time.Tense.ETERNAL;
import static nars.util.UtilityFunctions.aveAri;


public class PremiseBuilder extends BinaryTask<PLink<Task>,PLink<Term>> {


    public PremiseBuilder(@Nullable PLink<Task> tasklink, @Nullable PLink<Term> termlink) {
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
                long now = nar.time();
                if (task.isQuestOrQuestion()) {
                    //answer projects to the question time
                    long when = task.isEternal() ? ETERNAL : task.nearestStartOrEnd(now);
                    match = table.answer(when, now, dur, task, (Compound) beliefTerm, (TaskConcept)beliefConcept, nar);
                } else {
                    //should not project the matched belief
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


        float beliefPriority = belief!=null ? belief.pri() : Float.NaN;

        //TODO lerp by the two budget's qualities instead of aveAri,or etc ?


        //combine either the task or the tasklink. this makes tasks more competitive allowing the priority reduction to be applied to either the task (in belief table) or the tasklink's ordinary forgetting
        //taskLinkCopy.pri();
        //Math.max(task.priSafe(0), taskLinkCopy.priSafe(0));
        //taskLinkCopy.pri();
        //aveAri(taskLinkCopy.pri(), task.priSafe(0));


        float pri = beliefPriority != beliefPriority ? taskPri :
                //Math.max
                aveAri
                //or
                    (taskPri, beliefPriority);


        //aveAri(taskLinkBudget.pri(), termLinkBudget.pri());
        //nar.conceptPriority(c);



        return new ITask[] { new DerivePremise(new Premise(task, beliefTerm, belief,
            //pri
        this.pri() * pri
        )) };
    }




    public static class DerivePremise extends UnaryTask<Premise> {

        static final ThreadLocal<BufferedDerivation> derivation =
                ThreadLocal.withInitial(BufferedDerivation::new);

        public DerivePremise(Premise premise) {
            super(premise, premise.pri());
        }

        @Override
        public ITask[] run(NAR n) {

            BufferedDerivation d = derivation.get();

            assert(d.buffer.isEmpty());

            d.restartA(n);
            d.restartB(value.task);
            d.restartC(value, Util.lerp(pri, Param.UnificationTTLMax, Param.UnificationTTLMin));

            DefaultDeriver.the.test(d);

            return d.flush();



//                    assert (start >= ttlRemain);
//
//                    ttl -= (start - ttlRemain);
//                    if (ttl <= 0) break;

//                    int nextDerivedTasks = d.buffer.size();
//                    int numDerived = nextDerivedTasks - derivedTasks;
//                    ttl -= numDerived * derivedTaskCost;
//                    derivedTasks = nextDerivedTasks;


        }
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

                aa = aa.eval(nar.terms);

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
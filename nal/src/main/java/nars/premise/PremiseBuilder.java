package nars.premise;

import jcog.data.MutableIntRange;
import jcog.list.FasterList;
import nars.*;
import nars.attention.Crosslink;
import nars.bag.Bag;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.concept.Concept;
import nars.derive.Deriver;
import nars.link.BLink;
import nars.table.BeliefTable;
import nars.task.DerivedTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.subst.UnifySubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

import static nars.term.Terms.compoundOrNull;
import static nars.util.UtilityFunctions.or;


abstract public class PremiseBuilder {

    private static final Logger logger = LoggerFactory.getLogger(PremiseBuilder.class);


    public int newPremiseMatrix(@NotNull Concept c,
                               @NotNull NAR nar,
                               int tasklinks, MutableIntRange termlinks,
                               @NotNull Consumer<DerivedTask> target,
                               @NotNull Deriver deriver) {

        return newPremiseMatrix(c, tasklinks, termlinks, c.tasklinks(), c.termlinks(), deriver, target, nar);
    }

    public int newPremiseMatrix(@NotNull Concept c, int tasklinks, MutableIntRange termlinks, @NotNull Bag<Task> tasklinkBag, @NotNull Bag<Term> termlinkBag, @NotNull Deriver deriver, @NotNull Consumer<DerivedTask> target, @NotNull NAR nar) {

        c.commit();

        int tasklinksSampled = (int) Math.ceil(tasklinks);

        FasterList<BLink<Task>> tasksBuffer = (FasterList) $.newArrayList(tasklinksSampled);
        tasklinkBag.sample(tasklinksSampled, tasksBuffer::add);

        int tasksBufferSize = tasksBuffer.size();
        if (tasksBufferSize > 0) {
            return newPremiseMatrix(c, termlinks, target, deriver, termlinkBag, tasksBuffer, nar);
        } else {
            if (Param.DEBUG_EXTRA)
                logger.warn("{} has zero tasklinks", c);
            return 0;
        }
    }

    /**
     * derives matrix of: concept => (tasklink x termlink) => premises
     */
    public int newPremiseMatrix(@NotNull Concept c, MutableIntRange termlinks, @NotNull Consumer<DerivedTask> target, @NotNull Deriver deriver, @NotNull Bag<Term> termlinkBag, List<BLink<Task>> taskLinks, @NotNull NAR nar) {

        int count = 0;

        int numTaskLinks = taskLinks.size();
        int termlinksSampled = (int) Math.ceil(termlinks.hi());

        FasterList<BLink<? extends Term>> termsBuffer = (FasterList) $.newArrayList(termlinksSampled);
        termlinkBag.sample(termlinksSampled, termsBuffer::add);

        float priFactor;
//        float busy = (float) nar.emotion.busyMassAvg.getMean() * nar.emotion.learning();
//        if (busy == busy && busy > 1f)
//            priFactor = 1f/busy;
//        else
            priFactor = 1f;



        int termsBufferSize = termsBuffer.size();
        if (termsBufferSize > 0) {

            //current termlink counter, as it cycles through what has been sampled, give it a random starting position
            int jl = nar.random.nextInt(termsBufferSize);

            //random starting position
            int il = nar.random.nextInt(numTaskLinks);

            int countPerTasklink = 0;

            long now = nar.time();

            for (int i = 0; i < numTaskLinks && countPerTasklink < numTaskLinks; i++, il++) {

                BLink<Task> taskLink = taskLinks.get(il % numTaskLinks);

                Task task = taskLink.get(); /*match(taskLink.get(), nar); if (task==null) continue;*/

                int countPerTermlink = 0;

                int termlinksPerForThisTask = termlinks.lerp(taskLink.pri());

                for (int j = 0; j < termsBufferSize && countPerTermlink < termlinksPerForThisTask; j++, jl++) {

                    Premise p = premise(c, task, termsBuffer.get(jl % termsBufferSize).get(), now, nar, priFactor);
                    if (p != null) {
                        deriver.accept(new Derivation(nar, p, target));
                        countPerTermlink++;
                    }

                }

                countPerTasklink += countPerTermlink > 0 ? 1 : 0;

            }

            count += countPerTasklink;

        } else {
            if (Param.DEBUG_EXTRA)
                logger.warn("{} has zero termlinks", c);
        }


        return count;
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
    Premise premise(@NotNull Concept c, @NotNull final Task task, Term beliefTerm, long now, NAR nar, float priFactor) {

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
                            boolean processed = nar.input(answered) != null;
                        }

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

        return newPremise(c, task, beliefTerm, belief, qua, pri * priFactor);
    }

    abstract protected Premise newPremise(@NotNull Concept c, @NotNull Task task, Term beliefTerm, Task belief, float qua, float pri);
//    {
//        return new Premise(c, task, beliefTerm, belief, pri, qua);
//    }


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
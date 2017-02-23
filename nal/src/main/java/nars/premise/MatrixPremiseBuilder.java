package nars.premise;

import jcog.Util;
import jcog.bag.Bag;
import jcog.data.MutableIntRange;
import jcog.list.FasterList;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.budget.BLink;
import nars.concept.Concept;
import nars.derive.Deriver;
import nars.task.DerivedTask;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * constructs premises from a virtual matrix of tasks (row x column)
 */
public class MatrixPremiseBuilder extends PremiseBuilder {

    private static final Logger logger = LoggerFactory.getLogger(MatrixPremiseBuilder.class);

    public final DerivationBuilder derivationBuilder = (p, each, nar)->{
        return new Derivation(nar, p, each,
                Util.lerp(p.qua(), Param.UnificationMatchesMax, 1),
                Param.UnificationStackMax
        );
    };

    @Override
    public @NotNull Premise newPremise(@NotNull Termed c, @NotNull Task task, Term beliefTerm, Task belief, float pri, float qua) {

        //return new DefaultPremise
        return new PreferSimpleAndConfidentPremise(c, task, beliefTerm, belief, pri, qua);

    }


    public int newPremiseMatrix(@NotNull Concept c,
                                @NotNull NAR nar,
                                int tasklinks, MutableIntRange termlinks,
                                @NotNull Consumer<DerivedTask> target,
                                @NotNull Deriver deriver) {

        return newPremiseMatrix(c, tasklinks, termlinks, c.tasklinks(), c.termlinks(), deriver, target, nar);
    }

    public int newPremiseMatrix(@NotNull Concept c, int tasklinks, MutableIntRange termlinks, @NotNull Bag<Task,BLink<Task>> tasklinkBag, @NotNull Bag<Term,BLink<Term>> termlinkBag, @NotNull Deriver deriver, @NotNull Consumer<DerivedTask> target, @NotNull NAR nar) {

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
    public int newPremiseMatrix(@NotNull Concept c, MutableIntRange termlinks, @NotNull Consumer<DerivedTask> target, @NotNull Deriver deriver, @NotNull Bag<Term,BLink<Term>> termlinkBag, List<BLink<Task>> taskLinks, @NotNull NAR nar) {

        int count = 0;

        int numTaskLinks = taskLinks.size();
        int termlinksSampled = (int) Math.ceil(termlinks.hi());

        FasterList<BLink<Term>> termsBuffer = (FasterList) $.newArrayList(termlinksSampled);
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

                int countPerTermlink = 0;

                int termlinksPerForThisTask = termlinks.lerp(taskLink.pri());

                for (int j = 0; j < termsBufferSize && countPerTermlink < termlinksPerForThisTask; j++, jl++) {


                    Premise p = premise(c, taskLink, termsBuffer.get(jl % termsBufferSize).get(), now, nar, priFactor, -1f);
                    if (p != null) {
                        Derivation d = derivationBuilder.derive(p, target, nar);
                        if (d!=null) {
                            deriver.accept(d);
                            countPerTermlink++;
                        }
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

}


//    /**
//     * Evaluate the quality of a belief as a solution to a problem, then reward
//     * the belief and de-prioritize the problem
//     *
//     * @param question  The problem (question or goal) to be solved
//     * @param solution The belief as solution
//     * @param question     The task to be immediately processed, or null for continued
//     *                 process
//     * @return The budget for the new task which is the belief activated, if
//     * necessary
//     */
//    public static Budget solutionBudget(@NotNull Task question, @NotNull Task solution, @NotNull Truth projectedTruth, @NotNull NAR m) {
//        //boolean feedbackToLinks = false;
//        /*if (task == null) {
//            task = nal.getCurrentTask();
//            feedbackToLinks = true;
//        }*/
//
//
//        boolean judgmentTask = question.isBelief();
//        //float om = orderMatch(problem.term(), solution.term(), duration);
//        //if (om == 0) return 0f;
//        float quality = Tense.solutionQuality(question, solution, projectedTruth, m.time());
//        if (quality <= 0)
//            return null;
//
//        Budget budget = null;
//        if (judgmentTask) {
//            question.budget().orPriority(quality);
//        } else {
//            float taskPriority = question.pri();
//
//            budget = new RawBudget(
//                    and(taskPriority, quality),
//                    //UtilityFunctions.or(taskPriority, quality),
//                    BudgetFunctions.truthToQuality(solution.truth()));
//            question.budget().setPriority(Math.min(1 - quality, taskPriority));
//        }
//        /*
//        if (feedbackToLinks) {
//            TaskLink tLink = nal.getCurrentTaskLink();
//            tLink.setPriority(Math.min(1 - quality, tLink.getPriority()));
//            TermLink bLink = nal.getCurrentBeliefLink();
//            bLink.incPriority(quality);
//        }*/
//        return budget;
//    }


//    public static float solutionQuality(Task problem, Task solution, Truth truth, long time) {
//        return Tense.solutionQuality(problem.hasQueryVar(), problem.getOccurrenceTime(), solution, truth, time);
//    }


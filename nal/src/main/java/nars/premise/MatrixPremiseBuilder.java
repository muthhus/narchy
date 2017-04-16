package nars.premise;

import jcog.bag.PLink;
import jcog.bag.Priority;
import jcog.list.FasterList;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.derive.Deriver;
import nars.task.DerivedTask;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

import static nars.time.Tense.ETERNAL;

/**
 * constructs premises from a virtual matrix of tasks (row x column)
 */
public class MatrixPremiseBuilder extends PremiseBuilder {

    private static final Logger logger = LoggerFactory.getLogger(MatrixPremiseBuilder.class);

    public final DerivationBudgeting budgeting;
    public final Deriver deriver;


    public MatrixPremiseBuilder(Deriver deriver, DerivationBudgeting budgeting) {
        this.deriver = deriver;
        this.budgeting = budgeting;
    }


    @Override
    public Derivation newPremise(@NotNull Termed c, @NotNull Task task, @NotNull Term beliefTerm, @Nullable Task belief, float pri, @NotNull Consumer<DerivedTask> each, @NotNull NAR nar) {

        Premise p = new Premise(task, beliefTerm, belief, pri);

        return new Derivation(nar, p, each,
                budgeting,
                Param.UnificationStackMax,
                Param.UnificationTTL
        );
    }


//    public int newPremiseMatrix(@NotNull Concept c, int tasklinks, @NotNull MutableIntRange termlinks, @NotNull Consumer<DerivedTask> target, @NotNull NAR nar) {
//
//        @NotNull Bag<Task, BLink<Task>> tasklinkBag = c.tasklinks();
//        @NotNull Bag<Term, BLink<Term>> termlinkBag = c.termlinks();
//
//
//        tasklinkBag.commit();
//        termlinkBag.commit();
//
//        int tasklinksSampled = (int) Math.ceil(tasklinks);
//
//        FasterList<BLink<Task>> tasksBuffer = (FasterList) $.newArrayList(tasklinksSampled);
//        tasklinkBag.sample(tasklinksSampled, tasksBuffer::add);
//
//        int numTaskLinks = tasksBuffer.size();
//        if (numTaskLinks > 0) {
//
//            int termlinksSampled = (int) Math.ceil(termlinks.hi());
//
//            FasterList<BLink<Term>> termsBuffer = (FasterList) $.newArrayList(termlinksSampled);
//            termlinkBag.sample(termlinksSampled, termsBuffer::add);
//
//            //if (!termsBuffer.isEmpty()) {
//
//            int countPerTasklink = 0;
//
//            //random starting position
//            int il = nar.random.nextInt(numTaskLinks);
//
//            for (int i = 0; i < numTaskLinks && countPerTasklink < numTaskLinks; i++, il++) {
//
//                BLink<Task> taskLink = tasksBuffer.get(il % numTaskLinks);
//
//                return newPremiseVector(c, taskLink, termlinks, target, termsBuffer, nar);
//            }
//        } else {
//            if (Param.DEBUG_EXTRA)
//                logger.warn("{} has zero tasklinks", c);
//        }
//
//        return 0;
//    }





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


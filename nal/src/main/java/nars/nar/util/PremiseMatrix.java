package nars.nar.util;

import jcog.data.MutableIntRange;
import jcog.list.FasterList;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nal.Deriver;
import nars.nal.Premise;
import nars.nal.meta.Derivation;
import nars.task.DerivedTask;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * derives matrix of: concept => (tasklink x termlink) => premises
 */
public class PremiseMatrix {

    private static final Logger logger = LoggerFactory.getLogger(PremiseMatrix.class);


    public static int run(@NotNull Concept c,
                          @NotNull NAR nar,
                          int tasklinks, MutableIntRange termlinks,
                          @NotNull Consumer<DerivedTask> target,
                          @NotNull Deriver deriver) {

        return run(c, tasklinks, termlinks, c.tasklinks(), c.termlinks(), deriver, target, nar);
    }

    public static int run(@NotNull Concept c, int tasklinks, MutableIntRange termlinks, @NotNull Bag<Task> tasklinkBag, @NotNull Bag<Term> termlinkBag, @NotNull Deriver deriver, @NotNull Consumer<DerivedTask> target, @NotNull NAR nar) {

        c.commit();

        int tasklinksSampled = (int) Math.ceil(tasklinks);

        FasterList<BLink<Task>> tasksBuffer = (FasterList) $.newArrayList(tasklinksSampled);
        tasklinkBag.sample(tasklinksSampled, tasksBuffer::add);

        int tasksBufferSize = tasksBuffer.size();
        if (tasksBufferSize > 0) {
            return run(c, termlinks, target, deriver, termlinkBag, tasksBuffer, nar);
        } else {
            if (Param.DEBUG_EXTRA)
                logger.warn("{} has zero tasklinks", c);
            return 0;
        }
    }

    public static int run(@NotNull Concept c, MutableIntRange termlinks, @NotNull Consumer<DerivedTask> target, @NotNull Deriver deriver, @NotNull Bag<Term> termlinkBag, List<BLink<Task>> taskLinks, @NotNull NAR nar) {

        int count = 0;

        int numTaskLinks = taskLinks.size();
        int termlinksSampled = (int) Math.ceil(termlinks.hi());

        FasterList<BLink<? extends Term>> termsBuffer = (FasterList) $.newArrayList(termlinksSampled);
        termlinkBag.sample(termlinksSampled, termsBuffer::add);


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

                    Premise p = Premise.tryPremise(c, task, termsBuffer.get(jl % termsBufferSize).get(), now, nar);
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




        /*
        catch (RuntimeException e) {

            //if (Param.DEBUG)
                e.printStackTrace();

            logger.error("run {}", e);
        }
         */

        return count;
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
package nars.nar.util;

import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.budget.Budget;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nal.Conclusion;
import nars.nal.Deriver;
import nars.nal.Premise;
import nars.nal.meta.PremiseEval;
import nars.term.Term;
import nars.util.data.list.FasterList;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * derives matrix of: concept => (tasklink x termlink) => premises
 */
public class FireConceptSquared extends Conclusion {

    private static final Logger logger = LoggerFactory.getLogger(FireConceptSquared.class);

    final static Deriver deriver = Deriver.getDefaultDeriver();
    public final int premisesFired;


    public FireConceptSquared(@NotNull Concept c, @NotNull NAR nar, int tasklinks, int termlinks, @NotNull Consumer<Task> batch) {
        super(batch);

        int count = 0;

        try {

            c.commit();

            int tasklinksSampled = (int)Math.ceil(tasklinks * Param.BAG_OVERSAMPLING);

            FasterList<BLink<Task>> tasksBuffer = $.newArrayList(tasklinksSampled);
            c.tasklinks().sample(tasklinksSampled, tasksBuffer::addIfNotNull);

            int tasksBufferSize = tasksBuffer.size();
            if (tasksBufferSize > 0) {

                int termlinksSampled = (int)Math.ceil(termlinks * Param.BAG_OVERSAMPLING);

                FasterList<BLink<Term>> termsBuffer = $.newArrayList(termlinksSampled);
                c.termlinks().sample(termlinksSampled, termsBuffer::addIfNotNull);


                int termsBufferSize = termsBuffer.size();
                if (termsBufferSize > 0) {

                    //current termlink counter, as it cycles through what has been sampled, give it a random starting position
                    int jl = nar.random.nextInt(termsBufferSize);

                    //random starting position
                    int il = nar.random.nextInt(tasksBufferSize);

                    int countPerTasklink = 0;

                    for (int i = 0; i < tasksBufferSize && countPerTasklink < tasklinks; i++, il++) {

                        BLink<Task> taskLink = tasksBuffer.get( il % tasksBufferSize );

                        Task task = taskLink.get();
                        if (task == null || task.isDeleted())
                            continue;

                        Budget taskLinkBudget = taskLink.clone(); //save a copy because in multithread, the original may be deleted in-between the sample result and now
                        if (taskLinkBudget == null)
                            continue;

                        long now = nar.time();

                        int countPerTermlink = 0;

                        for (int j = 0; j < termsBufferSize && countPerTermlink < termlinks; j++, jl++) {

                            Premise p = Premise.build(nar, c, now, task, taskLinkBudget, termsBuffer.get( jl % termsBufferSize ));

                            if (p != null) {

                                new PremiseEval(nar, deriver, p, this);
                                countPerTermlink++;
                            }

                        }

                        countPerTasklink+= countPerTermlink > 0 ? 1 : 0;

                    }

                    count+=countPerTasklink;

                } else {
                    if (Param.DEBUG_EXTRA)
                        logger.warn(c + " has zero termlinks");
                }

            } else {
                if (Param.DEBUG_EXTRA)
                    logger.warn(c + " has zero tasklinks");
            }


        } catch (RuntimeException e) {

            if (Param.DEBUG)
                e.printStackTrace();

            logger.error("run {}", e.toString());
        }

        this.premisesFired = count;
    }
}
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
import nars.nal.derive.TrieDeriver;
import nars.nal.meta.PremiseEval;
import nars.term.Term;
import nars.util.data.list.FasterList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * creates a matrix of termlink x tasklink premises and derives them
 */
public class FireConceptSquared extends Conclusion {

    private static final Logger logger = LoggerFactory.getLogger(FireConceptSquared.class);

    final static Deriver deriver = Deriver.getDefaultDeriver();



    public FireConceptSquared(Concept c, NAR nar, int tasklinks, int termlinks, Consumer<Task> batch) {
        super(batch);

        try {

            c.commit();

            int count = 0;


            int tasklinksSampled = (int)Math.ceil(tasklinks * Param.OVERSAMPLE_BAGS);

            FasterList<BLink<Task>> tasksBuffer = $.newArrayList(tasklinksSampled);
            c.tasklinks().sample(tasklinksSampled, tasksBuffer::addIfNotNull);

            if (!tasksBuffer.isEmpty()) {

                int termlinksSampled = (int)Math.ceil(termlinks * Param.OVERSAMPLE_BAGS);

                FasterList<BLink<Term>> termLinks = $.newArrayList(termlinksSampled);
                c.termlinks().sample(termlinksSampled, termLinks::addIfNotNull);

                if (!termLinks.isEmpty()) {

                    int countPerTasklink = 0;

                    for (int i = 0, tasksBufferSize = tasksBuffer.size(); i < tasksBufferSize && countPerTasklink < tasklinks; i++) {

                        BLink<Task> taskLink = tasksBuffer.get(i);

                        Task task = taskLink.get();
                        if (task == null || task.isDeleted())
                            continue;

                        Budget taskLinkBudget = taskLink.clone(); //save a copy because in multithread, the original may be deleted in-between the sample result and now
                        if (taskLinkBudget.isDeleted())
                            continue;

                        long now = nar.time();

                        int countPerTermlink = 0;

                        for (int j = 0, termsArraySize = termLinks.size(); j < termsArraySize && countPerTermlink < termlinks; j++) {

                            Premise p = Premise.build(nar, c, now, task, taskLinkBudget, termLinks.get(j));
                            if (p != null) {

                                new PremiseEval(nar, deriver, p, this);
                                countPerTermlink++;
                            }

                        }

                        countPerTasklink+= countPerTermlink;

                    }

                    count+=countPerTasklink;

                } else {
                    logger.warn(c + " has zero termlinks");
                }

            } else {
                logger.warn(c + " has zero tasklinks");
            }


        } catch (RuntimeException e) {

            if (Param.DEBUG)
                e.printStackTrace();

            logger.error("run {}", e.toString());
        }

    }
}
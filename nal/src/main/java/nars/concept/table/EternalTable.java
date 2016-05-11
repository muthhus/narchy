package nars.concept.table;

import nars.Global;
import nars.NAR;
import nars.bag.impl.SortedArrayTable;
import nars.budget.Budget;
import nars.nal.LocalRules;
import nars.task.Revision;
import nars.task.RevisionTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Terms;
import nars.truth.Truth;
import org.happy.collections.lists.decorators.SortedList_1x4;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Created by me on 5/7/16.
 */
public class EternalTable extends SortedArrayTable<Task,Task> {

    public EternalTable(Map<Task, Task> index) {
        super(Global.newArrayList(1), index, SortedList_1x4.SearchType.BinarySearch);
        setCapacity(1);
    }

    @Override
    protected final void removeWeakest(Object reason) {
        Task x = remove(weakest());
        if (!x.isDeleted())
            x.delete(reason);
    }

    @Nullable
    @Override
    public final Task key(Task task) {
        return task;
    }

    @Override
    public final int compare(@NotNull Task o1, @NotNull Task o2) {
        float f1 = BeliefTable.rankEternalByOriginality(o2); //reversed
        float f2 = BeliefTable.rankEternalByOriginality(o1);
        if (f1 < f2)
            return -1;
        if (f1 > f2)
            return 1;
        return 0;
    }

    @Nullable
    public /*Revision*/Task tryRevision(@NotNull Task newBelief, @NotNull NAR nar) {
        List<Task> beliefs = list();
        int bsize = beliefs.size();
        if (bsize == 0)
            return null; //nothing to revise with

        Compound newBeliefTerm = newBelief.term();

        final long newTime = newBelief.occurrence(); //nar.time();

        //Try to select a best revision partner from existing beliefs:
        Task oldBelief = null;
        float bestRank = 0, bestConf = 0;
        Truth conclusion = null;
        final float newBeliefConf = newBelief.conf();
        Truth newBeliefTruth = newBelief.truth();

        for (int i = 0; i < bsize; i++) {
            Task x = beliefs.get(i);

            if (!LocalRules.isRevisible(newBelief, x))
                continue;

            float matchFactor = Terms.termRelevance(newBeliefTerm, x.term());
            if (matchFactor <= 0)
                continue;

//
//            float factor = tRel * freqMatch;
//            if (factor < best) {
//                //even with conf=1.0 it wouldnt be enough to exceed existing best match
//                continue;
//            }

            //            float minValidConf = Math.min(newBeliefConf, x.conf());
//            if (minValidConf < bestConf) continue;
//            float minValidRank = BeliefTable.rankEternalByOriginality(minValidConf, totalEvidence);
//            if (minValidRank < bestRank) continue;

            Truth oldBeliefTruth = x.truth();

            Truth c = newBelief.isEternal() ?
                    Revision.revision(newBeliefTruth, oldBeliefTruth, matchFactor, bestConf) :
                    Revision.revision(newBelief, x, newTime, matchFactor, bestConf);

            if (c == null)
                continue;

            //avoid a duplicate truth at the same time
            if (newTime == x.occurrence() && c.equals(oldBeliefTruth))
                continue;
            if (newTime == newBelief.occurrence() && c.equals(newBeliefTruth))
                continue;

            float cconf = c.conf();
            final int totalEvidence = 1; //newBelief.evidence().length + x.evidence().length; //newBelief.evidence().length + x.evidence().length;
            float rank = BeliefTable.rankEternalByOriginality(cconf, totalEvidence);

            if (rank > bestRank) {
                bestRank = rank;
                bestConf = cconf;
                oldBelief = x;
                conclusion = c;
            }
        }

        if (oldBelief != null) {
            Budget revisionBudget = Revision.budgetRevision(conclusion, newBelief, oldBelief, nar);
            if (revisionBudget != null) {
                return new RevisionTask(
                        LocalRules.intermpolate(
                                newBelief, oldBelief,
                                newBeliefConf, oldBelief.conf()),
                        revisionBudget,
                        newBelief, oldBelief,
                        conclusion,
                        nar.time(), newTime);
            }
        }
        return null;
    }

}

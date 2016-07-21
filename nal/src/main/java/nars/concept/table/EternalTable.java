package nars.concept.table;

import nars.NAR;
import nars.task.Revision;
import nars.task.RevisionTask;
import nars.task.Task;
import nars.truth.Truth;
import nars.util.data.sorted.SortedArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

import static nars.concept.table.BeliefTable.rankEternalByConfAndOriginality;
import static nars.nal.Tense.ETERNAL;

/**
 * Created by me on 5/7/16.
 */
public class EternalTable extends SortedArray<Task> implements TaskTable, Comparator<Task> {

    int capacity;

    public EternalTable(int initialCapacity) {
        super(Task[]::new);
        capacity(initialCapacity);
    }

    public void capacity(int c) {
        if (this.capacity != c) {

            this.capacity = c;

            int s = size();

            //TODO can be accelerated by batch remove operation
            while (c < s--) {
                removeWeakest();
            }
        }
    }



//    protected final Task removeWeakest(Object reason) {
//        if (!isEmpty()) {
//            Task x = remove(size() - 1);
//            x.delete(reason);
//        }
//    }

    public final Task strongest() {
        if (isEmpty()) return null;
        return list[0];
    }

    public final Task weakest() {
        if (isEmpty()) return null;
        return list[size()-1];
    }

    public float minRank() {
        Task w = weakest();
        if ( w== null) return 0;
        return rankEternalByConfAndOriginality(w);
    }

    @Override
    public final int compare(@NotNull Task o1, @NotNull Task o2) {
        float f1 = rankEternalByConfAndOriginality(o2); //reversed
        float f2 = rankEternalByConfAndOriginality(o1);
        if (f1 < f2)
            return -1;
        if (f1 > f2)
            return 1;
        return 0;
    }

    @Nullable
    public /*Revision*/Task tryRevision(@NotNull Task newBelief, @NotNull NAR nar) {

        int bsize = size();
        if (bsize == 0)
            return null; //nothing to revise with

        //Try to select a best revision partner from existing beliefs:
        Task oldBelief = null;
        float bestRank = 0f, bestConf = 0f;
        Truth conclusion = null;
        final float newBeliefConf = newBelief.conf();
        Truth newBeliefTruth = newBelief.truth();

        for (int i = 0; i < bsize; i++) {
            Task x = list[i];

            if (!Revision.isRevisible(newBelief, x))
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

            Truth c = Revision.revisionEternal(newBeliefTruth, oldBeliefTruth, 1f, bestConf);
            if (c == null)
                continue;

            //avoid a duplicate truth
            if (c.equals(oldBeliefTruth) || c.equals(newBeliefTruth))
                continue;

            float cconf = c.conf();
            final int totalEvidence = 1; //newBelief.evidence().length + x.evidence().length; //newBelief.evidence().length + x.evidence().length;
            float rank = rank(cconf, totalEvidence);

            if (rank > bestRank) {
                bestRank = rank;
                bestConf = cconf;
                oldBelief = x;
                conclusion = c;
            }
        }

        return oldBelief != null ?
                new RevisionTask(
                    Revision.intermpolate(
                        newBelief, oldBelief,
                        newBeliefConf, oldBelief.conf()
                    ),
                    newBelief, oldBelief,
                    conclusion,
                    nar.time(), ETERNAL).
                        budget(oldBelief, newBelief).
                        log("Insertion Revision")
                :
                null;
    }

    public final Task put(final Task t) {
        Task displaced = null;

        if (size() == capacity()) {
            Task l = weakest();
            if (l.conf() <= t.conf()) {
                displaced = removeWeakest();
            }
        }

        add(t, this);

        return displaced;
    }

    public final Truth truth() {
        Task s = strongest();
        return s!=null ? s.truth() : null;
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public void remove(@NotNull Task belief, List<Task> displ) {
        /*Task removed = */remove(indexOf(belief, this));
        TaskTable.removeTask(belief, null, displ);
    }

    public final float rank(float eConf, int length) {
        return rankEternalByConfAndOriginality(eConf, length);
    }

    public boolean isFull() {
        return size() == capacity();
    }
}

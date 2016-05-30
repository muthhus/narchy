package nars.concept.table;

import nars.NAR;
import nars.bag.impl.SortedTable;
import nars.nal.Tense;
import nars.task.Revision;
import nars.task.Task;
import nars.task.TruthPolation;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static nars.concept.table.BeliefTable.rankTemporalByConfidenceAndOriginality;
import static nars.nal.Tense.ETERNAL;

/** stores the items unsorted; revection manages their ranking and removal */
public class MicrosphereTemporalBeliefTable extends DefaultListTable<Task,Task> implements TemporalBeliefTable {

    private final SortedTable<Task, Task> eternal;
    /** history factor:
     *      higher means it is easier to hold beliefs further away from current time at the expense of accuracy
     *      lower means more accuracy at the expense of shorter memory span
     */
    private final float historyFactor = 1.5f;
    long min, max;
    private TruthPolation polation;
    private long lastUpdate = Tense.TIMELESS;

    public MicrosphereTemporalBeliefTable(Map<Task, Task> mp, SortedTable<Task,Task> eternal) {
        super(mp);
        setCapacity(1);
        this.eternal = eternal;
    }

    public static float rank(@NotNull Task t, long when, float ageFactor) {
        return rankTemporalByConfidenceAndOriginality(t, when, when, ageFactor, -1);
    }

    @Nullable
    @Override
    public Task ready(@NotNull Task input, @NotNull NAR nar) {
        int cap = capacity();

        this.lastUpdate = nar.time();
        if (cap == 0)
            return null;

        int s1 = size();
        if (s1 >= cap) {

            if (removeAlreadyDeleted() >= cap) {

                //the result of compression is processed separately
                Task merged = compress(input, nar.time());
                if (merged == null) {
                    //not compressible with respect to this input, so reject the input
                    return null;
                } else if (merged!=input) {
                    // else: the result of compression has freed a space for the incoming input
                    nar.process(merged);

                }
                else {
                    //only one space has been freed for the input, no merging resulted

                }
            }

        }

        if (isFull()) {
            //WHY DOES THIS HAPPEN, IS IT DANGEROUS
            return null;
        }


        return input;
    }

    protected float updateRange() {
        if (min == ETERNAL) {
            //invalidated, recalc:
            long tmin = Long.MAX_VALUE, tmax = Long.MIN_VALUE;
            int s = size();
            for (int i = 0; i < s; i++) {
                Task t = get(i);
                long o = t.occurrence();
                if (o < tmin) tmin = o;
                if (o > tmax) tmax = o;
            }

            this.min = tmin;
            this.max = tmax;
        }

        return ageFactor();
    }

    @Nullable
    @Override
    protected Task addItem(@NotNull Task i) {
        super.addItem(i);
        long occ = i.occurrence();
        if ((occ < min) || (occ > max)) {
            invalidateRange();
        }
        return null;
    }


    @Override
    protected Task removeItem(@NotNull Task removed) {
        if (super.removeItem(removed) == removed) {
            long occ = removed.occurrence();
            if ((occ == min) || (occ == max)) {
                invalidateRange();
            }
            return removed;
        }
        return null;
    }



    private void invalidateRange() {
        min = max = ETERNAL;
    }

    @Override public Task weakest() {
        if (lastUpdate == Tense.TIMELESS)
            throw new RuntimeException("unable to measure weakest without knowing current time");
        return weakest(lastUpdate, null, Float.POSITIVE_INFINITY);
    }

    @Override
    protected final void removeWeakest(@Nullable Object reason) {
        remove(weakest()).delete(reason);
    }

    @Nullable
    public Task weakest(long now, @Nullable Task excluding, float minRank) {
        Task weakest = null;
        float weakestRank = minRank;
        int n = size();

        float ageFactor = ageFactor();
        long[] excludingEvidence = excluding!=null ? excluding.evidence() : null;
        for (int i = 0; i < n; i++) {

            Task ii = get(i);
            if (excluding!=null &&
                    ((ii == excluding) || (Stamp.overlapping(excludingEvidence, ii.evidence()))))
                continue;

            //consider ii for being the weakest ranked task to remove
            float r = rank(ii, now, ageFactor);
            if (r < weakestRank) {
                weakestRank = r;
                weakest = ii;
            }

        }

        return weakest;
    }

    public float rank(@NotNull Task t, long when) {
        return rank(t, when, ageFactor());

    }

    public float ageFactor() {
        //return 1f;
        long range = max - min;
        return (range == 0) ? 1 :
                ((1f) / (range * historyFactor));
    }


    /** frees one slot by removing 2 and projecting a new belief to their midpoint. returns the merged task */
    @Nullable
    protected Task compress(@NotNull Task input, long now) {

        updateRange();

        float inputRank = rank(input, now);

        Task a = weakest(now, null, inputRank);
        if (a == null)
            return null;

        Task b = weakest(now, a, Float.POSITIVE_INFINITY);
        if (a == b)
            throw new RuntimeException();

        Task merged;
        if (b!=null) {
            merged = merge(a, b, now);

            remove(b);
            TaskTable.removeTask(b, "Revection Revision");
        } else {
            merged = null;
        }

        remove(a);
        TaskTable.removeTask(a, (b == null) ? "Revection Forget" : "Revection Revision");


        return merged!=null ? merged : input;
    }

    /** t is the target time of the new merged task */
    public Task merge(Task a, Task b, long now) {
        float ac = a.confWeight();
        float bc = b.confWeight();
        int mid = Math.round((a.occurrence() * ac + b.occurrence() * bc) / (ac + bc));
        Truth truth = truth(mid);
        if (truth == null)
            return null;
        return Revision.merge(a, b, now, mid, truth);
    }


    @Override
    public final Task key(Task task) {
        return task;
    }

    @Nullable
    @Override public Task top(long when) {

        //removeDeleted();


        //find the best balance of temporal proximity and confidence
        int ls = size();
        if (ls == 1)
            return get(0); //the only task

        Task best = null;

        float ageFactor = updateRange();

        float bestRank = -1;

        for (int i = 0; i < ls; i++) {
            Task x = get(i);
            float r = rank(x, when, ageFactor);
            if (r > bestRank) {
                best = x;
                bestRank = r;
            }
        }

        return best;

    }

    @Nullable
    @Override
    public Truth truth(long when) {
        int c = capacity();
        if (polation == null || polation.capacity() < c) {
            float ecap = eternal.capacity();
            polation = new TruthPolation(c, ecap / (ecap + c));
        }

        //removeDeleted();

        return polation.truth(when, list, eternal.top());
    }




    private final int removeAlreadyDeleted() {
        int s = size();
        for (int i = 0; i < s; ) {
            Task x = get(i);
            if (x.isDeleted()) {
                removeItem(i);
                s--;
            } else {
                i++;
            }
        }
        return s;
    }

//    public Task weakest(Task input, NAR nar) {
//
//        //if (polation == null) {
//            //force update for current time
//
//        polation.credit.clear();
//        Truth current = truth(nar.time());
//        //}
//
////        if (polation.credit.isEmpty())
////            throw new RuntimeException("empty credit table");
//
//        List<Task> list = list();
//        float min = Float.POSITIVE_INFINITY;
//        Task minT = null;
//        for (int i = 0, listSize = list.size(); i < listSize; i++) {
//            Task t = list.get(i);
//            float x = polation.value(t, -1);
//            if (x >= 0 && x < min) {
//                min = x;
//                minT = t;
//            }
//        }
//
//        System.out.println("removing " + min + "\n\t" + polation.credit);
//
//        return minT;
//    }


    //    public @Nullable Truth topTemporalCurrent(long when, long now, @Nullable Task topEternal) {
//        //find the temporal with the best rank
//        Task t = topTemporal(when, now);
//        if (t == null) {
//            return (topEternal != null) ? topEternal.truth() : Truth.Null;
//        } else {
//            Truth tt = t.truth();
//            return (topEternal() != null) ? tt.interpolate(topEternal.truth()) : tt;
//
//            //return t.truth();
//        }
//    }


//    //NEEDS DEBUGGED
//    @Nullable public Truth topTemporalWeighted(long when, long now, @Nullable Task topEternal) {
//
//        float sumFreq = 0, sumConf = 0;
//        float nF = 0, nC = 0;
//
//        if (topEternal!=null) {
//            //include with strength of 1
//
//            float ec = topEternal.conf();
//
//            sumFreq += topEternal.freq() * ec;
//            sumConf += ec;
//            nF+= ec;
//            nC+= ec;
//        }
//
//        List<Task> temp = list();
//        int numTemporal = temp.size();
//
//        if (numTemporal == 1) //optimization: just return the only temporal truth value if it's the only one
//            return temp.get(0).truth();
//
//
////        long maxtime = Long.MIN_VALUE;
////        long mintime = Long.MAX_VALUE;
////        for (int i = 0, listSize = numTemporal; i < listSize; i++) {
////            long t = temp.get(i).occurrence();
////            if (t > maxtime)
////                maxtime = t;
////            if (t < mintime)
////                mintime = t;
////        }
////        float dur = 1f/(1f + (maxtime - mintime));
//
//
//        long mdt = Long.MAX_VALUE;
//        for (int i = 0; i < numTemporal; i++) {
//            long t = temp.get(i).occurrence();
//            mdt = Math.min(mdt, Math.abs(now - t));
//        }
//        float window = 1f / (1f + mdt/2);
//
//
//        for (int i = 0, listSize = numTemporal; i < listSize; i++) {
//            Task x = temp.get(i);
//
//            float tc = x.conf();
//
//            float w = TruthFunctions.temporalIntersection(
//                    when, x.occurrence(), now, window);
//
//            //strength decreases with distance in time
//            float strength =  w * tc;
//
//            sumConf += tc * w;
//            nC+=tc;
//
//            sumFreq += x.freq() * strength;
//            nF+=strength;
//        }
//
//        return nC == 0 ? Truth.Null :
//                new DefaultTruth(sumFreq / nF, (sumConf/nC));
//    }

}

package nars.concept.table;

import nars.Global;
import nars.NAR;
import nars.bag.impl.SortedTable;
import nars.budget.BudgetMerge;
import nars.nal.Tense;
import nars.task.*;
import nars.truth.Stamp;
import nars.truth.Truth;

import java.util.List;
import java.util.Map;

import static nars.concept.table.BeliefTable.rankTemporalByConfidenceAndOriginality;
import static nars.nal.Tense.DTERNAL;
import static nars.nal.Tense.ETERNAL;

/** stores the items unsorted; revection manages their ranking and removal */
public class MicrosphereRevectionTemporalBeliefTable extends ArrayListTable<Task,Task> implements TemporalBeliefTable {

    private TruthPolation polation;
    private final SortedTable<Task, Task> eternal;

    long min = 0, max = 0;

    public MicrosphereRevectionTemporalBeliefTable(Map<Task, Task> mp, int cap, SortedTable<Task,Task> eternal) {
        super(mp, Global.newArrayList(cap));
        setCapacity(cap);
        this.eternal = eternal;
    }

    @Override
    public void setCapacity(int c) {
        if (c!=0 && (c < 3))
            throw new RuntimeException("temporal capacity must be > 2");
        super.setCapacity(c);
    }

    @Override
    public Task prepare(Task input, NAR nar) {
        if (capacity() == 0)
            return null;

        if (isFull() /*&& temporal.capacity() > 1*/) {

            if (null == compress(input, nar.time(), nar)) {
                return null; //reject input because it didnt rank
            }

            /*if (!Revection.revect(input, this, nar)) {
                return null; //rejected input
            }*/

//            Task w = weakest(input, nar);
//            if (w == null)
//                throw new RuntimeException("no weakest task determined");
//            remove(w);
//            nar.remove(w, "Temporal Forget");

        }


        //proceed with this task now that there is room
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

    @Override
    protected Task addItem(Task i) {
        long occ = i.occurrence();
        if ((occ < min) || (occ > max)) {
            invalidateRange();
        }
        return super.addItem(i);
    }

    @Override
    protected Task removeItem(Task removed) {
        long occ = removed.occurrence();
        if ((occ == min) || (occ == max)) {
            invalidateRange();
        }
        return super.removeItem(removed);
    }

    private void invalidateRange() {
        min = max = ETERNAL;
    }

    public Task weakest(long now, Task excluding, float minRank) {
        Task weakest = null;
        float weakestRank = minRank;
        int n = size();

        float ageFactor = ageFactor();
        for (int i = 0; i < n; i++) {

            Task ii = get(i);
            if (ii == excluding)
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

    public float rank(Task t, long when) {
        return rank(t, when, ageFactor());

    }
    public float rank(Task t, long when, float ageFactor) {
        return rankTemporalByConfidenceAndOriginality(t, when, when, ageFactor, -1);
    }

    public float ageFactor() {
        return 1f / (max - min);
    }


    /** frees one slot by removing 2 and projecting a new belief to their midpoint */
    protected Task compress(Task input, long now, NAR nar) {

        updateRange();

        float inputRank = rank(input, now);

        Task a = weakest(now, null, inputRank);
        if (a == null)
            return null;

        Task b = weakest(now, a, Float.POSITIVE_INFINITY);

        //TODO proper iterpolate: truth, time, dt
        float ac = a.conf();
        float bc = b.conf();
        long newOcc = Math.round((a.occurrence() * ac + b.occurrence() * bc) / (ac + bc));

        Truth newTruth = truth(newOcc);

        long[] newEv = Stamp.zip(a, b); //TODO impl a weighted zip

        //TODO interpolate the dt()
        int newDT;
        if (b.term().dt()!=DTERNAL)
            newDT = b.term().dt();
        else if (a.term().dt()!=DTERNAL)
            newDT = a.term().dt();
        else
            newDT = DTERNAL;

        Task merged = new MutableTask(a.term().dt(newDT), a, b, now, newOcc, newEv, newTruth, BudgetMerge.avgDQBlend)
                .log("Revection Merge");

        remove(a);
        TaskTable.removeTask(a, "Revection Forget", nar);
        remove(b);
        TaskTable.removeTask(b, "Revection Forget", nar);

        nar.process(merged);
        return merged;
    }



    public final Task key(Task task) {
        return task;
    }

    @Override public Task top(long when, long now) {

        List<? extends Task> l = list();

        //find the best balance of temporal proximity and confidence
        int ls = l.size();
        if (ls == 1)
            return l.get(0); //the only task

        Task best = null;

        float ageFactor = updateRange();

        float bestRank = -1;

        for (int i = 0; i < ls; i++) {
            Task x = l.get(i);
            float r = rank(x, when, ageFactor);
            if (r > bestRank) {
                best = x;
                bestRank = r;
            }
        }

        return best;

    }

    @Override
    public Truth truth(long when) {
        if (polation == null || polation.capacity() < capacity()) {
            int ecap = eternal.capacity();
            polation = new TruthPolation(capacity(), ecap / (ecap + capacity()));
        }

        return polation.truth(when, list(), eternal.top());
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

package nars.concept.table;

import com.gs.collections.impl.list.mutable.primitive.IntArrayList;
import nars.NAR;
import nars.Param;
import nars.nal.Stamp;
import nars.nal.Tense;
import nars.task.Revision;
import nars.task.Task;
import nars.task.TruthPolation;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import nars.util.Util;
import nars.util.data.list.FasterList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

import static nars.concept.table.BeliefTable.rankTemporalByConfidenceAndOriginality;
import static nars.nal.Tense.ETERNAL;

/**
 * stores the items unsorted; revection manages their ranking and removal
 */
public class MicrosphereTemporalBeliefTable extends FasterList<Task> implements TemporalBeliefTable {

    static final int MAX_TRUTHPOLATION_SIZE = 64;
    static final ThreadLocal<TruthPolation> truthpolations = ThreadLocal.withInitial(() -> {
        return new TruthPolation(MAX_TRUTHPOLATION_SIZE);
    });


    private long min = Tense.ETERNAL, max = Tense.ETERNAL;
    private long lastUpdate = Tense.TIMELESS;
    private int capacity;

    public MicrosphereTemporalBeliefTable(int initialCapacity) {
        super();
        capacity(initialCapacity);
    }

    public void capacity(int initialCapacity) {
        this.capacity = initialCapacity;
    }

    @Override
    public final int capacity() {
        return capacity;
    }

    public static float rank(@NotNull Task t, long when, long now, float ageFactor) {
        return rankTemporalByConfidenceAndOriginality(t, when, now, ageFactor, -1);
        //return rankTemporalByConfidence(t, when, now, ageFactor, -1);
    }

    @Override
    public Task add(@NotNull Task input, EternalTable eternal, List<Task> displ, @NotNull NAR nar) {


        int cap = capacity();
        if (cap == 0)
            return null;

        this.lastUpdate = nar.time();


        //the result of compression is processed separately
        Task next = compress(input, nar.time(), eternal, displ);
        if (next == null) {
            //not compressible with respect to this input, so reject the input
            return null;
        } else if (next != input) {
            // else: the result of compression has freed a space for the incoming input
            //nar.runLater(()->{
            nar.process(next);
            //});
        } else {
            //space has been freed for the input, no merging resulted

        }

        if (isFull()) {
            //WHY DOES THIS HAPPEN, IS IT DANGEROUS
            //if (Global.DEBUG)
            //throw new RuntimeException(this + " compression failed");
            return null;
        }

        add(input);

        return input;
    }

    @Override public final boolean isFull() {
        return size() == capacity();
    }

//    @Override
//    public void minTime(long minT) {
//        this.min = minT;
//    }
//
//    @Override
//    public void maxTime(long maxT) {
//        this.max = maxT;
//    }

    @Override
    public long minTime() {
        ageFactor();
        return min;
    }

    @Override
    public long maxTime() {
        ageFactor();
        return max;
    }

    @Override
    public boolean add(Task t) {
        if (super.add(t)) {
            long o = t.occurrence();
            if ((o < min) || (o > max))
                invalidateRange();
            return true;
        }
        return false;
    }



    @Override
    public void remove(@NotNull Task removed, List<Task> displ) {
        @Nullable Task actuallyRemoved = super.removed(removed);

        if (actuallyRemoved != removed) {
            removed.delete(); //ensure this extra copy is deleted
        }

        displ.add(actuallyRemoved);

        invalidRangeIfLimit(removed);

    }

    private void invalidRangeIfLimit(@NotNull Task removed) {
        long occ = removed.occurrence();
        if ((occ == min) || (occ == max)) {
            invalidateRange();
        }
    }


    @Override
    public final boolean remove(Object object) {
        if (super.remove(object)) {
            invalidRangeIfLimit((Task)object);
            return true;
        }
        return false;
    }

    @Override
    public final Task remove(int index) {
        Task t = super.remove(index);
        if (t!=null) {
            invalidRangeIfLimit(t);
        }
        return t;
    }

    private final void invalidateRange() {
        min = max = ETERNAL;
    }

    @Nullable
    public Task weakest() {
        if (lastUpdate == Tense.TIMELESS)
            throw new RuntimeException("unable to measure weakest without knowing current time");
        return weakest(lastUpdate, null, Float.POSITIVE_INFINITY);
    }


    protected final void removeWeakest(@Nullable Object reason) {

        int sizeBefore = size();
        compress();
        if (size() < sizeBefore)
            return; //compression successful

        removed(weakest()).delete(reason);
    }

    @Nullable
    public Task weakest(long now, @Nullable Task excluding, float minRank) {
        Task weakest = null;
        float weakestRank = minRank;
        int n = size();

        float ageFactor = ageFactor();
        long[] excludingEvidence = excluding != null ? excluding.evidence() : null;
        for (int i = 0; i < n; i++) {

            Task ii = get(i);
            if (excluding != null &&
                    ((ii == excluding) || (!Param.REVECTION_ALLOW_MERGING_OVERLAPPING_EVIDENCE && Stamp.overlapping(excludingEvidence, ii.evidence()))))
                continue;

            //consider ii for being the weakest ranked task to remove
            float r = rank(ii, now, now, ageFactor);
            if (r < weakestRank) {
                weakestRank = r;
                weakest = ii;
            }

        }

        return weakest;
    }


    public float ageFactor() {

        if (min == ETERNAL) {
            //invalidated, recalc:
            long tmin = Long.MAX_VALUE, tmax = Long.MIN_VALUE;

            //TODO synchronized(..

            int s = size();
            for (int i = 0; i < s; i++) {
                long o = get(i).occurrence();
                if (o < tmin) tmin = o;
                if (o > tmax) tmax = o;
            }

            this.min = tmin;
            this.max = tmax;
        }

        //return 1f;
        long range = max - min;
        /* history factor:
           higher means it is easier to hold beliefs further away from current time at the expense of accuracy
           lower means more accuracy at the expense of shorter memory span
     */
        float historyFactor = Param.TEMPORAL_DURATION;
        return (range == 0) ? 1 :
                ((1f) / (range * historyFactor));
    }

    @Nullable
    protected Task compress() {
        return compress(null, lastUpdate, null, new FasterList() /* HACK unnecessary */);
    }

    /**
     * frees one slot by removing 2 and projecting a new belief to their midpoint. returns the merged task
     */
    protected Task compress(@Nullable Task input, long now, @Nullable EternalTable eternal, List<Task> displ) {

        int cap = capacity();
        if (size() < cap || removeAlreadyDeleted() < cap) {
            return input; //no need for compression
        }


        float inputRank = input != null ? rank(input, now, now, ageFactor()) : Float.POSITIVE_INFINITY;

        Task a = weakest(now, null, inputRank);
        if (a == null)
            return null;

        Task b = weakest(now, a, Float.POSITIVE_INFINITY);
        if (a == b)
            throw new RuntimeException();

        Task merged;
        if (b != null) {
            merged = merge(a, b, now, eternal);

            remove(b);
            TaskTable.removeTask(b, "Revection Revision", displ);
        } else {
            merged = null;
        }

        remove(a);
        TaskTable.removeTask(a, (b == null) ? "Revection Forget" : "Revection Revision", displ);


        return merged != null ? merged : input;
    }

    /**
     * t is the target time of the new merged task
     */
    @Nullable
    private Task merge(@NotNull Task a, @NotNull Task b, long now, @Nullable EternalTable eternal) {
        double ac = a.confWeight();
        double bc = b.confWeight();
        long mid = (long) ((a.occurrence() * ac + b.occurrence() * bc) / (ac + bc));

        //more evidence overlap indicates redundant information, so reduce the confWeight (measure of evidence) by this amount
        //TODO weight the contributed overlap amount by the relative confidence provided by each task
        float overlap = Stamp.overlapFraction(a.evidence(), b.evidence());

        float distanceFade = min!=ETERNAL ? TruthFunctions.projection(
                //minTime(),
                //maxTime(),
                //mid
                (a.occurrence()),
                (b.occurrence()),
                (now) //relative to current NAR time
                //(mid) //relative to the midpoint of the tasks
        ) : 1f;

        float confScale = Param.REVECTION_CONFIDENCE_FACTOR * distanceFade * (1f - overlap);

        if (confScale < Param.BUDGET_EPSILON) //TODO use NAR.confMin which will be higher than this
            return null;

        Truth truth = truth(mid, now, eternal);
        if (truth!=null)
            truth = truth.confMultViaWeight(confScale);

        return truth != null ? Revision.merge(a, b, mid, now, truth) : null;
    }


    public long range() {
        return maxTime()-minTime();
    }

    /** normalize relative to the min/max span of this table */
    public float tnorm(long absolute) {
        double range = range();
        if (range == 0) return 0;
        return (float)((absolute - min) / range);
    }

    @Nullable
    @Override
    public Task strongest(long when, long now, @Nullable Task against) {

        //removeDeleted();

        //find the best balance of temporal proximity and confidence
        int ls = size();
        if (ls == 1)
            return get(0); //the only task

        Task best = null;

        float ageFactor = ageFactor();

        float bestRank = -1;

        for (int i = 0; i < ls; i++) {
            Task x = get(i);
            if (x == null) continue;

            float r = rank(x, when, now, ageFactor);

            if (against != null && Stamp.overlapping(x.evidence(), against.evidence())) {
                r *= Param.PREMISE_MATCH_OVERLAP_MULTIPLIER;
            }

            if (r > bestRank) {
                best = x;
                bestRank = r;
            }
        }

        return best;

    }

    @Nullable
    @Override
    public final Truth truth(long when, long now, @Nullable EternalTable eternal) {
        int s = size();
        switch (s) {
            case 0:
                return null;
            case 1:
                return get(0).projectTruth(when, now, false);
            default:
                return truthpolations.get().truth(when, this, eternal != null ? eternal.first() : null);
        }
    }


    private int removeAlreadyDeleted() {
        int s = size();
        for (int i = 0; i < s; ) {
            Task x = get(i);
            if (x == null || x.isDeleted()) {
                remove(i);
                s--;
            } else {
                i++;
            }
        }
        return s;
    }


    public final boolean removeIf(@NotNull Predicate<? super Task> o) {

        IntArrayList toRemove = new IntArrayList();
        for (int i = 0, thisSize = this.size(); i < thisSize; i++) {
            Task x = this.get(i);
            if ((x == null) || (o.test(x)))
                toRemove.add(i);
        }
        if (toRemove.isEmpty())
            return false;
        toRemove.forEach(this::remove);
        return true;
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

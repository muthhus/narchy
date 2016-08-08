package nars.concept.table;

import nars.NAR;
import nars.Param;
import nars.nal.Stamp;
import nars.task.Revision;
import nars.task.Task;
import nars.task.TruthPolation;
import nars.truth.Truth;
import nars.util.data.list.FasterList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static nars.concept.table.BeliefTable.rankTemporalByConfidence;
import static nars.concept.table.BeliefTable.rankTemporalByConfidenceAndOriginality;
import static nars.nal.UtilityFunctions.and;
import static nars.truth.TruthFunctions.projection;

/**
 * stores the items unsorted; revection manages their ranking and removal
 */
public class MicrosphereTemporalBeliefTable extends FasterList<Task> implements TemporalBeliefTable {

    static final int MAX_TRUTHPOLATION_SIZE = 64;
    static final ThreadLocal<TruthPolation> truthpolations = ThreadLocal.withInitial(() -> {
        return new TruthPolation(MAX_TRUTHPOLATION_SIZE);
    });

    private int capacity;

    public MicrosphereTemporalBeliefTable(int initialCapacity) {
        super();
        this.capacity = initialCapacity;
    }


    public void capacity(int newCapacity, long now, List<Task> displaced) {
        this.capacity = newCapacity;
        while (this.size() > newCapacity) {
            removeWeakest(displaced, now);
        }

    }

    @Override
    public final int capacity() {
        return capacity;
    }

    public static float rank(@NotNull Task t, long when, long now) {
        return rankTemporalByConfidenceAndOriginality(t, when, now, -1);
        //return rankTemporalByConfidence(t, when, now, -1);
    }

    @Override
    public Task add(@NotNull Task input, EternalTable eternal, List<Task> displ, @NotNull NAR nar) {


        int cap = capacity();
        if (cap == 0)
            return null;



        //the result of compression is processed separately
        Task next = compress(input, nar.time(), eternal, displ);
        if (next == null) {
            //not compressible with respect to this input, so reject the input
            return null;
        } else if (next != input) {
            // else: the result of compression has freed a space for the incoming input
            //nar.runLater(()->{
            //nar.input
            nar.inputLater(next);
            //});
        } else {
            //space has been freed for the input, no merging resulted

        }

        if (!isFull() && add(input)) {
            return input;
        } else {
            //HACK DOES THIS HAPPEN and WHY, IS IT DANGEROUS
            //if (Global.DEBUG)
            //throw new RuntimeException(this + " compression failed");
            return null;
        }
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
    public final void range(long[] t) {
        for (Task x : this.items) {
            if (x !=null) {
                long o = x.occurrence();
                if (o < t[0]) t[0] = o;
                if (o > t[1]) t[1] = o;
            }
        }
    }


    @Override
    public boolean remove(Object object) {
        return super.remove(object);
    }

    private final void remove(@NotNull Task removed, List<Task> displ) {
        int i = indexOf(removed);
        if (i == -1)
            return;

        Task x = remove(i, displ);
        if (x!=removed) {
            throw new RuntimeException("equal but different instances: " + removed);
        }
    }




//    @Override
//    public final boolean remove(Object object) {
//        if (super.remove(object)) {
//            invalidRangeIfLimit((Task)object);
//            return true;
//        }
//        return false;
//    }


    private final Task remove(int index, List<Task> displ) {
        @Nullable Task t = this.remove(index);
        if (t!=null) {
            displ.add(t);
        }
        return t;
    }

    @Nullable
    public Task weakest(long now) {
        return weakest(now, null, Float.POSITIVE_INFINITY);
    }


    protected final void removeWeakest(List<Task> displ, long now) {

        int sizeBefore = size();
        compress(displ, now);
        if (size() < sizeBefore)
            return; //compression successful

        remove(weakest(now), displ);
    }

    @Nullable
    public Task weakest(long now, @Nullable Task toMergeWith, float minRank) {
        Task weakest = null;
        float weakestRank = minRank;
        int n = size();

        long[] mergeEvidence = toMergeWith != null ? toMergeWith.evidence() : null;
        for (int i = 0; i < n; i++) {

            Task ii = get(i);
            if (toMergeWith != null &&
                    ((!Param.REVECTION_ALLOW_MERGING_OVERLAPPING_EVIDENCE &&
                            (/*Stamp.isCyclic(iiev) || */Stamp.overlapping(mergeEvidence, ii.evidence()))
                    )))
                continue;

            //consider ii for being the weakest ranked task to remove
            float r = rank(ii, now, now);
                    //(toMergeWith!=null ? (1f / (1f + Math.abs(ii.freq()-toMergeWith.freq()))) : 1f); //prefer close freq match
            if (r < weakestRank) {
                weakestRank = r;
                weakest = ii;
            }

        }

        return weakest;
    }




    @Nullable
    protected Task compress(List<Task> displ, long now) {
        return compress(null, now, null, displ);
    }

    /**
     * frees one slot by removing 2 and projecting a new belief to their midpoint. returns the merged task
     */
    protected Task compress(@Nullable Task input, long now, @Nullable EternalTable eternal, List<Task> displ) {

        int cap = capacity();
        if (size() < cap || removeAlreadyDeleted(displ) < cap) {
            return input; //no need for compression
        }


        float inputRank = input != null ? rank(input, now, now) : Float.POSITIVE_INFINITY;

        Task a = weakest(now, null, inputRank);
        if (a == null)
            return null;

        remove(a, displ);

        Task b = weakest(now, a, Float.POSITIVE_INFINITY);

        if (b != null) {

            //TaskTable.removeTask(b, "Revection Revision", displ);
            remove(b, displ);

            return merge(a, b, now, eternal);
        } else {
            return input;
        }

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

        /**
         * compute an integration of the area under the trapezoid formed by
         * computing the projected truth at the 'a' and 'b' time points
         * and mixing them by their relative confidence.
         * this is to represent a loss of confidence due to diffusion of
         * truth across a segment of time spanned by these two tasks as
         * they are merged into one.
         */
        float diffuseCost;
        /*if (minTime()==ETERNAL) {
            throw new RuntimeException(); //shouldnt happen
        } else {*/
        long aocc = a.occurrence();
        long bocc = b.occurrence();
        float aProj = projection(mid, now, aocc);
        float bProj = projection(mid, now, bocc);

        //TODO lerp blend these values ? avg? min?
        diffuseCost =
                //aveAri(aProj + bProj)/2f;
                //Math.min(aProj, bProj);
                and(aProj, bProj);

//        float relMin = projection(minTime(), mid, now);
//        float relMax = projection(maxTime(), mid, now);
//        float relevance = Math.max(relMin, relMax );


        float confScale = Param.REVECTION_CONFIDENCE_FACTOR * diffuseCost * (1f - overlap);

        if (confScale < Param.BUDGET_EPSILON) //TODO use NAR.confMin which will be higher than this
            return null;

        confScale = Math.min(1f, confScale);

        Truth truth = truth(mid, now, eternal);
        if (truth!=null)
            truth = truth.confMult(confScale);

        if (truth != null)
            return Revision.merge(a, b, mid, now, truth);

        return null;
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

        float bestRank = -1;

        for (int i = 0; i < ls; i++) {
            Task x = get(i);
            if (x == null) continue;

            float r = rank(x, when, now);

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

        //make a copy so that truthpolation can freely operate asynchronously
        int s;
        Task[] copy;
        synchronized (this) {
            s = size();
            if (s == 0) return null;

            copy = toArray(new Task[s]);
        }

        if (s == 1)
            return copy[0].projectTruth(when, now, false);
        else
            return truthpolations.get().truth(when, eternal != null ? eternal.strongest() : null, copy);

    }


    private int removeAlreadyDeleted(@NotNull List<Task> displ) {
        int s = size();
        for (int i = 0; i < s; ) {
            Task x = get(i);
            if (x == null || x.isDeleted()) {
                remove(i, displ);
                s--;
            } else {
                i++;
            }
        }
        return s;
    }


    //    public final boolean removeIf(@NotNull Predicate<? super Task> o) {
//
//        IntArrayList toRemove = new IntArrayList();
//        for (int i = 0, thisSize = this.size(); i < thisSize; i++) {
//            Task x = this.get(i);
//            if ((x == null) || (o.test(x)))
//                toRemove.add(i);
//        }
//        if (toRemove.isEmpty())
//            return false;
//        toRemove.forEach(this::remove);
//        return true;
//    }

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

package nars.table;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.learn.microsphere.InterpolatingMicrosphere;
import nars.task.Revision;
import nars.task.TruthPolation;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import nars.util.Util;
import nars.util.data.list.MultiRWFasterList;
import org.eclipse.collections.api.block.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.lang.Long.MAX_VALUE;
import static java.lang.Long.MIN_VALUE;
import static nars.learn.microsphere.InterpolatingMicrosphere.timeDecay;
import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.c2w;
import static nars.util.Util.sqr;

/**
 * stores the items unsorted; revection manages their ranking and removal
 */
public class MicrosphereTemporalBeliefTable implements TemporalBeliefTable {



    private volatile int capacity;
    final MultiRWFasterList<Task> list;

    public MicrosphereTemporalBeliefTable(int initialCapacity) {
        super();
        this.list = MultiRWFasterList.newList(initialCapacity);
        this.capacity = initialCapacity;
    }

    @NotNull
    @Override
    public Iterator<Task> iterator() {
        throw new UnsupportedOperationException();
        //return list.iterator();
    }

    @Override
    public final void forEach(Consumer<? super Task> action) {
        list.forEach(action);
    }

    public void capacity(int newCapacity, long now, @NotNull List<Task> trash) {

        if (this.capacity != newCapacity) {

            this.capacity = newCapacity;

            clean(trash);

            int nullAttempts = 4; //
            int toRemove = list.size() - newCapacity;
            for (int i = 0; i < toRemove && nullAttempts > 0; ) {

                Task ww = matchWeakest(now); //read-lock
                if (ww!=null) {
                    remove(ww, trash);
                    i++;
                } else {
                    nullAttempts--;
                }
            }
        }

    }

    @Override
    public final int size() {
        return list.size();
    }

    @Override
    public final boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public final int capacity() {
        return capacity;
    }



    @Nullable
    @Override
    public final TruthDelta add(@NotNull Task input, EternalTable eternal, @NotNull List<Task> trash, Concept concept, @NotNull NAR nar) {

        int cap = capacity();
        if (cap == 0)
            return null;

        //the result of compression is processed separately
        final TruthDelta[] delta = new TruthDelta[1];

        list.withWriteLockAndDelegate(l -> {
            final Truth before;

            long now = nar.time();

            before = truth(now, eternal);

            Task next;
            if ((next = compress(input, now, eternal, trash, concept)) != null) {

                add(input);

                if (next != input && list.size() + 1 <= cap) {
                    add(next);
                }

                final Truth after = truth(now, eternal);
                delta[0] = new TruthDelta(before, after);
            }
        });

        TruthDelta x = delta[0];
        return x;
    }

    private void add(@NotNull Task x) {
        if (list.add(x)) {
            long o = x.occurrence();
            if (minT!=MAX_VALUE) {
                if (o < minT) minT = o;
            }
            if (maxT!=Long.MIN_VALUE) {
                if (o > maxT) maxT = o;
            }
        }
    }

    static float rankTemporalByConfidence(@Nullable Task t, long now, float duration) {
        return t == null ? -1 : timeDecay(t.confWeight(), duration, Math.abs(t.occurrence() - now));
    }

    /** HACK use of volatiles here is a hack. it may rarely cause the bag to experience flashbacks. proper locking can solve this */
    private volatile long minT = MAX_VALUE, maxT = Long.MIN_VALUE;

    public long range() {
        if (minT==MAX_VALUE || maxT==MIN_VALUE) {
            //cached valus invalidated, re-compute
            forEach(u -> {
                long o = u.occurrence();
                if (minT > o)
                    minT = o;
                if (maxT < o)
                    maxT = o;
            });
        } else {
            //using cached value
        }

        if (minT==MAX_VALUE || maxT==MIN_VALUE) {
            return 1; //empty probably} else {
        } else {

            return Math.max(0, maxT - minT);
        }

    }

    public float duration() {
        return ((float)range()) / size();
    }

    @Override
    public boolean removeIf(@NotNull Predicate<? super Task> o, @NotNull List<Task> trash) {
        return list.removeIf(((Predicate<Task>) t -> {
            if (o.test(t)) {
                onRemoved( t, trash );
            }
            return false;
        }));
    }


    private void onRemoved(@NotNull Task t, @NotNull List<Task> trash) {
        if (minT != MAX_VALUE) {
            long o = t.occurrence();
            if ((minT == o) || (maxT == o)) {
                invalidateDuration();
            }
        }

        trash.add(t);
    }

    private void invalidateDuration() {
        //invalidate
        minT = MAX_VALUE;
        maxT = Long.MIN_VALUE;
    }


    @Override
    public final boolean isFull() {
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

//
//    @Override
//    public final void range(long[] t) {
//        for (Task x : this.items) {
//            if (x != null) {
//                long o = x.occurrence();
//                if (o < t[0]) t[0] = o;
//                if (o > t[1]) t[1] = o;
//            }
//        }
//    }


    public boolean remove(@NotNull Object object) {
        return list.remove(object);
    }

    private final boolean remove(@NotNull Task x, @NotNull List<Task> trash) {
        if (list.remove(x)) {
            onRemoved(x, trash);
            return true;
        }
        return false;
    }


//    @Override
//    public final boolean remove(Object object) {
//        if (super.remove(object)) {
//            invalidRangeIfLimit((Task)object);
//            return true;
//        }
//        return false;
//    }


    public Task matchWeakest(long now) {
        float duration = duration();
        return list.minBy(x -> rankTemporalByConfidence(x, now, duration));
    }

    public Task matchMerge(long now, @NotNull Task toMergeWith) {
        return list.maxBy(rankMatchMerge( toMergeWith, now ));
    }

    /** max value given to the ideal match for the provided task to be merged with */
    @NotNull public Function<Task, Float> rankMatchMerge(@NotNull Task y, long now) {

        //prefer (when selecting by minimum rank:)
        //  less freq delta
        //  less confidence
        //  less time delta
        //  more time from now

        long yo = y.occurrence();
        float duration = Math.abs(now - yo);
        float yf = y.freq();

        return x -> {

            long xo = x.occurrence();

            float d2 = duration/2f;
            return (1f + (1f - Math.abs(x.freq() - yf)))
                    * (1f + (1f - y.conf()))
                    * (1f + (1f - (float)sqr(xo - yo) / d2))
                    / (1f + (float)sqr(now - xo) / d2);
        };

    }

//    @NotNull public Function<Task, Float> rankPenalizingOverlap(long now, @NotNull Task toMergeWith) {
//        long occ = toMergeWith.occurrence();
//        ImmutableLongSet toMergeWithEvidence = toMergeWith.evidenceSet();
//        return x -> rankPenalizingOverlap(x, toMergeWithEvidence, occ, now);
//    }

//    public float rankPenalizingOverlap(@Nullable Task x, @NotNull LongSet evidence, long occ, long now) {
//        //return rankTemporalByConfidenceAndOriginality(t, when, now, -1);
//        return (x == null) ? Float.NEGATIVE_INFINITY : (Stamp.overlapFraction(evidence, x.evidence()) * rankTemporalByConfidence(x, now));
//    }



    /**
     * frees one slot by removing 2 and projecting a new belief to their midpoint. returns the merged task
     */
    @Nullable
    protected Task compress(@Nullable Task input, long now, @Nullable EternalTable eternal, @NotNull List<Task> trash, @Nullable Concept concept) {

        int cap = capacity();

        if (size() < cap || clean(trash)) {
            return input; //no need for compression
        }

        float dur = duration();

        //return rankTemporalByConfidenceAndOriginality(t, when, now, -1);
        float inputRank = input != null ? rankTemporalByConfidence(input, now, dur) : Float.POSITIVE_INFINITY;

        Task a = matchWeakest(now);
        //return rankTemporalByConfidenceAndOriginality(t, when, now, -1);
        if (a == null || inputRank <= rankTemporalByConfidence(a, now, dur) || !remove(a, trash)) {
            //dont continue if the input was too weak, or there was a problem removing a (like it got removed already by a different thread or something)
            return null;
        }

        Task b = matchMerge(now, a);
        if (b != null && remove(b, trash)) {
            return merge(a, b, now, concept, eternal);
        } else {
            return input;
        }

    }

    /**
     * t is the target time of the new merged task
     */
    @Nullable
    private Task merge(@NotNull Task a, @NotNull Task b, long now, @NotNull Concept concept, @Nullable EternalTable eternal) {

        float ac = c2w(a.conf());
        float bc = c2w(b.conf());
        long mid = (long) Math.round(Util.lerp(a.occurrence(), b.occurrence(), (double)ac / (ac + bc)));

//        float f =
//                //more evidence overlap indicates redundant information, so reduce the confWeight (measure of evidence) by this amount
//                //TODO weight the contributed overlap amount by the relative confidence provided by each task
//                //1f - Stamp.overlapFraction(a.evidence(), b.evidence())/2f;
//                1f;
        float p = ac/(ac+bc);
        Truth t = Revision.revise(a, p, b, Param.TRUTH_EPSILON /*nar.confMin*/);

        if (t != null)
            return Revision.mergeInterpolate(a, b, mid, now, t, concept);

        return null;
    }


    @Nullable
    @Override
    public final Task match(long when, long now, @Nullable Task against) {



        float dur = duration();
        if (against == null) {

            //System.out.println(this + " against: " + against + " @ " + now);

            //list.forEach(x -> System.out.println("\t" + x + "  " + rankTemporalByConfidence(x, now, dur)));

            Task l = list.maxBy(x -> rankTemporalByConfidence(x, now, dur));
            //System.out.println(l);
            return l;

        } else {
            return list.maxBy(x -> rankTemporalByConfidence(x, against.occurrence(), dur));
            //return list.maxBy(rankPenalizingOverlap(now, against));
        }

    }

    @Nullable
    public final Truth truth(long when, @Deprecated @Nullable EternalTable eternal) {
        return truth(when, when, eternal);
    }

    @Nullable
    @Override
    public final Truth truth(long when, long now, @Nullable EternalTable eternal) {


        Task topEternal = eternal.strongest();
        boolean includeEternal = topEternal != null;

        Task[] tr = list.toArray((i) -> new Task[i], includeEternal ? 1 : 0);
        int s = tr.length;
        if (includeEternal)
            tr[s -1] = topEternal;

        switch (s) {

            case 0:
                return null;

            case 1:
                Task the = tr[0];
                Truth res = the.truth();
                long o = the.occurrence();
                if ((now == ETERNAL || when == now) && o == when) //optimization: if at the current time and when
                    return res;
                else
                    return Revision.project(res, when, now, o, false);

            default:
                InterpolatingMicrosphere.LightCurve x = InterpolatingMicrosphere.lightCurve(duration());
                return new TruthPolation(s).truth(when, tr, x);

        }

    }



    private boolean clean(@NotNull List<Task> trash) {
        return list.removeIf(x -> {
            if (x == null) {
                return true;
            } else if (x.isDeleted()) {
                onRemoved(x, trash);
                return true;
            } else {
                return false;
            }
        });
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

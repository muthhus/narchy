package nars.table;

import jcog.Util;
import jcog.list.MultiRWFasterList;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.nal.Stamp;
import nars.task.Revision;
import nars.task.TruthPolation;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

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

    public void capacity(int newCapacity, NAR nar) {

        if (this.capacity != newCapacity) {

            this.capacity = newCapacity;

            int toRemove = list.size() - newCapacity;
            if (toRemove > 0) {

                clean(nar);

                toRemove = list.size() - newCapacity;
                if (toRemove > 0) {

                    List<Task> merged = $.newArrayList(0);
                    List<Task> trash = $.newArrayList(0);

                    synchronized (list) {


                        //compress until under-capacity
                        list.withWriteLockAndDelegate((l) -> {

                            float dur = nar.time.dur();

                            float confMin = nar.confMin.floatValue();

                            while (list.size() > capacity) {
                                long now = nar.time();

                                Task a = matchWeakest(now);

                                Task b = matchMerge(now, a, dur);

                                Task c = (b != null && b != a) ? merge(a, b, now, confMin) : null;

                                removeLater(a, trash);

                                if (c != null) {
                                    removeLater(b, trash);

                                    add(c);
                                    merged.add(c);
                                }

                            }

                        });

                    }

                    nar.tasks.change(merged, trash);

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
    public final TruthDelta add(@NotNull Task input, EternalTable eternal, Concept concept, @NotNull NAR nar) {

        int cap = capacity();
        if (cap == 0)
            return null;

        //the result of compression is processed separately
        final TruthDelta[] delta = new TruthDelta[1];

        Task[] merged = new Task[1];
        Task[] trash = new Task[2];

        list.withWriteLockAndDelegate(l -> {
            final Truth before;

            long now = nar.time();

            before = truth(now, eternal);

            Task next;
            if ((next = compress(input, now, nar, trash)) != null) {

                add(input);
                //this will be inserted to the index in a callee method

                if (next != input /*&& list.size() + 1 <= cap*/) {
                    add(next);
                    merged[0] = next;
                }

                final Truth after = truth(now, eternal);
                delta[0] = new TruthDelta(before, after);
            }
        });


        //update the index outside of the atomic procedure
        for (Task x : trash)
            if (x!=null)
                nar.tasks.remove(x);

        for (Task x : merged)
            if (x!=null)
                nar.tasks.addIfAbsent(x);

        return delta[0];
    }

    private void add(@NotNull Task x) {
        boolean added = list.add(x);
        assert added;
    }


//    /** HACK use of volatiles here is a hack. it may rarely cause the bag to experience flashbacks. proper locking can solve this */
//    private volatile long minT = MAX_VALUE, maxT = MIN_VALUE;

//    public long range() {
//        if (minT==MAX_VALUE || maxT==MIN_VALUE) {
//            //cached valus invalidated, re-compute
//            forEach(u -> {
//                long o = u.occurrence();
//                if (minT > o)
//                    minT = o;
//                if (maxT < o)
//                    maxT = o;
//            });
//        } else {
//            //using cached value
//        }
//
//        if (minT==MAX_VALUE || maxT==MIN_VALUE) {
//            return 1; //empty probably} else {
//        } else {
//
//            return Math.max(0, maxT - minT);
//        }
//
//    }

//    public float duration() {
//        //return (((float)range()) / size()) * 2f;
//        return 1f;
//    }

    @Override
    public boolean removeIf(@NotNull Predicate<? super Task> o, @NotNull NAR nar) {

        List<Task> toUnindex = $.newArrayList(0);

        boolean r = list.removeIf(((Predicate<Task>) t -> {
            if (o.test(t)) {
                toUnindex.add(t);
                t.delete();
                return true;
            }
            return false;
        }));

        //unindex the tasks outside of the atomic procedure
        if (r) {
            for (Task x : toUnindex) {
                nar.tasks.remove(x);
            }
            return true;
        }
        return false;
    }


    @Override
    public final boolean isFull() {
        return size() == capacity();
    }


    //    private void invalidateDuration() {
//        //invalidate
//        minT = MAX_VALUE;
//        maxT = MIN_VALUE;
//    }



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


    private final boolean removeLater(@NotNull Task x, List<Task> trash) {
        if (list.remove(x)) {
            x.delete();
            trash.add(x);
            return true;
        }
        return false;
    }


    public Task matchWeakest(long now) {
        return list.minBy(x -> rankTemporalByConfidence(x, now));
    }

    final float rankTemporalByConfidence(@Nullable Task t, long when) {
        return t == null ? -1 : t.confWeight(when) * (1f + t.range());
        //* (t.dur()+1);
    }

    public Task matchMerge(long now, @NotNull Task toMergeWith, float dur) {
        return list.maxBy(rankMatchMerge(toMergeWith, now, dur));
    }

    /**
     * max value given to the ideal match for the provided task to be merged with
     */
    @NotNull
    public Function<Task, Float> rankMatchMerge(@NotNull Task y, long now, float dur) {

        //prefer (when selecting by minimum rank:)
        //  less freq delta
        //  less time delta
        //  more time from now
        //  less range

        long yo = y.mid();
        float yf = y.freq();

        return x -> {
            if (x == y)
                return Float.NEGATIVE_INFINITY;

            long xo = x.mid();
//            float xtc = x.conf(yo);
//            if (xtc!=xtc) xtc = 0;

            return (1f + (1f - Math.abs(x.freq() - yf)))
                    //* (1f + (1f - xtc))
                    * (1f + 1f / (1f + x.range()))
                    * (1f + 1f / (1f + x.dur()))
                    * (1f + TruthPolation.evidenceDecay(1, dur, Math.abs(xo - now) + Math.abs(xo - yo)));
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
    protected Task compress(@Nullable Task input, long now, @NotNull NAR nar, Task[] trash) {

        int cap = capacity();

        if (size() < cap || clean(nar)) {
            return input; //no need for compression
        }

        //return rankTemporalByConfidenceAndOriginality(t, when, now, -1);
        float inputRank = input != null ? rankTemporalByConfidence(input, now) : Float.POSITIVE_INFINITY;

        Task a = matchWeakest(now);
        //return rankTemporalByConfidenceAndOriginality(t, when, now, -1);
        if (a == null || inputRank <= rankTemporalByConfidence(a, now)) {
            //dont continue if the input was too weak
            return null;
        }

        a.delete();
        list.remove(a);
        trash[0] = a;

        Task b = matchMerge(now, a, nar.time.dur());
        if (b != null) {
            Task merged = merge(a, b, now, nar.confMin.floatValue());

            b.delete();
            list.remove(b);
            trash[1] = b;

            return merged;
        } else {
            return input;
        }

    }

    /**
     * t is the target time of the new merged task
     */
    @Nullable
    private Task merge(@NotNull Task a, @NotNull Task b, long now, float confMin) {

        float ac = a.evi();
        float bc = b.evi();

        float f =
//                //more evidence overlap indicates redundant information, so reduce the confWeight (measure of evidence) by this amount
//                //TODO weight the contributed overlap amount by the relative confidence provided by each task
                1f - Stamp.overlapFraction(a.evidence(), b.evidence()) / 2f;
//                1f;

        float p = ac / (ac + bc);
        Truth t = Revision.revise(a, p, b, f, confMin);

        if (t != null) {
            long mid = (long) Math.round(Util.lerp(a.occurrence(), b.occurrence(), (double) ac / (ac + bc)));
            return Revision.mergeInterpolate(a, b, mid, now, t, true);
        }

        return null;
    }


    @Nullable
    @Override
    public final Task match(long when, @Deprecated long now, @Nullable Task against) {

        Task result = null;
        if (list.ifNotEmptyAcquireReadLock()) {
            MutableList<Task> l = list.internal();
            switch (l.size()) {
                case 0:
                    throw new RuntimeException("should not reach here");
                case 1:
                    result = l.get(0); //special case avoid creating the lambda
                default:
                    result = l.maxBy(x -> rankTemporalByConfidence(x, when));
            }
            list.unlockReadLock();
        }

        return result;

        //if (against == null) {
//
//        } else {
//            long then = when != ETERNAL ? when : now;
//            return list.maxBy(x -> rankTemporalByConfidence(x, then));
//            //return list.maxBy(rankPenalizingOverlap(now, against));
//        }

    }

    @Nullable
    public final Truth truth(long when, @Deprecated @Nullable EternalTable eternal) {
        return truth(when, when, eternal);
    }

    @Nullable
    @Override
    public final Truth truth(long when, long now, @Nullable EternalTable eternal) {

        Truth result = null;
        if (list.ifNotEmptyAcquireReadLock()) {

            result = TruthPolation.truth(eternal.match(), when, list.internal());

            list.unlockReadLock();
        }

        return result;
    }

    private final boolean clean(NAR nar) {
        return removeIf(Task::isDeleted, nar);
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

package nars.table;

import jcog.Util;
import jcog.list.FasterList;
import jcog.list.MultiRWFasterList;
import jcog.math.Interval;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.budget.Budget;
import nars.concept.Concept;
import nars.task.Revision;
import nars.task.TruthPolation;
import nars.time.Time;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import static java.lang.Math.abs;

/**
 * stores the items unsorted; revection manages their ranking and removal
 */
public class MicrosphereTemporalBeliefTable extends MultiRWFasterList<Task> implements TemporalBeliefTable {

    private int capacity;

    public MicrosphereTemporalBeliefTable(int initialCapacity) {
        super(new FasterList<Task>(initialCapacity));
        this.capacity = initialCapacity;
    }

    /**
     * warning: not efficient as forEach visiting
     */
    @NotNull
    @Override
    public Iterator<Task> iterator() {
        return toImmutable().iterator();
    }


    public void capacity(int newCapacity, NAR nar) {

        if (this.capacity != newCapacity) {

            this.capacity = newCapacity;

            //compress until under-capacity
            ifSizeExceedsWriteWith(newCapacity, (l) -> {

                int toRemove = l.size() - newCapacity; //will be positive

                clean(l);

                toRemove = l.size() - newCapacity;
                if (toRemove > 0) {

                    Time time = nar.time;

                    float dur = time.dur();
                    long now = time.time();

                    float confMin = nar.confMin.floatValue();

                    Function<Task, Float> rank = temporalConfidence(now, dur);

                    while (l.size() > capacity) {

                        Task a = l.minBy(rank);

                        Task b = matchMerge(l, now, a, dur);

                        Task c = (b != null && b != a) ? merge(a, b, now, confMin) : null;

                        remove(l, a);

                        if (c != null) {
                            remove(l, b);

                            l.add(c);

                        }

                    }

                }

            });

        }

    }


    @Override
    public boolean remove(Task x) {
        final boolean[] removed = new boolean[1];
        withWriteLockAndDelegate(l -> {
            removed[0] = l.remove(x);
        });
        x.delete();
        return removed[0];
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


        withWriteLockAndDelegate(l -> {
            final Truth before;


            Time time = nar.time;
            long now = time.time();
            float dur = time.dur();

            before = truth(now, dur, eternal);

            Task next;

            float confMin = nar.confMin.floatValue();
            if ((next = compress(input, now, l, dur, confMin)) != null) {

                l.add(input);
                //this will be inserted to the index in a callee method

                if (next != input /*&& list.size() + 1 <= cap*/) {
                    l.add(next);
                }

                final Truth after = truth(now, dur, eternal);
                delta[0] = new TruthDelta(before, after);

                feedback(l, input);
            }
        });

        return delta[0];
    }

    /**
     * apply feedback for a newly inserted task
     */
    protected void feedback(MutableList<Task> l, @NotNull Task inserted) {
        float f = inserted.freq();
        float q = inserted.qua();
        float c = inserted.conf();
        long is = inserted.start();
        long ie = inserted.end();
        Interval ii = new Interval(is, ie);

        float penaltySum = 0;

        for (int i = 0, lSize = l.size(); i < lSize; i++) {
            Task x = l.get(i);
            if (x == inserted) continue;

            float dq = q - x.qua();
            if (dq > Param.BUDGET_EPSILON) {

                float df = abs(f - x.freq());
                float dqf = dq * df;

                if (dqf > Param.BUDGET_EPSILON) {

                    long xs = x.start();
                    long xe = x.end();
                    Interval overlap = ii.intersection(new Interval(xs, xe));
                    if (overlap != null) {

                        float penalty = dqf * ((1f + overlap.length()) / (1f + (xe - xs)));
                        if (penalty > Param.BUDGET_EPSILON) {
                            Budget b = x.budget();
                            float pBefore = b.priSafe(0), pAfter;
                            if (pBefore > 0) {
                                b.mul(1f - penalty, 1f - penalty);
                                pAfter = b.pri();
                                penaltySum += pAfter - pBefore;
                            }
                        }

                    }
                }
            }
        }

        if (penaltySum > 0)
            inserted.budget().priAdd(penaltySum); //absorb removed priority from sibling tasks
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
    public void clear(NAR nar) {

        ifNotEmptyWriteWith(l -> {
            l.forEach(Task::delete);
            l.clear();
            return null;
        });

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


    static final boolean remove(MutableList<Task> l, @NotNull Task x) {
        if (l.remove(x)) {
            x.delete();
            return true;
        }
        return false;
    }


    private Function<Task, Float> temporalConfidence(long when, float dur) {
        return x -> rankTemporalByConfidenceAndQuality(x, when, dur);
    }

    final float rankTemporalByConfidenceAndQuality(@Nullable Task t, long when, float dur) {
        return t != null ? t.confWeight(when, dur) : Float.NEGATIVE_INFINITY;// * (1+t.range()) * t.qua();

//        long range = Math.max(1, t.range());
//        long worstDistance = range == 1 ? Math.abs(when - t.mid()) : Math.max(abs(when - t.start()), abs(when - t.end()));
//        return t == null ? -1 :
//                (t.conf() * range / (range + (worstDistance * worstDistance)));
    }

    Task matchMerge(MutableList<Task> l, long now, @NotNull Task toMergeWith, float dur) {
        return l.maxBy(rankMatchMerge(toMergeWith, now, dur));
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

        long ys = y.start();
        long ye = y.end();
        float yf = y.freq();

        return x -> {
            if ((x == y) || (x == null))
                return Float.NEGATIVE_INFINITY;

            long xs = x.start();
            long xe = x.end();
//            float xtc = x.conf(yo);
//            if (xtc!=xtc) xtc = 0;

            return (1f + (1f - abs(x.freq() - yf)))
                    //* (1f + (1f - xtc))
                    //* (1f + 1f / (1f + x.range()))
                    //* (1f + 1f / (1f + x.dur()))
                    * (1f
                    + TruthPolation.evidenceDecay(1, dur,
                    abs(xs - ys) + abs(xe - ye) + abs((xe + xs) / 2f - now))
            )
                    ;
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
    protected Task compress(@Nullable Task input, long now, MutableList<Task> l, float dur, float confMin) {

        int cap = capacity();

        if (l.size() < cap || clean(l)) {
            return input; //no need for compression
        }

        //return rankTemporalByConfidenceAndOriginality(t, when, now, -1);
        float inputRank = input != null ? rankTemporalByConfidenceAndQuality(input, now, dur) : Float.POSITIVE_INFINITY;

        Task a = l.minBy(temporalConfidence(now, dur));
        //return rankTemporalByConfidenceAndOriginality(t, when, now, -1);
        if (a == null || inputRank < rankTemporalByConfidenceAndQuality(a, now, dur)) {
            //dont continue if the input was too weak
            return null;
        }

        remove(l, a);

        Task b = matchMerge(l, now, a, dur);
        if (b != null) {
            Task merged = merge(a, b, now, confMin);
            if (merged == null) {
                return input;
            } else {
                remove(l, b);
                return merged;
            }
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

            double factor = (double) ac / (ac + bc);

            Interval ai = new Interval(a.start(), a.end());
            Interval bi = new Interval(b.start(), b.end());
            Interval overlap = ai.intersection(bi);
            long mergedStart, mergedEnd;
            if (overlap != null) {
                Interval union = ai.union(bi);
                mergedStart = union.a;
                //(long) Math.round(Util.lerp(factor, a.start(), b.start()));
                mergedEnd = union.b;
                //(long) Math.round(Util.lerp(factor, a.end(), b.end()));

                float timeRatio = Math.min(1f,
                        (float) Math.max(1, overlap.length()) /
                                Math.max(1, union.length())
                );
                t = t.confWeightMult(timeRatio);
            } else {
                //create point sample
                mergedStart = mergedEnd = Math.round(Util.lerp(factor, a.mid(), b.mid()));
            }

            if (t != null)
                return Revision.mergeInterpolate(a, b, mergedStart, mergedEnd, now, t, true);
        }

        return null;
    }


    @Nullable
    @Override
    public final Task match(long when, @Deprecated long now, float dur, @Nullable Task against) {
        return ifNotEmptyReadWith(l -> {
            switch (l.size()) {
//                case 0:
//                    throw new RuntimeException("should not reach here");
                case 1:
                    return l.get(0); //special case avoid creating the lambda
                default:
                    return l.maxBy(temporalConfidence(when, dur));
            }
        });
    }

    @Nullable
    public final Truth truth(long when, float dur, @Deprecated @Nullable EternalTable eternal) {
        return truth(when, when, dur, eternal);
    }

    @Nullable
    @Override
    public final Truth truth(long when, long now, float dur, @Nullable EternalTable eternal) {

        Task topEternal = eternal != null ? eternal.match() : null;

        Truth r = ifNotEmptyReadWith(l -> {
            return TruthPolation.truth(topEternal, when, dur, l);
        });

        return (r == null && topEternal != null) ? topEternal.truth() : r;
    }

    static final boolean clean(MutableList<Task> l) {
        return l.removeIf(((Predicate<Task>) Task::isDeleted ));
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

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

/**
 * stores the items unsorted; revection manages their ranking and removal
 */
public class MicrosphereTemporalBeliefTable extends MultiRWFasterList<Task> implements TemporalBeliefTable {

    private volatile int capacity;

    public MicrosphereTemporalBeliefTable(int initialCapacity) {
        super(new FasterList<Task>(initialCapacity));
        this.capacity = initialCapacity;
    }

    /** warning: not efficient as forEach visiting */
    @NotNull @Override public Iterator<Task> iterator() {
        return toImmutable().iterator();
    }


    public void capacity(int newCapacity, NAR nar) {

        if (this.capacity != newCapacity) {

            this.capacity = newCapacity;

            //compress until under-capacity
            List<Task>[] changes = ifNotEmptyWriteWith((l) -> {

                int toRemove = l.size() - newCapacity;
                if (toRemove <= 0) {
                    return null;
                }

                List<Task> merged;
                List<Task> trash = $.newArrayList(toRemove);

                clean(l, trash);

                toRemove = l.size() - newCapacity;
                if (toRemove > 0) {

                    Time time = nar.time;

                    float dur = time.dur();
                    long now = time.time();

                    float confMin = nar.confMin.floatValue();

                    Function<Task, Float> rank = temporalConfidence(now);
                    merged = $.newArrayList(1);

                    while (l.size() > capacity) {

                        Task a = l.minBy(rank);

                        Task b = matchMerge(l, now, a, dur);

                        Task c = (b != null && b != a) ? merge(a, b, now, confMin) : null;

                        removeLater(l, a, trash);

                        if (c != null) {
                            removeLater(l, b, trash);

                            l.add(c);

                            merged.add(c);
                        }

                    }

                } else {
                    merged = null;
                }

                return new List[] { merged, trash };
            });

            if (changes!=null) {
                nar.tasks.remove(changes[1]);
                nar.inputLater(changes[1]);
                //nar.tasks.change(changes[0], changes[1]);
            }

        }

    }


    @Override
    public boolean remove(Task x) {
        final boolean[] removed = new boolean[1];
        withWriteLockAndDelegate(l -> {
           removed[0] = l.remove(x);
        });
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

        Task[] merged = new Task[1];
        List<Task> trash = $.newArrayList();

        withWriteLockAndDelegate(l -> {
            final Truth before;


            Time time = nar.time;
            long now = time.time();

            before = truth(now, eternal);

            Task next;

            float dur = time.dur();
            float confMin = nar.confMin.floatValue();
            if ((next = compress(input, now, l, trash, dur, confMin)) != null) {

                l.add(input);
                //this will be inserted to the index in a callee method

                if (next != input /*&& list.size() + 1 <= cap*/) {
                    l.add(merged[0] = next);
                }

                final Truth after = truth(now, eternal);
                delta[0] = new TruthDelta(before, after);

                feedback(l, input);
            }
        });

        //update the index outside of the atomic procedure
        nar.tasks.remove(trash);

        for (Task x : merged)
            if (x != null)
                nar.tasks.addIfAbsent(x);

        return delta[0];
    }

    /** apply feedback for a newly inserted task */
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

                float df = Math.abs(f - x.freq());
                float dqf = dq * df;

                if (dqf > Param.BUDGET_EPSILON) {

                    long xs = x.start();
                    long xe = x.end();
                    Interval overlap = ii.intersection(new Interval(xs, xe));
                    if (overlap!=null) {

                        float penalty =  dqf * ((1f + overlap.length()) / (1f + (xe-xs)));
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

        List<Task> copy = ifNotEmptyWriteWith(l->{
            List<Task> cc = $.newArrayList(l.size());
            cc.addAll(l);
            l.clear();
            return cc;
        });

        if (copy!=null)
            copy.forEach(t -> nar.tasks.remove(t));
    }


    boolean removeIfDeleted(MutableList<Task> l, List<Task> trash) {
        boolean r = l.removeIf(((Predicate<Task>) t -> {
            if (t.isDeleted()) {
                trash.add(t);
                return true;
            }
            return false;
        }));

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


    private final boolean removeLater(MutableList<Task> l, @NotNull Task x, List<Task> trash) {
        if (l.remove(x)) {
            x.delete();
            trash.add(x);
            return true;
        }
        return false;
    }


    private Function<Task, Float> temporalConfidence(long when) {
        return x -> rankTemporalByConfidence(x, when);
    }

    final float rankTemporalByConfidence(@Nullable Task t, long when) {
        return t == null ? -1 : t.conf(when);
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

        long yo = y.mid();
        float yf = y.freq();

        return x -> {
            if ((x == y) || (x == null))
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
    protected Task compress(@Nullable Task input, long now, MutableList<Task> l, List<Task> trash, float dur, float confMin) {

        int cap = capacity();

        if (l.size() < cap || clean(l, trash)) {
            return input; //no need for compression
        }

        //return rankTemporalByConfidenceAndOriginality(t, when, now, -1);
        float inputRank = input != null ? rankTemporalByConfidence(input, now) : Float.POSITIVE_INFINITY;

        Task a = l.minBy(temporalConfidence(now));
        //return rankTemporalByConfidenceAndOriginality(t, when, now, -1);
        if (a == null || inputRank < rankTemporalByConfidence(a, now)) {
            //dont continue if the input was too weak
            return null;
        }

        removeLater(l, a, trash);

        Task b = matchMerge(l, now, a, dur);
        if (b != null) {
            Task merged = merge(a, b, now, confMin);
            if (merged == null) {
                return input;
            } else {
                removeLater(l, b, trash);
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

            long mergedStart = //Math.min(a.start(), b.start());
                         (long) Math.round(Util.lerp(factor, a.start(), b.start()));
            long mergedEnd = //Math.max(a.end(), b.end());
                         (long) Math.round(Util.lerp(factor, a.end(), b.end()));

            return Revision.mergeInterpolate(a, b, mergedStart, mergedEnd, now, t, true);
        }

        return null;
    }


    @Nullable
    @Override
    public final Task match(long when, @Deprecated long now, @Nullable Task against) {
        return ifNotEmptyReadWith(l->{
            switch (l.size()) {
//                case 0:
//                    throw new RuntimeException("should not reach here");
                case 1:
                    return l.get(0); //special case avoid creating the lambda
                default:
                    return l.maxBy(temporalConfidence(when));
            }
        });
    }

    @Nullable
    public final Truth truth(long when, @Deprecated @Nullable EternalTable eternal) {
        return truth(when, when, eternal);
    }

    @Nullable @Override public final Truth truth(long when, long now, @Nullable EternalTable eternal) {

        Task topEternal = eternal!=null ? eternal.match() : null;

        Truth r = ifNotEmptyReadWith(l->{
            return TruthPolation.truth(topEternal, when, l);
        });

        return (r == null && topEternal!=null) ? topEternal.truth() : r;
    }

    private final boolean clean(MutableList<Task> l, List<Task> trash) {
        return removeIfDeleted(l, trash);
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

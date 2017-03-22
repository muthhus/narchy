package nars.table;

import jcog.list.FasterList;
import jcog.list.Top2;
import jcog.math.Interval;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.bag.impl.TaskHijackBag;
import nars.budget.Budget;
import nars.budget.BudgetMerge;
import nars.concept.Concept;
import nars.task.Revision;
import nars.task.TruthPolation;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.util.signal.SignalTask;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

import static java.lang.Math.abs;
import static jcog.math.Interval.intersectLength;
import static nars.util.UtilityFunctions.or;

/**
 * stores the items unsorted; revection manages their ranking and removal
 */
public class HijackTemporalBeliefTable extends TaskHijackBag implements TemporalBeliefTable {



    public HijackTemporalBeliefTable(int initialCapacity, Random random) {
        super(4 /* reprobes */, BudgetMerge.maxBlend, random);
        setCapacity(initialCapacity);
    }

    @Override
    public void capacity(int c, NAR nar) {
        setCapacity(c);
    }

//    @Override
//    public float pri(@NotNull Task key) {
//        long dt = key.mid() - lastCommitTime;
//
//        float f = super.pri(key) * key.conf() * 1f / (1f + Math.abs(dt));
//        if (dt > 0)
//            f = or(f, 0.5f);
//
//        return f;
//    }

    //    public void capacity(int newCapacity, NAR nar) {
//
//        if (this.capacity != newCapacity) {
//
//            this.capacity = newCapacity;
//
//            //compress until under-capacity
//            ifSizeExceedsWriteWith(newCapacity, (l) -> {
//
//                int toRemove = l.size() - newCapacity; //will be positive
//
//                if (clean(l)) {
//                    toRemove = l.size() - newCapacity;
//                }
//
//                if (toRemove > 0) {
//
//                    Time time = nar.time;
//
//                    int dur = time.dur();
//                    long now = time.time();
//
//                    float confMin = nar.confMin.floatValue();
//
//                    Function<Task, Float> rank = temporalConfidence(now, now, dur);
//
//                    while (l.size() > capacity) {
//
//                        Task a = l.minBy(rank);
//
//                        Task b = matchMerge(l, now, a, dur);
//
//                        Task c = (b != null && b != a) ? merge(a, b, now, confMin, dur) : null;
//
//                        remove(l, a, true);
//
//                        if (c != null) {
//                            remove(l, b, true);
//
//                            l.add(c);
//                        }
//
//                    }
//
//                }
//
//            });
//
//        }
//
//    }


    @Override
    protected boolean replace(Task incoming, Task existing) {
        if (incoming instanceof SignalTask)
            return true;

        float incomingPri = pri(incoming);
        return hijackSoftmax(incomingPri, pri(existing));
        //return hijackGreedy(incomingPri, pri(existing));
    }

    @Override
    public float pri(@NotNull Task t) {
        return (1f + t.priSafe(0)) * (1f + t.conf());
    }

    @Nullable
    @Override
    public final Task add(@NotNull Task input, @Deprecated EternalTable eternal, @Deprecated Concept concept, @NotNull NAR nar) {


        return add(input, nar);

//        int cap = capacity();
//        if (cap == 0)
//            return null;
//
//
//
//        //the result of compression is processed separately
//        final TruthDelta[] delta = new TruthDelta[1];
//
//
//        withWriteLockAndDelegate(l -> {
//
//            //1. check for duplicate, merge budget. exit
//            int size = l.size();
//            for (int i = 0; i < size; i++) {
//                Task x = l.get(i);
//                if (x == input)
//                    return; //same instance
//
//                if (x.equals(input)) {
//                    BudgetMerge.maxBlend.apply(x.budget(), input.budget(), 1f);
//                    return;
//                }
//            }
//
//            final Truth before;
//
//            Time time = nar.time;
//            long now = time.time();
//            int dur = time.dur();
//
//            before = truth(now, dur, eternal, l);
//
//            Task next;
//
//            float confMin = nar.confMin.floatValue();
//            if ((next = compress(input, now, l, dur, confMin)) != null) {
//
//                l.add(input);
//                //this will be inserted to the index in a callee method
//
//                if (next != input /*&& list.size() + 1 <= cap*/) {
//                    l.add(next);
//                }
//
//                final Truth after = truth(now, dur, eternal, l);
//                delta[0] = new TruthDelta(before, after);
//
//                if (Param.SIBLING_TEMPORAL_TASK_FEEDBACK)
//                    feedback(l, input);
//            }
//        });
//
//        return delta[0];
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
                    long overlapLength = Interval.intersectLength(is, ie, xs, xe);
                    if (overlapLength >= 0) {

                        float penalty = dqf * ((1f + overlapLength) / (1f + (xe - xs)));
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

//    public int duration() {
//        //return (((float)range()) / size()) * 2f;
//        return 1f;
//    }




//    @Override
//    public final boolean isFull() {
//        return size() == capacity();
//    }


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


    final boolean remove(MutableList<Task> l, @NotNull Task x, boolean delete) {
        if (l.remove(x)) {
            if (delete)
                x.delete();
            onRemoved(x);
            return true;
        }
        return false;
    }


    static private Function<Task, Float> temporalConfidence(long when, long now, int dur) {
        return x -> rankTemporalByConfidence(x, when, now, dur);
    }

//    static private FloatFunction<Task> temporalConfidenceF(long when, long now, int dur) {
//        return x -> rankTemporalByConfidence(x, when, now, dur);
//    }

    static final float rankTemporalByConfidence(@Nullable Task t, long when, long now, int dur) {

        if (t == null)
            return Float.NEGATIVE_INFINITY;

        float r = t.evi(when, dur);
        //float r = t.evi(when, dur) * (1+ (t.end()-t.start()) );// * t.qua();

//        //HACK present/future prediction boost
//        if (t.start() >= now) {
//            r += 1f;
//        }

        return r;
    }

    @Nullable static Task matchMerge(@NotNull FasterList<Task> l, long now, @NotNull Task toMergeWith, int dur) {
        return l.maxBy(Float.NEGATIVE_INFINITY, rankMatchMerge(toMergeWith, now, dur));
    }

    /**
     * max value given to the ideal match for the provided task to be merged with
     */
    @NotNull
    static public FloatFunction<Task> rankMatchMerge(@NotNull Task y, long now, int dur) {

        //prefer
        //  same frequency
        //  long time from now
        //  non-zero overlap with the task
        //  least stamp overlap
        //  low confidence?
        //  similar range

        long ys = y.start();
        long ye = y.end();
        float yRange = (ye - ys) / dur;
        float yf = y.freq();
        float yDist = Math.min(abs(ye - now), abs(ys - now)) / dur;
        long[] yStamp = y.stamp();

        return x -> {
            if ((x == y) || (x == null))
                return Float.NEGATIVE_INFINITY;

            long xs = x.start();
            long xe = x.end();
//            float xtc = x.conf(yo);
//            if (xtc!=xtc) xtc = 0;

            long overlap = intersectLength(ys, ye, xs, xe);
            if (overlap == -1)
                return Float.NEGATIVE_INFINITY; //dont allow merge if no overlap

            return (1f - abs(x.freq() - yf)) *
                   (1f / (1f + abs(yRange  - (xe - xs)))) *
                   (1f + Math.min(abs(xe-now), abs(xs-now))/yDist) *
                   (1f + overlap / (1 + yRange)) *
                   (1f - Stamp.overlapFraction(yStamp, x.stamp())/2f)
                   //(1f + (1f - x.conf()))
            ;
        };

    }


    /**
     * frees one slot by removing 2 and projecting a new belief to their midpoint. returns the merged task
     */
    protected Task compress(@Nullable Task input, long now, FasterList<Task> l, int dur, float confMin) {

        int cap = capacity();

        if (l.size() < cap || clean(l)) {
            return input; //no need for compression
        }

        //return rankTemporalByConfidenceAndOriginality(t, when, now, -1);
        float inputRank = input != null ? rankTemporalByConfidence(input, now, now, dur) : Float.POSITIVE_INFINITY;

        Task a = l.minBy(temporalConfidence(now, now, dur));

        if (a == null) {
            return null; // this probably shouldnt happen
        }

        if (inputRank < rankTemporalByConfidence(a, now, now, dur)) {
            //if weaker than everything else, attempt a merge of the input with another

            Task b = matchMerge(l, now, input, dur);
            if (b == null) {
                return null; //nothing to merge with
            }

            Task merged = merge(input, b, now, confMin, dur);
            if (merged == null) {
                return null; //merge failed
            }

            remove(l, b, true);

            return merged;

        } else {
            //merge 2 weakest and allow the input as-is

            remove(l, a, false);

            Task b = matchMerge(l, now, a, dur);
            if (b != null) {
                Task merged = merge(a, b, now, confMin, dur);

                a.delete();  //delete a after the merge, so that its budget revises

                if (merged == null) {
                    return input;
                } else {
                    remove(l, b, true);
                    return merged;
                }
            } else {
                a.delete();

                return input;
            }

        }

    }

    /**
     * t is the target time of the new merged task
     */
    @Nullable private Task merge(@NotNull Task a, @NotNull Task b, long now, float confMin, int dur) {


        Interval ai = new Interval( a.start() , a.end() );
        Interval bi = new Interval( b.start() , b.end() );

        Interval timeOverlap = ai.intersection(bi);

        if (timeOverlap != null) {
            float aw = a.evi();
            float bw = b.evi();

            float aa = aw * (1 + ai.length());
            float bb = bw * (1 + bi.length());
            float p = aa / (aa + bb);

            float stampDiscount =
//                //more evidence overlap indicates redundant information, so reduce the confWeight (measure of evidence) by this amount
//                //TODO weight the contributed overlap amount by the relative confidence provided by each task
                    1f - Stamp.overlapFraction(a.stamp(), b.stamp()) / 2f;

            //discount related to loss of stamp when its capacity to contain the two incoming is reached
            float stampCapacityDiscount =
                    Math.min(1f, ((float)Param.STAMP_CAPACITY) / (a.stamp().length + b.stamp().length));

            float rangeEquality = 0.5f / (1f + Math.abs(ai.length() - bi.length()));


            Interval union = ai.union(bi);
            float timeDiscount = rangeEquality + (1f-rangeEquality) * ((float) (timeOverlap.length())) / (1 + union.length());

            Truth t = Revision.merge(a, p, b, stampDiscount * timeDiscount * stampCapacityDiscount, confMin);
            if (t!=null) {
                long mergedStart = union.a;
                long mergedEnd = union.b;
                return Revision.mergeInterpolate(a, b, mergedStart, mergedEnd, now, t, true);
            }
        }

        return null;

    }


    @Nullable
    @Override
    public Task match(long when, long now, int dur, @Nullable Task against) {
        Top2<Task> s = new Top2<>(temporalConfidence(when, now, dur), this);

        Task a = s.a;
        if (s.b == null)
            return a;
        Task c = merge(a, s.b, now, a.conf(), dur);
        return c != null ? c : a;
    }

    @Nullable
    @Override
    public Truth truth(long when, long now, int dur, @Nullable EternalTable eternal) {

        return TruthPolation.truth(eternal != null ? eternal.match() : null, when, dur, this);
        //return Truth.maxConf(r, topEternal);
        //return (r == null && topEternal != null) ? topEternal.truth() : r;
    }

    final boolean clean(MutableList<Task> l) {
        return l.removeIf(x -> {
            if (x.isDeleted()) {
                onRemoved(x);
                return true;
            }
            return false;
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
////        int dur = 1f/(1f + (maxtime - mintime));
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
